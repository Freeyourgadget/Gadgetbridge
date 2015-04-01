package nodomain.freeyourgadget.gadgetbridge.protocol;

import nodomain.freeyourgadget.gadgetbridge.GBCommand;

public abstract class GBDeviceProtocol {

    public byte[] encodeSMS(String from, String body) {
        return null;
    }

    public byte[] encodeEmail(String from, String subject, String body) {
        return null;
    }

    public byte[] encodeSetTime(long ts) {
        return null;
    }

    public byte[] encodeSetCallState(String number, String name, GBCommand command) {
        return null;
    }

    public byte[] encodeSetMusicInfo(String artist, String album, String track) {
        return null;
    }

    public byte[] encodeFirmwareVersionReq() {
        return null;
    }

    public byte[] encodeAppInfoReq() {
        return null;
    }

    public byte[] encodeAppDelete(int id, int index) {
        return null;
    }

    public byte[] encodePhoneVersion(byte os) {
        return null;
    }

    public GBDeviceCommand decodeResponse(byte[] responseData) {
        return null;
    }
}