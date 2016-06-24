package nodomain.freeyourgadget.gadgetbridge.model;

public class CannedMessagesSpec {
    public static final byte TYPE_GENERIC = 0;
    public static final byte TYPE_MISSEDCALLS = 1;
    public static final byte TYPE_NEWSMS = 2;

    public int type;
    public String[] cannedMessages;
}