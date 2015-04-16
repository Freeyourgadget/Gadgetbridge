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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;

public class PBWReader {
    private static final String TAG = PebbleIoThread.class.getSimpleName();

    private GBDeviceApp app;
    private final Uri uri;
    private final ContentResolver cr;
    private ArrayList<PebbleInstallable> pebbleInstallables;

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
                        JSONObject application = json.getJSONObject("application");

                        String name = application.getString("name");
                        int size = application.getInt("size");
                        long crc = application.getLong("crc");
                        pebbleInstallables.add(new PebbleInstallable(name, size, (int) crc, PebbleProtocol.PUTBYTES_TYPE_BINARY));
                        Log.i(TAG, "found app binary to install: " + name);
                        try {
                            JSONObject resources = json.getJSONObject("resources");
                            name = resources.getString("name");
                            size = resources.getInt("size");
                            crc = resources.getLong("crc");
                            pebbleInstallables.add(new PebbleInstallable(name, size, (int) crc, PebbleProtocol.PUTBYTES_TYPE_RESOURCES));
                            Log.i(TAG, "found resources to install: " + name);
                        } catch (JSONException e) {
                            // no resources, that is no problem
                        }
                        try {
                            JSONObject worker = json.getJSONObject("worker");
                            name = worker.getString("name");
                            size = worker.getInt("size");
                            crc = worker.getLong("crc");
                            pebbleInstallables.add(new PebbleInstallable(name, size, (int) crc, PebbleProtocol.PUTBYTES_TYPE_WORKER));
                            Log.i(TAG, "found worker to install: " + name);
                        } catch (JSONException e) {
                            // no worker, that is no problem
                        }
                    } catch (JSONException e) {
                        // no application, that is a problem
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

}