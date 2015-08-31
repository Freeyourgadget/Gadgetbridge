package nodomain.freeyourgadget.gadgetbridge.service.serial;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;

public abstract class GBDeviceProtocol {

    public byte[] encodeSMS(String from, String body) {
        return null;
    }

    public byte[] encodeEmail(String from, String subject, String body) {
        return null;
    }

    public byte[] encodeGenericNotification(String title, String details, int handle) {
        return null;
    }

    public byte[] encodeSetTime() {
        return null;
    }

    public byte[] encodeSetCallState(String number, String name, ServiceCommand command) {
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

    public byte[] encodeScreenshotReq() {
        return null;
    }

    public byte[] encodeAppDelete(UUID uuid) {
        return null;
    }

    public byte[] encodeAppStart(UUID uuid) {
        return null;
    }

    public byte[] encodeSynchronizeActivityData() {
        return null;
    }

    public byte[] encodeReboot() {
        return null;
    }

    public byte[] encodeFindDevice(boolean start) {
        return null;
    }

    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        return null;
    }
}