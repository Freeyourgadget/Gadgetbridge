package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ListView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.Widget;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class WidgetConfigurationActivity extends Activity {
    private static final Logger LOG = LoggerFactory.getLogger(Widget.class);
    int mAppWidgetId;

    LinkedHashMap<String, Pair<String, Integer>> allDevices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // make the result intent and set the result to canceled
        Intent resultValue; resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(WidgetConfigurationActivity.this);
        builder.setTitle(R.string.widget_settings_select_device_title);

        allDevices = getAllDevices(getApplicationContext());

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Pair<String, Integer>> item : allDevices.entrySet()) {
            list.add(item.getKey());
        }
        String[] allDevicesString = list.toArray(new String[0]);

        builder.setSingleChoiceItems(allDevicesString, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ListView lw = ((AlertDialog) dialog).getListView();
                int selectedItemPosition = lw.getCheckedItemPosition();

                if (selectedItemPosition > -1) {
                    Map.Entry<String, Pair<String, Integer>> selectedItem =
                            (Map.Entry<String, Pair<String, Integer>>) allDevices.entrySet().toArray()[selectedItemPosition];
                    WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
                    widgetPreferenceStorage.saveWidgetPrefs(getApplicationContext(), String.valueOf(mAppWidgetId), selectedItem.getValue().first);
                }
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent resultValue; resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_CANCELED, resultValue);
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public LinkedHashMap getAllDevices(Context appContext) {
        DaoSession daoSession;
        GBApplication gbApp = (GBApplication) appContext;
        LinkedHashMap<String, Pair<String, Integer>> newMap = new LinkedHashMap<>(1);
        List<? extends GBDevice> devices = gbApp.getDeviceManager().getDevices();

        try (DBHandler handler = GBApplication.acquireDB()) {
            daoSession = handler.getDaoSession();
            for (GBDevice device : devices) {
                DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                Device dbDevice = DBHelper.findDevice(device, daoSession);
                int icon = device.isInitialized() ? device.getType().getIcon() : device.getType().getDisabledIcon();
                if (dbDevice != null && coordinator != null
                        && (coordinator.supportsActivityDataFetching() || coordinator.supportsActivityTracking())
                        && !newMap.containsKey(device.getAliasOrName())) {
                    newMap.put(device.getAliasOrName(), new Pair(device.getAddress(), icon));
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting list of all devices: " + e);
        }
        return newMap;
    }
}
