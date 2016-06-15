package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.woxthebox.draglistview.DragListView;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ExternalPebbleJSActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAppAdapter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public abstract class AbstractAppManagerFragment extends Fragment {
    public static final String ACTION_REFRESH_APPLIST
            = "nodomain.freeyourgadget.gadgetbridge.appmanager.action.refresh_applist";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAppManagerFragment.class);


    public void refreshList() {
    }

    public String getSortFilename() {
        return null;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GBApplication.ACTION_QUIT)) {
                //   finish();
            } else if (action.equals(ACTION_REFRESH_APPLIST)) {
                int appCount = intent.getIntExtra("app_count", 0);
                for (Integer i = 0; i < appCount; i++) {
                    String appName = intent.getStringExtra("app_name" + i.toString());
                    String appCreator = intent.getStringExtra("app_creator" + i.toString());
                    UUID uuid = UUID.fromString(intent.getStringExtra("app_uuid" + i.toString()));
                    GBDeviceApp.Type appType = GBDeviceApp.Type.values()[intent.getIntExtra("app_type" + i.toString(), 0)];

                    boolean found = false;
                    for (final ListIterator<GBDeviceApp> iter = appList.listIterator(); iter.hasNext(); ) {
                        final GBDeviceApp app = iter.next();
                        if (app.getUUID().equals(uuid)) {
                            app.setOnDevice(true);
                            iter.set(app);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        GBDeviceApp app = new GBDeviceApp(uuid, appName, appCreator, "", appType);
                        app.setOnDevice(true);
                        appList.add(app);
                    }
                }

                mGBDeviceAppAdapter.notifyDataSetChanged();
            }
        }
    };

    private Prefs prefs;

    protected final List<GBDeviceApp> appList = new ArrayList<>();
    private GBDeviceAppAdapter mGBDeviceAppAdapter;
    private GBDeviceApp selectedApp = null;
    protected GBDevice mGBDevice = null;

    protected List<GBDeviceApp> getSystemApps() {
        List<GBDeviceApp> systemApps = new ArrayList<>();
        if (prefs.getBoolean("pebble_force_untested", false)) {
            systemApps.add(new GBDeviceApp(UUID.fromString("4dab81a6-d2fc-458a-992c-7a1f3b96a970"), "Sports (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
            systemApps.add(new GBDeviceApp(UUID.fromString("cf1e816a-9db0-4511-bbb8-f60c48ca8fac"), "Golf (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        }
        if (mGBDevice != null && !"aplite".equals(PebbleUtils.getPlatformName(mGBDevice.getHardwareVersion()))) {
            systemApps.add(new GBDeviceApp(PebbleProtocol.UUID_PEBBLE_HEALTH, "Health (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        }

        return systemApps;
    }

    protected List<GBDeviceApp> getSystemWatchfaces() {
        List<GBDeviceApp> systemWatchfaces = new ArrayList<>();
        systemWatchfaces.add(new GBDeviceApp(UUID.fromString("8f3c8686-31a1-4f5f-91f5-01600c9bdc59"), "Tic Toc (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
        return systemWatchfaces;
    }

    protected List<GBDeviceApp> getCachedApps() {
        List<GBDeviceApp> cachedAppList = new ArrayList<>();
        File cachePath;
        try {
            cachePath = new File(FileUtils.getExternalFilesDir().getPath() + "/pbw-cache");
        } catch (IOException e) {
            LOG.warn("could not get external dir while reading pbw cache.");
            return cachedAppList;
        }

        File files[] = cachePath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".pbw")) {
                    String baseName = file.getName().substring(0, file.getName().length() - 4);
                    //metadata
                    File jsonFile = new File(cachePath, baseName + ".json");
                    //configuration
                    File configFile = new File(cachePath, baseName + "_config.js");
                    try {
                        String jsonstring = FileUtils.getStringFromFile(jsonFile);
                        JSONObject json = new JSONObject(jsonstring);
                        cachedAppList.add(new GBDeviceApp(json, configFile.exists()));
                    } catch (Exception e) {
                        LOG.warn("could not read json file for " + baseName, e.getMessage(), e);
                        cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), baseName, "N/A", "", GBDeviceApp.Type.UNKNOWN));
                    }
                }
            }
        }
        return cachedAppList;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mGBDevice = ((AppManagerActivity) getActivity()).getGBDevice();

        prefs = GBApplication.getPrefs();

        refreshList();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GBApplication.ACTION_QUIT);
        filter.addAction(ACTION_REFRESH_APPLIST);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);

        GBApplication.deviceService().onAppInfoReq();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_appmanager, container, false);

        DragListView appListView = (DragListView) (rootView.findViewById(R.id.appListView));
        appListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mGBDeviceAppAdapter = new GBDeviceAppAdapter(appList, R.layout.item_with_details, R.id.item_image, true, this);
        appListView.setAdapter(mGBDeviceAppAdapter, false);
        //registerForContextMenu(appListView);
        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.appmanager_context, menu);
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedApp = appList.get(acmi.position);

        if (!selectedApp.isInCache()) {
            menu.removeItem(R.id.appmanager_app_reinstall);
            menu.removeItem(R.id.appmanager_app_delete_cache);
        }
        if (!PebbleProtocol.UUID_PEBBLE_HEALTH.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_health_activate);
            menu.removeItem(R.id.appmanager_health_deactivate);
        }
        if (selectedApp.getType() == GBDeviceApp.Type.APP_SYSTEM) {
            menu.removeItem(R.id.appmanager_app_delete);
        }
        if (!selectedApp.isConfigurable()) {
            menu.removeItem(R.id.appmanager_app_configure);
        }
        if (mGBDevice != null && !mGBDevice.getFirmwareVersion().startsWith("v3")) {
            menu.removeItem(R.id.appmanager_app_move_to_top);
        }
        menu.setHeaderTitle(selectedApp.getName());
    }

    private void removeAppFromList(UUID uuid) {
        for (final ListIterator<GBDeviceApp> iter = appList.listIterator(); iter.hasNext(); ) {
            final GBDeviceApp app = iter.next();
            if (app.getUUID().equals(uuid)) {
                iter.remove();
                mGBDeviceAppAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.appmanager_health_deactivate:
            case R.id.appmanager_app_delete_cache:
                String baseName;
                try {
                    baseName = FileUtils.getExternalFilesDir().getPath() + "/pbw-cache/" + selectedApp.getUUID();
                } catch (IOException e) {
                    LOG.warn("could not get external dir while trying to access pbw cache.");
                    return true;
                }

                String[] suffixToDelete = new String[]{".pbw", ".json", "_config.js"};

                for (String suffix : suffixToDelete) {
                    File fileToDelete = new File(baseName + suffix);
                    if (!fileToDelete.delete()) {
                        LOG.warn("could not delete file from pbw cache: " + fileToDelete.toString());
                    } else {
                        LOG.info("deleted file: " + fileToDelete.toString());
                    }
                }
                removeAppFromList(selectedApp.getUUID());
                // fall through
            case R.id.appmanager_app_delete:
                GBApplication.deviceService().onAppDelete(selectedApp.getUUID());
                return true;
            case R.id.appmanager_app_reinstall:
                File cachePath;
                try {
                    cachePath = new File(FileUtils.getExternalFilesDir().getPath() + "/pbw-cache/" + selectedApp.getUUID() + ".pbw");
                } catch (IOException e) {
                    LOG.warn("could not get external dir while trying to access pbw cache.");
                    return true;
                }
                GBApplication.deviceService().onInstallApp(Uri.fromFile(cachePath));
                return true;
            case R.id.appmanager_health_activate:
                GBApplication.deviceService().onInstallApp(Uri.parse("fake://health"));
                return true;
            case R.id.appmanager_app_configure:
                GBApplication.deviceService().onAppStart(selectedApp.getUUID(), true);

                Intent startIntent = new Intent(getContext().getApplicationContext(), ExternalPebbleJSActivity.class);
                startIntent.putExtra("app_uuid", selectedApp.getUUID());
                startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
                startActivity(startIntent);
                return true;
            case R.id.appmanager_app_move_to_top:
                GBApplication.deviceService().onAppReorder(new UUID[]{selectedApp.getUUID()});
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
