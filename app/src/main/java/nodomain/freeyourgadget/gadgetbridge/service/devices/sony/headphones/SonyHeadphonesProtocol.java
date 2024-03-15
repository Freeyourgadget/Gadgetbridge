/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AdaptiveVolumeControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AudioUpsampling;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatEnabled;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.VoiceNotifications;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SoundPosition;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SurroundMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.TouchSensor;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.WideAreaTap;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.AbstractSonyProtocolImpl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.SonyProtocolImplV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v2.SonyProtocolImplV2;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SonyHeadphonesProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(SonyHeadphonesProtocol.class);

    private byte sequenceNumber = 0;

    // Request queue, sent every ACK, as we can't send them all at once
    // Initially, it should contain all the init requests
    private final Queue<Request> requestQueue = new LinkedList<>();
    private int pendingAcks = 0;

    private AbstractSonyProtocolImpl protocolImpl = null;

    public SonyHeadphonesProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] res) {
        final Message message = Message.fromBytes(res);
        if (message == null) {
            return null;
        }

        LOG.info("Received {}", message);

        final MessageType messageType = message.getType();

        if (messageType == MessageType.ACK) {
            if (sequenceNumber == message.getSequenceNumber()) {
                LOG.warn("Unexpected ACK sequence number {}", message.getSequenceNumber());
                return null;
            }

            sequenceNumber = message.getSequenceNumber();

            return new GBDeviceEvent[]{handleAck()};
        }

        if (message.getPayload().length == 0) {
            LOG.warn("Empty message: {}", message);
            return null;
        }

        final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        final List<GBDeviceEvent> events = new ArrayList<>();

        if (protocolImpl == null) {
            // Check if we got an init response, which should indicate the protocol version
            if (MessageType.COMMAND_1.equals(messageType) && message.getPayload()[0] == 0x01) {
                // Init reply, set the protocol version

                final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences(
                        DeviceSettingsPreferenceConst.PREF_SONY_ACTUAL_PROTOCOL_VERSION, null
                );
                events.add(eventUpdatePreferences);

                final String protocolVersionPref = prefs.getString("pref_protocol_version", "auto");
                final String protocolVersion;
                if (protocolVersionPref.equals("auto")) {
                    if (message.getPayload().length == 4) {
                        // WH-1000XM3:      01:00:40:10
                        // WF-SP800N 1.0.1: 01:00:70:00
                        // Wi-SP600N:       01:00:40:10
                        protocolVersion = "v1";
                    } else if (message.getPayload().length == 8) {
                        // WF-1000XM4 1.1.5: 01:00:01:00:00:00:00:00
                        // LinkBuds S 2.0.2: 01:00:03:00:00:07:00:00
                        // WH-1000XM5 1.1.3: 01:00:03:00:00:00:00:00
                        // WF-1000XM5 2.0.1: 01:00:03:00:10:04:00:00
                        // LinkBuds   1.0.3: 01:00:02:00:10:00:00:00
                        protocolVersion = "v2";
                    } else {
                        LOG.error("Unexpected init response payload length: {}", message.getPayload().length);
                        return events.toArray(new GBDeviceEvent[0]);
                    }
                } else {
                    protocolVersion = protocolVersionPref;
                }

                LOG.info("Sony protocol version: {}/{}", protocolVersionPref, protocolVersion);

                eventUpdatePreferences.withPreference(
                        DeviceSettingsPreferenceConst.PREF_SONY_ACTUAL_PROTOCOL_VERSION,
                        protocolVersion
                );

                switch (protocolVersion) {
                    case "v1":
                        protocolImpl = new SonyProtocolImplV1(getDevice());
                        break;
                    case "v2":
                        protocolImpl = new SonyProtocolImplV2(getDevice());
                        break;
                    default:
                        LOG.warn("Unknown protocol version {}", protocolVersion);
                        return events.toArray(new GBDeviceEvent[0]);
                }
            }
        }

        if (protocolImpl == null) {
            LOG.error("No protocol implementation, ignoring message");
            return events.toArray(new GBDeviceEvent[0]);
        }

        try {
            switch (messageType) {
                case COMMAND_1:
                case COMMAND_2:
                    events.add(new GBDeviceEventSendBytes(encodeAck(message.getSequenceNumber())));
                    events.addAll(protocolImpl.handlePayload(messageType, message.getPayload()));
                    break;
                default:
                    LOG.warn("Unknown message type for {}", message);
                    return events.toArray(new GBDeviceEvent[0]);
            }
        } catch (final Exception e) {
            // Don't crash the app if we somehow fail to handle the payload
            LOG.error("Error handling payload", e);
        }

        return events.toArray(new GBDeviceEvent[0]);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        if (protocolImpl == null) {
            LOG.error("No protocol implementation, ignoring config {}", config);
            return super.encodeSendConfiguration(config);
        }

        final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        final Request configRequest;

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL:
            case DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE:
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL:
                configRequest = protocolImpl.setAmbientSoundControl(AmbientSoundControl.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE:
                configRequest = protocolImpl.setAmbientSoundControlButtonMode(AmbientSoundControlButtonMode.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_NOISE_OPTIMIZER_START:
                configRequest = protocolImpl.startNoiseCancellingOptimizer(true);
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_NOISE_OPTIMIZER_CANCEL:
                configRequest = protocolImpl.startNoiseCancellingOptimizer(false);
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION:
                configRequest = protocolImpl.setSoundPosition(SoundPosition.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE:
                configRequest = protocolImpl.setSurroundMode(SurroundMode.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE:
                configRequest = protocolImpl.setEqualizerPreset(EqualizerPreset.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BASS:
                configRequest = protocolImpl.setEqualizerCustomBands(EqualizerCustomBands.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_UPSAMPLING:
                configRequest = protocolImpl.setAudioUpsampling(AudioUpsampling.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_VOLUME:
                configRequest = protocolImpl.setVolume(prefs.getInt(config, 15));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_TOUCH_SENSOR:
                configRequest = protocolImpl.setTouchSensor(TouchSensor.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF:
                configRequest = protocolImpl.setAutomaticPowerOff(AutomaticPowerOff.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_LEFT:
            case DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_RIGHT:
                configRequest = protocolImpl.setButtonModes(ButtonModes.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_DOUBLE_TAP:
            case DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_TRIPLE_TAP:
                configRequest = protocolImpl.setQuickAccess(QuickAccess.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_PAUSE_WHEN_TAKEN_OFF:
                configRequest = protocolImpl.setPauseWhenTakenOff(PauseWhenTakenOff.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_NOTIFICATION_VOICE_GUIDE:
                configRequest = protocolImpl.setVoiceNotifications(VoiceNotifications.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_CONNECT_TWO_DEVICES:
                LOG.warn("Connection to two devices not implemented ('{}')", config);
                return super.encodeSendConfiguration(config);
            case DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT:
                configRequest = protocolImpl.setSpeakToChatEnabled(SpeakToChatEnabled.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY:
            case DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE:
            case DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_TIMEOUT:
                configRequest = protocolImpl.setSpeakToChatConfig(SpeakToChatConfig.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_ADAPTIVE_VOLUME_CONTROL:
                configRequest = protocolImpl.setAdaptiveVolumeControl(AdaptiveVolumeControl.fromPreferences(prefs));
                break;
            case DeviceSettingsPreferenceConst.PREF_SONY_WIDE_AREA_TAP:
                configRequest = protocolImpl.setWideAreaTap(WideAreaTap.fromPreferences(prefs));
                break;
            default:
                LOG.warn("Unknown config '{}'", config);
                return super.encodeSendConfiguration(config);
        }

        if (configRequest == null) {
            LOG.warn("Failed to encode config request for {}", config);
            return super.encodeSendConfiguration(config);
        }

        pendingAcks++;

        return configRequest.encode(sequenceNumber);
    }

    @Override
    public byte[] encodeTestNewFunction() {
        //return Request.fromHex(MessageType.COMMAND_1, "c40100").encode(sequenceNumber);

        return null;
    }

    @Override
    public byte[] encodePowerOff() {
        if (protocolImpl != null) {
            return protocolImpl.powerOff().encode(sequenceNumber);
        }

        return super.encodePowerOff();
    }

    public byte[] encodeAck(byte sequenceNumber) {
        return new Message(MessageType.ACK, (byte) (1 - sequenceNumber), new byte[0]).encode();
    }

    public byte[] encodeInit() {
        pendingAcks++;
        return new Message(
                MessageType.COMMAND_1,
                sequenceNumber,
                new byte[]{
                        (byte) 0x00,
                        (byte) 0x00
                }
        ).encode();
    }

    public void enqueueRequests(final List<Request> requests) {
        LOG.debug("Enqueueing {} requests", requests.size());

        requestQueue.addAll(requests);
    }

    public int getPendingAcks() {
        return pendingAcks;
    }

    public void decreasePendingAcks() {
        pendingAcks--;
    }

    public byte[] getFromQueue() {
        return requestQueue.remove().encode(sequenceNumber);
    }

    public boolean hasProtocolImplementation() {
        return protocolImpl != null;
    }

    private GBDeviceEvent handleAck() {
        pendingAcks--;

        if (!requestQueue.isEmpty()) {
            LOG.debug("Outstanding requests in queue: {}", requestQueue.size());

            final Request request = requestQueue.remove();

            return new GBDeviceEventSendBytes(request.encode(sequenceNumber));
        }

        if (GBDevice.State.INITIALIZING.equals(getDevice().getState())) {
            // The queue is now empty, so we have got all the information from the device
            // Mark it as initialized

            return new GBDeviceEventUpdateDeviceState(GBDevice.State.INITIALIZED);
        }

        return null;
    }
}
