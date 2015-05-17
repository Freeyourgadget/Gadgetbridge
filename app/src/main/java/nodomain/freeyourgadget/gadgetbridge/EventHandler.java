package nodomain.freeyourgadget.gadgetbridge;

public interface EventHandler {
    void onSMS(String from, String body);

    void onEmail(String from, String subject, String body);

    void onGenericNotification(String title, String details);

    void onSetTime(long ts);

    void onSetCallState(String number, String name, GBCommand command);

    void onSetMusicInfo(String artist, String album, String track);

    void onFirmwareVersionReq();

    void onBatteryInfoReq();

    void onAppInfoReq();

    void onAppDelete(int id, int index);

    void onPhoneVersion(byte os);

    void onReboot();
}
