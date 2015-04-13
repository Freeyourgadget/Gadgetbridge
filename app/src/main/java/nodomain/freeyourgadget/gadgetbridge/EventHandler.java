package nodomain.freeyourgadget.gadgetbridge;

public interface EventHandler {
    public void onSMS(String from, String body);

    public void onEmail(String from, String subject, String body);

    public void onSetTime(long ts);

    public void onSetCallState(String number, String name, GBCommand command);

    public void onSetMusicInfo(String artist, String album, String track);

    public void onFirmwareVersionReq();

    public void onAppInfoReq();

    public void onAppDelete(int id, int index);

    public void onPhoneVersion(byte os);
}
