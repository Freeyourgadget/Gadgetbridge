/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Calls;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Menstrual;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetPhoneInfoRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendMenstrualModifyTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetMusicStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Handles responses that are not a reply to a request
 *
 */
public class AsynchronousResponse {
    private static final Logger LOG = LoggerFactory.getLogger(AsynchronousResponse.class);

    private final HuaweiSupportProvider support;
    private final Handler mFindPhoneHandler = new Handler();
    private final static HashMap<Integer, String> dayOfWeekMap = new HashMap<>();
    static {
        dayOfWeekMap.put(Calendar.MONDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_MO);
        dayOfWeekMap.put(Calendar.TUESDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_TU);
        dayOfWeekMap.put(Calendar.WEDNESDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_WE);
        dayOfWeekMap.put(Calendar.THURSDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_TH);
        dayOfWeekMap.put(Calendar.FRIDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_FR);
        dayOfWeekMap.put(Calendar.SATURDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SA);
        dayOfWeekMap.put(Calendar.SUNDAY, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SU);
    }

    public AsynchronousResponse(HuaweiSupportProvider support) {
        this.support = support;
    }

    public void handleResponse(HuaweiPacket response) {
        // Ignore messages if the key isn't set yet
        if (support.getParamsProvider().getSecretKey() == null)
            return;

        try {
            response.parseTlv();
        } catch (HuaweiPacket.ParseException e) {
            LOG.error("Parse TLV exception", e);
            return;
        }

        try {
            handleFindPhone(response);
            handleMusicControls(response);
            handleCallControls(response);
            handlePhoneInfo(response);
            handleMenstrualModifyTime(response);
        } catch (Request.ResponseParseException e) {
            LOG.error("Response parse exception", e);
        }
    }

    private void handleFindPhone(HuaweiPacket response) throws Request.ResponseParseException {
        if (response.serviceId == FindPhone.id && response.commandId == FindPhone.Response.id) {
            if (!(response instanceof FindPhone.Response))
                throw new Request.ResponseTypeMismatchException(response, FindPhone.Response.class);

            SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(support.getDeviceMac());

            String findPhone = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_FIND_PHONE, support.getContext().getString(R.string.p_off));

            if (findPhone.equals(support.getContext().getString(R.string.p_off))) {
                LOG.debug("Find phone command received, but it is disabled");
                // TODO: hide applet on device
                return;
            }

            if (sharedPreferences.getBoolean("disable_find_phone_with_dnd", false) && dndActive()) {
                LOG.debug("Find phone command received, ringing prevented because of DND");
                // TODO: stop the band from showing as ringing
                return;
            }

            if (!findPhone.equals(support.getContext().getString(R.string.p_on))) {
                // Duration set, stop after specified time
                String strDuration = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION, "0");

                int duration = Integer.parseInt(strDuration);
                if (duration > 0) {
                    mFindPhoneHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                            findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                            support.evaluateGBDeviceEvent(findPhoneEvent);

                            // TODO: stop the band from showing as ringing
                        }
                    }, duration * 1000L);
                }
            }

            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            if (((FindPhone.Response) response).start)
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
            else
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
            support.evaluateGBDeviceEvent(findPhoneEvent);
        }
    }

    private boolean dndActive() {
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(support.getDeviceMac());

        String dndSwitch = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB, "off");
        if (dndSwitch.equals(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF))
            return false;

        String startStr = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_START, "00:00");
        if (dndSwitch.equals("automatic")) startStr = "00:00";
        String endStr = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_END, "23:59");
        if (dndSwitch.equals("automatic")) endStr = "23:59";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalTime currentTime = LocalTime.now();
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);

            if (start.isAfter(currentTime))
                return false;
            if (end.isBefore(currentTime))
                return false;
        } else {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
            try {
                Date currentTime = dateFormat.parse(String.format(GBApplication.getLanguage(), "%d:%d",
                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                        Calendar.getInstance().get(Calendar.MINUTE)));
                Date start = dateFormat.parse(startStr);
                Date end = dateFormat.parse(endStr);

                assert start != null;
                if (start.after(currentTime))
                    return false;
                assert end != null;
                if (end.before(currentTime))
                    return false;
            } catch (ParseException e) {
                LOG.error("Parse exception for DnD", e);
            }
        }

        Calendar date = Calendar.getInstance();
        String preferenceString = dayOfWeekMap.get(date.get(Calendar.DAY_OF_WEEK));

        return sharedPreferences.getBoolean(preferenceString, true);
    }

    /**
     * Handles asynchronous music packet, for the following events:
     *  - The app is opened on the band (sends back music info)
     *  - A button is clicked
     *    - Play
     *    - Pause
     *    - Previous
     *    - Next
     *  - The volume is adjusted
     * @param response Packet to be handled
     */
    private void handleMusicControls(HuaweiPacket response) throws Request.ResponseParseException {
        if (response.serviceId == MusicControl.id) {
            AudioManager audioManager = (AudioManager) this.support.getContext().getSystemService(Context.AUDIO_SERVICE);

            if (response.commandId == MusicControl.MusicStatusResponse.id) {
                if (!(response instanceof MusicControl.MusicStatusResponse))
                    throw new Request.ResponseTypeMismatchException(response, MusicControl.MusicStatusResponse.class);

                MusicControl.MusicStatusResponse resp = (MusicControl.MusicStatusResponse) response;
                if (resp.status != -1 && resp.status != 0x000186A0) {
                    LOG.warn("Music information error, will stop here: " + Integer.toHexString(resp.status));
                    return;
                }

                LOG.debug("Music information requested, sending acknowledgement and music info.");
                SetMusicStatusRequest setMusicStatusRequest = new SetMusicStatusRequest(this.support, MusicControl.MusicStatusResponse.id, MusicControl.successValue);
                try {
                    setMusicStatusRequest.doPerform();
                } catch (IOException e) {
                    GB.toast("Failed to send music status request", Toast.LENGTH_SHORT, GB.ERROR, e);
                    LOG.error("Failed to send music status request (1)", e);
                }
                // Send Music Info
                this.support.sendSetMusic();
            } else if (response.commandId == MusicControl.Control.id) {
                if (!(response instanceof MusicControl.Control.Response))
                    throw new Request.ResponseTypeMismatchException(response, MusicControl.Control.Response.class);

                MusicControl.Control.Response resp = (MusicControl.Control.Response) response;

                if (resp.buttonPresent) {
                    if (resp.button != MusicControl.Control.Response.Button.Unknown) {
                        GBDeviceEventMusicControl musicControl = new GBDeviceEventMusicControl();
                        switch (resp.button) {
                            case Play:
                                LOG.debug("Music - Play button event received");
                                musicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                                break;
                            case Pause:
                                LOG.debug("Music - Pause button event received");
                                musicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                                break;
                            case Previous:
                                LOG.debug("Music - Previous button event received");
                                musicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                                break;
                            case Next:
                                LOG.debug("Music - Next button event received");
                                musicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                                break;
                            case Volume_up:
                                LOG.debug("Music - Volume up button event received");
                                musicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                                break;
                            case Volume_down:
                                LOG.debug("Music - Volume down button event received");
                                musicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                                break;
                            default:
                        }
                        this.support.evaluateGBDeviceEvent(musicControl);
                    }
                }
                if (resp.volumePresent) {
                    byte volume = resp.volume;
                    if (volume > audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                        LOG.warn("Music - Received volume is too high: 0x"
                                + Integer.toHexString(volume)
                                + " > 0x"
                                + Integer.toHexString(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                        );
                        // TODO: probably best to send back an error code, though I wouldn't know which
                        return;
                    }
                    if (Build.VERSION.SDK_INT > 28) {
                        if (volume < audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)) {
                            LOG.warn("Music - Received volume is too low: 0x"
                                    + Integer.toHexString(volume)
                                    + " < 0x"
                                    + audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                            );
                            // TODO: probably best to send back an error code, though I wouldn't know which
                            return;
                        }
                    }
                    LOG.debug("Music - Setting volume to: 0x" + Integer.toHexString(volume));
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                }

                if (resp.buttonPresent || resp.volumePresent) {
                    SetMusicStatusRequest setMusicStatusRequest = new SetMusicStatusRequest(this.support, MusicControl.Control.id, MusicControl.successValue);
                    try {
                        setMusicStatusRequest.doPerform();
                    } catch (IOException e) {
                        GB.toast("Failed to send music status request", Toast.LENGTH_SHORT, GB.ERROR, e);
                        LOG.error("Failed to send music status request (2)", e);
                    }
                }
            }
        }
    }

    private void handleCallControls(HuaweiPacket response) throws Request.ResponseParseException {
        if (response.serviceId == Calls.id && response.commandId == Calls.AnswerCallResponse.id) {
            if (!(response instanceof Calls.AnswerCallResponse))
                throw new Request.ResponseTypeMismatchException(response, Calls.AnswerCallResponse.class);

            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(support.getDevice().getAddress());

            GBDeviceEventCallControl callControlEvent = new GBDeviceEventCallControl();
            switch (((Calls.AnswerCallResponse) response).action) {
                case UNKNOWN:
                    LOG.info("Unknown action for call");
                    return;
                case CALL_ACCEPT:
                    callControlEvent.event = GBDeviceEventCallControl.Event.ACCEPT;
                    LOG.info("Accepted call");

                    if (!prefs.getBoolean("enable_call_accept", true)) {
                        LOG.info("Disabled accepting calls, ignoring");
                        return;
                    }

                    break;
                case CALL_REJECT:
                    callControlEvent.event = GBDeviceEventCallControl.Event.REJECT;
                    LOG.info("Rejected call");

                    if (!prefs.getBoolean("enable_call_reject", true)) {
                        LOG.info("Disabled rejecting calls, ignoring");
                        return;
                    }

                    break;
            }
            support.evaluateGBDeviceEvent(callControlEvent);
        }
    }

    private void handlePhoneInfo(HuaweiPacket response) {
        if (response.serviceId == DeviceConfig.id && response.commandId == DeviceConfig.PhoneInfo.id) {
            if (!(response instanceof DeviceConfig.PhoneInfo.Response)) {
                // TODO: exception
                return;
            }
            DeviceConfig.PhoneInfo.Response phoneInfoResp = (DeviceConfig.PhoneInfo.Response) response;
            GetPhoneInfoRequest getPhoneInfoReq = new GetPhoneInfoRequest(this.support, phoneInfoResp.info);
            try {
                getPhoneInfoReq.doPerform();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMenstrualModifyTime(HuaweiPacket response) {
        if (response.serviceId == Menstrual.id && response.commandId == Menstrual.ModifyTime.id) {
            if (!(response instanceof Menstrual.ModifyTime.Response)) {
                // TODO: exception
                return;
            }
            //Menstrual.ModifyTime.Response menstrualModifyTimeResp = (Menstrual.ModifyTime.Response) response;
            SendMenstrualModifyTimeRequest sendMenstrualModifyTimeReq = new SendMenstrualModifyTimeRequest(this.support);
            try {
                sendMenstrualModifyTimeReq.doPerform();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
