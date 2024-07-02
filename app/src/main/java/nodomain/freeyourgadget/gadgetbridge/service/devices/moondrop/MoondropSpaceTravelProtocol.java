/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.moondrop;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

public class MoondropSpaceTravelProtocol extends GBDeviceProtocol {
    private static final int EQUALIZER_PRESET_PKT_LEN = 1;
    private static final byte EQUALIZER_PRESET_FEATURE = (byte)0x05;
    private static final byte EQUALIZER_PRESET_PDU_GET = (byte)0x02;
    private static final byte EQUALIZER_PRESET_PDU_SET = (byte)0x03;

    private static final int TOUCH_ACTIONS_PKT_LEN = 12;
    private static final byte TOUCH_ACTIONS_FEATURE = (byte)0x01;
    private static final byte TOUCH_ACTIONS_PDU_GET = (byte)0x02;
    private static final byte TOUCH_ACTIONS_PDU_SET = (byte)0x03;

    protected MoondropSpaceTravelProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] data) {
        List<GBDeviceEvent> events = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data);

        while (buf.hasRemaining()) {
            GaiaPacket packet = GaiaPacket.decode(buf);

            if (packet == null)
                break;

            if (packet.getPduType() != GaiaPacket.PDU_RESPONSE)
                continue;

            short featureId = packet.getFeatureId();
            short pduId = packet.getPduId();
            byte[] payload = packet.getPayload();

            if (featureId == EQUALIZER_PRESET_FEATURE && pduId == EQUALIZER_PRESET_PDU_GET)
                events.add(handlePacketEqualizerPreset(payload));
            else if (featureId == TOUCH_ACTIONS_FEATURE && pduId == TOUCH_ACTIONS_PDU_GET)
                events.add(handlePacketTouchActions(payload));
        }

        return events.toArray(new GBDeviceEvent[0]);
    }

    private GBDeviceEvent handlePacketEqualizerPreset(byte[] payload) {
        if (payload.length != EQUALIZER_PRESET_PKT_LEN)
            return null;

        byte preset = payload[0];

        return new GBDeviceEventUpdatePreferences(PREF_MOONDROP_EQUALIZER_PRESET, String.valueOf(preset));
    }

    private GBDeviceEvent handlePacketTouchActions(byte[] payload) {
        if (payload.length != TOUCH_ACTIONS_PKT_LEN)
            return null;

        Map<String, Object> prefs = new HashMap<>();

        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_PLAY_PAUSE_EARBUD, PREF_MOONDROP_TOUCH_PLAY_PAUSE_TRIGGER, payload[1]));
        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_MEDIA_PREV_EARBUD, PREF_MOONDROP_TOUCH_MEDIA_PREV_TRIGGER, payload[2]));
        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_MEDIA_NEXT_EARBUD, PREF_MOONDROP_TOUCH_MEDIA_NEXT_TRIGGER, payload[3]));
        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_CALL_PICK_HANG_EARBUD, PREF_MOONDROP_TOUCH_CALL_PICK_HANG_TRIGGER, payload[6]));
        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_CALL_START_EARBUD, PREF_MOONDROP_TOUCH_CALL_START_TRIGGER, payload[7]));
        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_ASSISTANT_EARBUD, PREF_MOONDROP_TOUCH_ASSISTANT_TRIGGER, payload[8]));
        prefs.putAll(decodeTouchAction(PREF_MOONDROP_TOUCH_ANC_MODE_EARBUD, PREF_MOONDROP_TOUCH_ANC_MODE_TRIGGER, payload[10]));

        return new GBDeviceEventUpdatePreferences(prefs);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        switch (config) {
            case PREF_MOONDROP_EQUALIZER_PRESET:
                return encodeSetEqualizerPreset();
            case PREF_MOONDROP_TOUCH_PLAY_PAUSE_EARBUD:
            case PREF_MOONDROP_TOUCH_PLAY_PAUSE_TRIGGER:
            case PREF_MOONDROP_TOUCH_MEDIA_PREV_EARBUD:
            case PREF_MOONDROP_TOUCH_MEDIA_PREV_TRIGGER:
            case PREF_MOONDROP_TOUCH_MEDIA_NEXT_EARBUD:
            case PREF_MOONDROP_TOUCH_MEDIA_NEXT_TRIGGER:
            case PREF_MOONDROP_TOUCH_CALL_PICK_HANG_EARBUD:
            case PREF_MOONDROP_TOUCH_CALL_PICK_HANG_TRIGGER:
            case PREF_MOONDROP_TOUCH_CALL_START_EARBUD:
            case PREF_MOONDROP_TOUCH_CALL_START_TRIGGER:
            case PREF_MOONDROP_TOUCH_ASSISTANT_EARBUD:
            case PREF_MOONDROP_TOUCH_ASSISTANT_TRIGGER:
            case PREF_MOONDROP_TOUCH_ANC_MODE_EARBUD:
            case PREF_MOONDROP_TOUCH_ANC_MODE_TRIGGER:
                return encodeSetTouchActions();
        }

        return super.encodeSendConfiguration(config);
    }

    public byte[] encodeGetEqualizerPreset() {
        return new GaiaPacket(EQUALIZER_PRESET_FEATURE, EQUALIZER_PRESET_PDU_GET).encode();
    }

    public byte[] encodeGetTouchActions() {
        return new GaiaPacket(TOUCH_ACTIONS_FEATURE, TOUCH_ACTIONS_PDU_GET).encode();
    }

    private byte[] encodeSetEqualizerPreset() {
        Prefs prefs = getDevicePrefs();
        byte preset = Byte.parseByte(prefs.getString(PREF_MOONDROP_EQUALIZER_PRESET, "0"));

        byte[] payload = new byte[] { preset };

        return new GaiaPacket(EQUALIZER_PRESET_FEATURE, EQUALIZER_PRESET_PDU_SET, payload).encode();
    }

    private byte[] encodeSetTouchActions() {
        Prefs prefs = getDevicePrefs();
        byte actionPlayPause = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_PLAY_PAUSE_EARBUD, PREF_MOONDROP_TOUCH_PLAY_PAUSE_TRIGGER);
        byte actionMediaPrev = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_MEDIA_PREV_EARBUD, PREF_MOONDROP_TOUCH_MEDIA_PREV_TRIGGER);
        byte actionMediaNext = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_MEDIA_NEXT_EARBUD, PREF_MOONDROP_TOUCH_MEDIA_NEXT_TRIGGER);
        byte actionCallPickHang = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_CALL_PICK_HANG_EARBUD, PREF_MOONDROP_TOUCH_CALL_PICK_HANG_TRIGGER);
        byte actionCallStart = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_CALL_START_EARBUD, PREF_MOONDROP_TOUCH_CALL_START_TRIGGER);
        byte actionAssistant = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_ASSISTANT_EARBUD, PREF_MOONDROP_TOUCH_ASSISTANT_TRIGGER);
        byte actionAncMode = encodeTouchAction(prefs, PREF_MOONDROP_TOUCH_ANC_MODE_EARBUD, PREF_MOONDROP_TOUCH_ANC_MODE_TRIGGER);

        byte[] payload = new byte[] {
            (byte)0x01,
            actionPlayPause,
            actionMediaPrev,
            actionMediaNext,
            (byte)0x30,
            (byte)0x30,
            actionCallPickHang,
            actionCallStart,
            actionAssistant,
            actionAssistant,
            actionAncMode,
            actionAncMode,
        };

        return new GaiaPacket(TOUCH_ACTIONS_FEATURE, TOUCH_ACTIONS_PDU_SET, payload).encode();
    }

    private byte encodeTouchAction(Prefs prefs, String prefEarbud, String prefTrigger) {
        int earbud = Integer.valueOf(prefs.getString(prefEarbud, "3"));
        int trigger = Integer.valueOf(prefs.getString(prefTrigger, "0"));

        return (byte)((earbud << 4) | trigger);
    }

    private Map<String, Object> decodeTouchAction(String prefEarbud, String prefTrigger, byte action) {
        int earbud = (action & 0xf0) >> 4;
        int trigger = action & 0x0f;

        return new HashMap<String, Object>() {{
            put(prefEarbud, String.valueOf(earbud));
            put(prefTrigger, String.valueOf(trigger));
        }};
    }
}
