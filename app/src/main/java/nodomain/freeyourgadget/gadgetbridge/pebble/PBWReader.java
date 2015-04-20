package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;

public class PBWReader {
    private static final String TAG = PebbleIoThread.class.getSimpleName();
    private static final HashMap<String, Byte> appFileTypesMap;

    static {
        appFileTypesMap = new HashMap<String, Byte>();
        appFileTypesMap.put("application", PebbleProtocol.PUTBYTES_TYPE_BINARY);
        appFileTypesMap.put("resources", PebbleProtocol.PUTBYTES_TYPE_RESOURCES);
        appFileTypesMap.put("worker", PebbleProtocol.PUTBYTES_TYPE_WORKER);
    }

    private static final HashMap<String, Byte> fwFileTypesMap;

    static {
        fwFileTypesMap = new HashMap<String, Byte>();
        fwFileTypesMap.put("firmware", PebbleProtocol.PUTBYTES_TYPE_FIRMWARE);
        fwFileTypesMap.put("resources", PebbleProtocol.PUTBYTES_TYPE_SYSRESOURCES);
    }

    private final Uri uri;
    private final ContentResolver cr;
    private GBDeviceApp app;
    private ArrayList<PebbleInstallable> pebbleInstallables;
    private boolean isFirmware = false;
    private String hwRevision = null;

    public PBWReader(Uri uri, Context context) {
        this.uri = uri;
        cr = context.getContentResolver();

        InputStream fin = null;
        try {
            fin = new BufferedInputStream(cr.openInputStream(uri));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        ZipInputStream zis = new ZipInputStream(fin);
        ZipEntry ze;
        pebbleInstallables = new ArrayList<PebbleInstallable>();
        byte[] buffer = new byte[1024];
        int count;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                if (fileName.equals("manifest.json")) {
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
                                pebbleInstallables.add(new PebbleInstallable(name, size, (int) crc, type));
                                Log.i(TAG, "found file to install: " + name);
                            } catch (JSONException e) {
                                // not fatal
                            }
                        }
                    } catch (JSONException e) {
                        // no JSON at all that is a problem
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
                        if (appName != null && appCreator != null && appVersion != null) {
                            // FIXME: dont assume WATCHFACE
                            app = new GBDeviceApp(-1, -1, appName, appCreator, appVersion, GBDeviceApp.Type.WATCHFACE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean isFirmware() {
        return isFirmware;
    }

    public GBDeviceApp getGBDeviceApp() {
        return app;
    }

    public ZipInputStream getInputStreamFile(String filename) {
        InputStream fin = null;
        try {
            fin = new BufferedInputStream(cr.openInputStream(uri));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ZipInputStream zis = new ZipInputStream(fin);
        ZipEntry ze = null;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().equals(filename)) {
                    return zis;
                }
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PebbleInstallable[] getPebbleInstallables() {
        return pebbleInstallables.toArray(new PebbleInstallable[pebbleInstallables.size()]);
    }

    public String getHWRevision() {
        return hwRevision;
    }
}