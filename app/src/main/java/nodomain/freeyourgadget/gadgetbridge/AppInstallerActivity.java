package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class AppInstallerActivity extends Activity {

    private final String TAG = this.getClass().getSimpleName();

    TextView debugTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appinstaller);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        debugTextView = (TextView) findViewById(R.id.debugTextView);
        debugTextView.setText("contents:\n");
        Uri uri = getIntent().getData();

        ContentResolver cr = getContentResolver();
        InputStream fin = null;
        try {
            fin = cr.openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        ZipInputStream zis = new ZipInputStream(fin);
        ZipEntry ze = null;
        GBDeviceApp app;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().equals("appinfo.json")) {
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
                            debugTextView.setText("This is just a test, you cant install anything yet \n\n" + appName + " Version " + appVersion + " by " + appCreator + "\n");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
