package nodomain.freeyourgadget.gadgetbridge.service.serial;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

public abstract class GBDeviceProtocol {

    public byte[] encodeNotification(NotificationSpec notificationSpec) {
        return null;
    }

    public byte[] encodeSetTime() {
        return null;
    }

    public byte[] encodeSetCallState(String number, String name, int command) {
        return null;
    }

    public byte[] encodeSetMusicInfo(String artist, String album, String track, int duration, int trackCount, int trackNr) {
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

    public byte[] encodeAppStart(UUID uuid, boolean start) {
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

    public byte[] encodeEnableRealtimeSteps(boolean enable) {
        return null;
    }

    public byte[] encodeEnableHeartRateSleepSupport(boolean enable) {
        return null;
    }

    public byte[] encodeEnableRealtimeHeartRateMeasurement(boolean enable) { return null; }

    public byte[] encodeAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        return null;
    }

    public byte[] encodeDeleteCalendarEvent(int type, long id) {
        return null;
    }

    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        return null;
    }
}
