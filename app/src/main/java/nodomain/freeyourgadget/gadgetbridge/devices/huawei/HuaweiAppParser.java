package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class HuaweiAppParser {

    public static class AppFileEntry {
        public String filename;
        public String folder;
        public String unknown;
        public byte[] content;
    }

    private String packageName;
    private final ArrayList<AppFileEntry> entries = new ArrayList<>();

    public String getPackageName() {
        return packageName;
    }

    public byte[] getEntryContent(String filename) {
        for(AppFileEntry en: entries) {
            if(en.filename.equals(filename)) {
                return en.content;
            }
        }
        return null;
    }

    private byte[] readData(ByteBuffer data) {
        int len = data.getInt();
        byte[] newPayload = new byte[len];
        data.get(newPayload, 0, len);
        return newPayload;
    }

    private String readString(ByteBuffer data) {
        return new String(readData(data));
    }

    public void parseData(byte[] in) throws Exception {

        ByteBuffer data = ByteBuffer.wrap(in);

        byte magic = data.get();
        if(magic != (byte)0xbe){
            throw new Exception("Invalid magic");
        }
        this.packageName = readString(data);
        while(data.remaining() > 0) {
            int pos = data.position();
            byte a = data.get();
            if(a != 0x00)
                break;
            data.position(pos);
            AppFileEntry ent = new AppFileEntry();
            ent.filename = readString(data);
            ent.folder = readString(data);
            ent.unknown = readString(data);
            ent.content = readData(data);
            entries.add(ent);
        }
    }
}
