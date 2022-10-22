/*  Copyright (C) 2022 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1;

import static org.junit.Assert.*;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequest;

import org.junit.Test;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyProtocolImplV1Test {
    private final SonyProtocolImplV1 protocol = new SonyProtocolImplV1(null);

    @Test
    public void getAmbientSoundControl() {
        // TODO
        final Request request = protocol.getAmbientSoundControl();
        assertRequest(request, 0x0c, "66:02");
    }

    @Test
    public void setAmbientSoundControl() {
        // TODO
    }

    @Test
    public void getNoiseCancellingOptimizerState() {
        // TODO
    }

    @Test
    public void getAudioCodec() {
        // TODO
    }

    @Test
    public void getBattery() {
        // TODO
    }

    @Test
    public void getFirmwareVersion() {
        // TODO
    }

    @Test
    public void getAudioUpsampling() {
        // TODO
    }

    @Test
    public void setAudioUpsampling() {
        // TODO
    }

    @Test
    public void getAutomaticPowerOff() {
        // TODO
    }

    @Test
    public void setAutomaticPowerOff() {
        // TODO
    }

    @Test
    public void getButtonModes() {
        // TODO
    }

    @Test
    public void setButtonModes() {
        // TODO
        final Request request = protocol.setButtonModes(new ButtonModes(
                ButtonModes.Mode.AMBIENT_SOUND_CONTROL,
                ButtonModes.Mode.PLAYBACK_CONTROL
        ));
        assertRequest(request, "3e0c0100000005f806020020323c");
    }

    @Test
    public void getPauseWhenTakenOff() {
        // TODO
    }

    @Test
    public void setPauseWhenTakenOff() {
        // TODO
    }

    @Test
    public void getEqualizer() {
        // TODO
    }

    @Test
    public void setEqualizerPreset() {

    }

    @Test
    public void setEqualizerCustomBands() {
        // TODO
    }

    @Test
    public void getSoundPosition() {
        // TODO
    }

    @Test
    public void setSoundPosition() {
        // TODO
    }

    @Test
    public void getSurroundMode() {
        // TODO
    }

    @Test
    public void setSurroundMode() {
        // TODO
    }

    @Test
    public void getTouchSensor() {
        // TODO
    }

    @Test
    public void setTouchSensor() {
        // TODO
    }

    @Test
    public void getVoiceNotifications() {
        // TODO
    }

    @Test
    public void setVoiceNotifications() {
        // TODO
    }

    @Test
    public void startNoiseCancellingOptimizer() {
        // TODO
    }

    @Test
    public void powerOff() {
        // TODO
    }

    @Test
    public void handlePayload() {
        // TODO
    }

    @Test
    public void validInitPayload() {
        // TODO
    }

    @Test
    public void handleInitResponse() {
        // TODO
    }

    @Test
    public void handleAmbientSoundControl() {
        // TODO
    }

    @Test
    public void handleNoiseCancellingOptimizerStatus() {
        // TODO
    }

    @Test
    public void handleNoiseCancellingOptimizerState() {
        // TODO
    }

    @Test
    public void handleAudioUpsampling() {
        // TODO
    }

    @Test
    public void handleAutomaticPowerOff() {
        // TODO
    }

    @Test
    public void handleButtonModes() {
        // TODO
    }

    @Test
    public void handlePauseWhenTakenOff() {
        // TODO
    }

    @Test
    public void handleBattery() {
        // TODO
    }

    @Test
    public void handleAudioCodec() {
        // TODO
        final List<? extends GBDeviceEvent> event = protocol.handlePayload(
                MessageType.fromCode((byte) 0x0c),
                GB.hexStringToByteArray("1b:00:01".replace(":", ""))
        );

        assertEquals("Expect 2 events", 2, event.size());
    }

    @Test
    public void handleEqualizer() {
        // TODO
    }

    @Test
    public void handleFirmwareVersion() {
        // TODO
    }

    @Test
    public void handleJson() {
        // TODO
    }

    @Test
    public void handleAutomaticPowerOffButtonMode() {
        // TODO
    }

    @Test
    public void handleVirtualSound() {
        // TODO
    }

    @Test
    public void handleSoundPosition() {
        // TODO
    }

    @Test
    public void handleSurroundMode() {
        // TODO
    }

    @Test
    public void handleTouchSensor() {
        // TODO
    }

    @Test
    public void handleVoiceNotifications() {
        // TODO
    }

    @Test
    public void booleanFromByte() {
        assertEquals(Boolean.FALSE, protocol.booleanFromByte((byte) 0x00));
        assertEquals(Boolean.TRUE, protocol.booleanFromByte((byte) 0x01));
        assertNull(protocol.booleanFromByte((byte) 0x02));
    }

    @Test
    public void supportsWindNoiseCancelling() {
        // TODO
    }
}
