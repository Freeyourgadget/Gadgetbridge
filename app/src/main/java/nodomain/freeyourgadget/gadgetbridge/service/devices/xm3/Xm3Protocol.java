package nodomain.freeyourgadget.gadgetbridge.service.devices.xm3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.xm3.SoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.xm3.SoundPosition;
import nodomain.freeyourgadget.gadgetbridge.devices.xm3.SurroundMode;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class Xm3Protocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(Xm3Protocol.class);

    private static final byte PACKET_HEADER = 0x3e;
    private static final byte PACKET_TRAILER = 0x3c;

    public Xm3Protocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] res) {
        return null;
    }

    public byte[] encodeTestNewFunction() {
        return encodeCommand(new byte[] { 0x0c, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00 });
    }

    private byte[] encodeSoundControl(SoundControl soundControl) {
        return null;
    }

    private byte[] encodeAmbientSound(int level, boolean focusOnVoice) {
        if (level < 1 || level > 20) {
            throw new IllegalArgumentException("Level must be between 1 and 20");
        }

        return null;
    }

    private byte[] encodeTimeout(int minutes) {
        switch(minutes) {
            case 5: case 30: case 60:
                break;
            default:
                throw new IllegalArgumentException("Invalid timeout value");
        }
        return null;
    }

    private byte[] encodeOptimizer() {
        return null;
    }

    private byte[] encodeSoundPosition(SoundPosition position) {
        return null;
    }

    private byte[] encodeSurround(SurroundMode mode) {
        return null;
    }

    private byte[] encodeSoundQuality(boolean high) {
        return null;
    }

    private byte[] encodeEqualizer(int clearBass, int[] bands) {
        if (bands.length != 5) {
            throw new IllegalArgumentException("There must be 5 bands");
        }

        return null;
    }

    private byte[] encodeDSEEHX(boolean enabled) {
        return null;
    }

    private byte[] encodeButtonFunction(boolean assistant) {
        return null;
    }

    private byte[] encodeCommand(byte... params) {
        byte[] cmd = new byte[params.length + 2];

        cmd[0] = PACKET_HEADER;
        cmd[cmd.length - 1] = PACKET_TRAILER;
        System.arraycopy(params, 0, cmd, 1, params.length);
        cmd[cmd.length - 2] = calcChecksum(cmd);

        return cmd;
    }

    private byte calcChecksum(byte[] packet) {
        int chk = 0;
        for (int i = 1; i < packet.length - 2; i++) {
            chk += packet[i] & 255;
        }
        return (byte) chk;
    }
}
