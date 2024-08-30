package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class HuaweiBinAppParser {

    public static class HuaweiBinAppParseError extends Exception {
        public HuaweiBinAppParseError(String str) {
            super(str);
        }
    }

    public static class AppFileEntry {
        public String filename;
        public String path;
        public byte[] content;
    }

    private static final byte BIN_APP_MAGIC = (byte) 0xbe;

    private static final int SIGN_HEADER_LEN = 32;
    private static final byte[] SIGN_MAGIC = "hw signed app   ".getBytes();
    private static final byte[] SIGN_VERSION = "1000".getBytes();


    private String packageName;
    private final ArrayList<AppFileEntry> entries = new ArrayList<>();

    public String getPackageName() {
        return packageName;
    }

    public byte[] getEntryContent(String filename) {
        for (AppFileEntry en : entries) {
            if (en.filename.equals(filename)) {
                return en.content;
            }
        }
        return null;
    }

    private byte[] readData(ByteBuffer data) throws Exception {
        long len = data.getLong();
        if (len > Integer.MAX_VALUE || len <= 0) {
            throw new HuaweiBinAppParseError("Invalid data length");
        }
        byte[] newPayload = new byte[(int) len];
        data.get(newPayload, 0, (int) len);
        return newPayload;
    }

    private String readStringInternal(ByteBuffer data, int len) throws Exception {
        byte[] newPayload = new byte[len];
        data.get(newPayload, 0, len);
        return new String(newPayload, StandardCharsets.UTF_8);
    }


    private String readString(ByteBuffer data) throws Exception {
        int len = data.getInt();
        if (len <= 0 || len > data.remaining()) {
            throw new HuaweiBinAppParseError("Invalid string length");
        }
        return readStringInternal(data, len);
    }

    private String readEmptyString(ByteBuffer data) throws Exception {
        int len = data.getInt();
        if (len < 0 || len > data.remaining()) {
            throw new HuaweiBinAppParseError("Invalid string length");
        }
        return readStringInternal(data, len);
    }

    private int getSignLen(byte[] in) {
        byte[] signatureHeader = Arrays.copyOfRange(in, in.length - SIGN_HEADER_LEN, in.length);
        ByteBuffer signatureHeaderBuf = ByteBuffer.wrap(signatureHeader);
        byte[] magic = new byte[SIGN_MAGIC.length];
        signatureHeaderBuf.get(magic, 0, SIGN_MAGIC.length);
        if (!Arrays.equals(SIGN_MAGIC, magic))
            return 0;
        byte[] version = new byte[SIGN_VERSION.length];
        signatureHeaderBuf.get(version, 0, SIGN_VERSION.length);
        if (!Arrays.equals(SIGN_VERSION, version))
            return 0;
        //NOTE: we need only signature size.
        return signatureHeaderBuf.getInt();
    }

    public void parseData(byte[] in) throws Exception {
        //NOTE: Binary app file signed. We should avoid to read this signature.
        int signLen = getSignLen(in);
        ByteBuffer data = ByteBuffer.wrap(in);

        byte magic = data.get();
        if (magic != BIN_APP_MAGIC) {
            throw new HuaweiBinAppParseError("Invalid magic");
        }
        this.packageName = readString(data);
        while (data.remaining() > signLen) {
            AppFileEntry ent = new AppFileEntry();
            ent.filename = readString(data);
            ent.path = readEmptyString(data);
            ent.content = readData(data);
            entries.add(ent);
        }
    }
}
