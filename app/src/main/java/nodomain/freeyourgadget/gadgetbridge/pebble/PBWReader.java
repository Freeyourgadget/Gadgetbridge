package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

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
    private GBDeviceApp app;
    private final Uri uri;
    private final ContentResolver cr;
    private ArrayList<String> filesToInstall;

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
        ZipEntry ze = null;
        filesToInstall = new ArrayList<String>();
        try {
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                if (fileName.equals("pebble-app.bin") || fileName.equals("pebble-worker.bin") || fileName.equals("app_resources.pbpack")) {
                    filesToInstall.add(fileName);  // FIXME: do not hardcode filenames above
                } else if (fileName.equals("appinfo.json")) {
                    long bytes = ze.getSize();
                    if (bytes > 8192) // that should be too much
                        break;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
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

    public int getFileSize(String filename) {
        InputStream fin = null;
        try {
            fin = new BufferedInputStream(cr.openInputStream(uri));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        }

        ZipInputStream zis = new ZipInputStream(fin);
        ZipEntry ze = null;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().equals(filename)) {
                    return (int) ze.getSize();
                }
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String[] getFilesToInstall() {
        return filesToInstall.toArray(new String[filesToInstall.size()]);
    }

}