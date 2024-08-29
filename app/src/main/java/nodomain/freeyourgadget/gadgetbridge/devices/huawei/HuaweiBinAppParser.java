package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class HuaweiBinAppParser {

    public static class AppFileEntry {
        public String filename;
        public String path;
        public byte[] content;
    }

    private static final int signHeaderLen = 32;
    private static final byte[] signMagic = "hw signed app   ".getBytes();
    private static final byte[] signVersion = "1000".getBytes();


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
        long len = data.getLong();
        byte[] newPayload = new byte[(int)len];
        data.get(newPayload, 0, (int)len);
        return newPayload;
    }

    private String readString(ByteBuffer data) {
        int len = data.getInt();
        byte[] newPayload = new byte[len];
        data.get(newPayload, 0, len);
        return new String(newPayload, StandardCharsets.UTF_8);
    }

    private int getSignLen(byte[] in) {
        byte[] signatureHeader = Arrays.copyOfRange(in, in.length - signHeaderLen, in.length);
        ByteBuffer signatureHeaderBuf = ByteBuffer.wrap(signatureHeader);
        byte[] magic = new byte[signMagic.length];
        signatureHeaderBuf.get(magic, 0, signMagic.length);
        if(!Arrays.equals(signMagic, magic))
            return 0;
        byte[] version = new byte[signVersion.length];
        signatureHeaderBuf.get(version, 0, signVersion.length);
        if(!Arrays.equals(signVersion, version))
            return 0;
        //NOTE: we need only signature size.
        return signatureHeaderBuf.getInt();
    }



    public void parseData(byte[] in) throws Exception {
        //NOTE: Binary app file signed. We should avoid to read this signature.
        int signLen = getSignLen(in);
        ByteBuffer data = ByteBuffer.wrap(in);

        byte magic = data.get();
        if(magic != (byte)0xbe){
            throw new Exception("Invalid magic");
        }
        this.packageName = readString(data);
        while(data.remaining() > signLen) {
            AppFileEntry ent = new AppFileEntry();
            ent.filename = readString(data);
            ent.path = readString(data);
            ent.content = readData(data);
            entries.add(ent);
        }
    }
}
