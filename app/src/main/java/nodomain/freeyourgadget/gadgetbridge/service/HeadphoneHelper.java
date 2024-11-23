/*  Copyright (C) 2024 Arjan Schrijver, Daniele Gobbetti, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GBTextToSpeech;

public class HeadphoneHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HeadphoneHelper.class);

    public interface Callback {
        void evaluateGBDeviceEvent(GBDeviceEvent event);
    }

    private final GBDevice device;
    private final GBTextToSpeech gbTextToSpeech;
    private final Callback callback;

    public HeadphoneHelper(Context context, GBDevice device, Callback callback) {
        this.device = device;
        this.callback = callback;
        final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(this.device.getAddress());
        gbTextToSpeech = new GBTextToSpeech(context, new UtteranceProgressListener(),
                prefs.getBoolean(PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE, false) ?
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE :
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        );
    }

    public void dispose() {
        gbTextToSpeech.shutdown();
    }

    public void onSetCallState(CallSpec callSpec) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        if (!prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL, false))
                return;

        final int delayMillis = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL_DELAY, "15")) * 1000;

        if (CallSpec.CALL_INCOMING != callSpec.command)
                return;

        if (!gbTextToSpeech.isConnected()) { // schedule the automatic reply here, if the speech to text is not connected. Else it's done by the callback, and the timeout starts after the name or number have been spoken
            Looper mainLooper = Looper.getMainLooper();
            LOG.debug("Incoming call, scheduling auto answer in {} seconds.", delayMillis / 1000);

            new Handler(mainLooper).postDelayed(() -> {
                GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
                callCmd.event = GBDeviceEventCallControl.Event.ACCEPT;
                callback.evaluateGBDeviceEvent(callCmd);
            }, delayMillis); //15s

            return;
        }
        String speechText = callSpec.name;
        if (callSpec.name.equals(callSpec.number)) {
            StringBuilder numberSpeller = new StringBuilder();
            for (char c : callSpec.number.toCharArray()) {
                numberSpeller.append(c).append(" ");
            }
            speechText = numberSpeller.toString();
        }
        gbTextToSpeech.speak(speechText);
    }

    public void onNotification(NotificationSpec notificationSpec) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        if (!prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SPEAK_NOTIFICATIONS_ALOUD, false))
            return;

        if (gbTextToSpeech.isConnected()) {
            String notificationSpeller = new StringBuilder()
                    .append(notificationSpec.sourceName == null ? "" : notificationSpec.sourceName).append(". ")
                    .append(notificationSpec.title == null ? "" : notificationSpec.title).append(": ")
                    .append(notificationSpec.body == null ? "" : notificationSpec.body).toString();

            gbTextToSpeech.speakNotification(notificationSpeller);
        }
    }

    /**
     *
     * @param config
     * @return True if handled, false otherwise
     */
    public boolean onSendConfiguration(String config) {
        if (PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE.equals(config)) {
            final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            gbTextToSpeech.setAudioFocus(prefs.getBoolean(PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE, false) ?
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE :
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            return true;
        }
        return false;
    }

    private class UtteranceProgressListener extends android.speech.tts.UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
//            LOG.debug("UtteranceProgressListener onStart.");
        }

        @Override
        public void onDone(String utteranceId) {
//            LOG.debug("UtteranceProgressListener onDone.");

            gbTextToSpeech.abandonFocus();
            if (utteranceId.equals("call")) {
                SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(HeadphoneHelper.this.device.getAddress());
                final int delayMillis = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL_DELAY, "15")) * 1000;


                Looper mainLooper = Looper.getMainLooper();
                new Handler(mainLooper).postDelayed(() -> {
                    GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
                    callCmd.event = GBDeviceEventCallControl.Event.ACCEPT;
                    callback.evaluateGBDeviceEvent(callCmd);
                }, delayMillis); //15s
            }
        }

        @Override
        public void onError(String utteranceId) {
            LOG.error("UtteranceProgressListener returned error.");
        }
    }
}
