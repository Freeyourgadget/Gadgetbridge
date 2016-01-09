package nodomain.freeyourgadget.gadgetbridge.model;

public class NotificationSpec {
    public static final int FLAG_WEARABLE_REPLY = 0x00000001;

    public int flags;
    public int id;
    public String sender;
    public String phoneNumber;
    public String title;
    public String subject;
    public String body;
    public NotificationType type;
    public String sourceName;
    public String[] cannedReplies;
}
