package nodomain.freeyourgadget.gadgetbridge;

import java.util.UUID;

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

    void onAppStart(UUID uuid);

    void onAppDelete(UUID uuid);

    void onPhoneVersion(byte os);

    void onFetchActivityData();

    void onReboot();

}
