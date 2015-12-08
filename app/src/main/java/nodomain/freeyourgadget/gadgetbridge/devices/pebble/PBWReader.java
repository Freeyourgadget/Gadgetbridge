package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleProtocol;

public class PBWReader {
    private static final Logger LOG = LoggerFactory.getLogger(PBWReader.class);
    private static final HashMap<String, Byte> appFileTypesMap;
    private static final HashMap<String, Byte> fwFileTypesMap;

    static {
        appFileTypesMap = new HashMap<>();
        appFileTypesMap.put("application", PebbleProtocol.PUTBYTES_TYPE_BINARY);
        appFileTypesMap.put("resources", PebbleProtocol.PUTBYTES_TYPE_RESOURCES);
        appFileTypesMap.put("worker", PebbleProtocol.PUTBYTES_TYPE_WORKER);
    }

    static {
        fwFileTypesMap = new HashMap<>();
        fwFileTypesMap.put("firmware", PebbleProtocol.PUTBYTES_TYPE_FIRMWARE);
        fwFileTypesMap.put("resources", PebbleProtocol.PUTBYTES_TYPE_SYSRESOURCES);
    }

    private final Uri uri;
    private final ContentResolver cr;
    private GBDeviceApp app;
    private ArrayList<PebbleInstallable> pebbleInstallables = null;
    private boolean isFirmware = false;
    private boolean isLanguage = false;
    private boolean isValid = false;
    private String hwRevision = null;
    private short mSdkVersion;
    private short mAppVersion;
    private int mIconId;
    private int mFlags;

    public PBWReader(Uri uri, Context context, String platform) throws FileNotFoundException {
        this.uri = uri;
        cr = context.getContentResolver();

        InputStream fin = new BufferedInputStream(cr.openInputStream(uri));

        if (uri.toString().endsWith(".pbl")) {
            STM32CRC stm32crc = new STM32CRC();
            try {
                byte[] buf = new byte[2000];
                while (fin.available() > 0) {
                    int count = fin.read(buf);
                    stm32crc.addData(buf, count);
                }
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int crc = stm32crc.getResult();
            // language file
            app = new GBDeviceApp(UUID.randomUUID(), "Language File", "unknown", "unknown", GBDeviceApp.Type.UNKNOWN);
            File f = new File(uri.getPath());

            pebbleInstallables = new ArrayList<>();
            pebbleInstallables.add(new PebbleInstallable("lang", (int) f.length(), crc, PebbleProtocol.PUTBYTES_TYPE_FILE));

            isValid = true;
            isLanguage = true;
            return;
        }

        String platformDir = "";

        if (!uri.toString().endsWith(".pbz")) {
            platformDir = platform + "/";

            if (platform.equals("aplite")) {
                boolean hasApliteDir = false;
                InputStream afin = new BufferedInputStream(cr.openInputStream(uri));

                ZipInputStream zis = new ZipInputStream(afin);
                ZipEntry ze;
                try {
                    while ((ze = zis.getNextEntry()) != null) {
                        if (ze.getName().startsWith("aplite/")) {
                            hasApliteDir = true;
                            break;
                        }
                    }
                    zis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!hasApliteDir) {
                    platformDir = "";
                }
            }
        }

        ZipInputStream zis = new ZipInputStream(fin);
        ZipEntry ze;
        pebbleInstallables = new ArrayList<>();
        byte[] buffer = new byte[1024];
        int count;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                if (fileName.equals(platformDir + "manifest.json")) {
                    long bytes = ze.getSize();
                    if (bytes > 8192) // that should be too much
                        break;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }

                    String jsonString = baos.toString();
                    try {
                        JSONObject json = new JSONObject(jsonString);
                        HashMap<String, Byte> fileTypeMap;

                        try {
                            JSONObject firmware = json.getJSONObject("firmware");
                            fileTypeMap = fwFileTypesMap;
                            isFirmware = true;
                            hwRevision = firmware.getString("hwrev");
                        } catch (JSONException e) {
                            fileTypeMap = appFileTypesMap;
                            isFirmware = false;
                        }
                        for (Map.Entry<String, Byte> entry : fileTypeMap.entrySet()) {
                            try {
                                JSONObject jo = json.getJSONObject(entry.getKey());
                                String name = jo.getString("name");
                                int size = jo.getInt("size");
                                long crc = jo.getLong("crc");
                                byte type = entry.getValue();
                                pebbleInstallables.add(new PebbleInstallable(platformDir + name, size, (int) crc, type));
                                LOG.info("found file to install: " + platformDir + name);
                                isValid = true;
                            } catch (JSONException e) {
                                // not fatal
                            }
                        }
                    } catch (JSONException e) {
                        // no JSON at all that is a problem
                        isValid = false;
                        e.printStackTrace();
                        break;
                    }

                } else if (fileName.equals("appinfo.json")) {
                    long bytes = ze.getSize();
                    if (bytes > 8192) // that should be too much
                        break;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }

                    String jsonString = baos.toString();
                    try {
                        JSONObject json = new JSONObject(jsonString);
                        String appName = json.getString("shortName");
                        String appCreator = json.getString("companyName");
                        String appVersion = json.getString("versionLabel");
                        UUID uuid = UUID.fromString(json.getString("uuid"));
                        if (appName != null && appCreator != null && appVersion != null) {
                            // FIXME: dont assume WATCHFACE
                            app = new GBDeviceApp(uuid, appName, appCreator, appVersion, GBDeviceApp.Type.WATCHFACE);
                        }

                    } catch (JSONException e) {
                        isValid = false;
                        e.printStackTrace();
                        break;
                    }
                } else if (fileName.equals(platformDir + "pebble-app.bin")) {
                    zis.read(buffer, 0, 108);
                    byte[] tmp_buf = new byte[32];
                    ByteBuffer buf = ByteBuffer.wrap(buffer);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    buf.getLong();  // header, TODO: verifiy
                    buf.getShort(); // struct version, TODO: verify
                    mSdkVersion = buf.getShort();
                    mAppVersion = buf.getShort();
                    buf.getShort(); // size
                    buf.getInt(); // offset
                    buf.getInt(); // crc
                    buf.get(tmp_buf, 0, 32); // app name
                    buf.get(tmp_buf, 0, 32); // author
                    mIconId = buf.getInt();
                    LOG.info("got icon id from pebble-app.bin: " + mIconId);
                    buf.getInt(); // symbol table addr
                    mFlags = buf.getInt();
                    LOG.info("got flags from pebble-app.bin: " + mFlags);
                    // more follows but, not interesting for us
                }
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isFirmware() {
        return isFirmware;
    }

    public boolean isLanguage() {
        return isLanguage;
    }

    public boolean isValid() {
        return isValid;
    }

    public GBDeviceApp getGBDeviceApp() {
        return app;
    }

    public InputStream getInputStreamFile(String filename) {
        InputStream fin;
        try {
            fin = new BufferedInputStream(cr.openInputStream(uri));
            if (isLanguage) {
                return fin;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        ZipInputStream zis = new ZipInputStream(fin);
        ZipEntry ze;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().equals(filename)) {
                    return zis;
                }
            }
            zis.close();
        } catch (Throwable e) {
            try {
                zis.close();
            } catch (IOException e1) {
                // ignore
            }
            e.printStackTrace();
        }
        return null;
    }

    public PebbleInstallable[] getPebbleInstallables() {
        if (pebbleInstallables == null) {
            return null;
        }
        return pebbleInstallables.toArray(new PebbleInstallable[pebbleInstallables.size()]);
    }

    public String getHWRevision() {
        return hwRevision;
    }

    public short getSdkVersion() {
        return mSdkVersion;
    }

    public short getAppVersion() {
        return mAppVersion;
    }

    public int getFlags() {
        return mFlags;
    }

    public int getIconId() {
        return mIconId;
    }
}