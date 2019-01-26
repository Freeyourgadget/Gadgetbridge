/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Konrad Iturbe, Lem Dulfo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ExternalPebbleJSActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAppAdapter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;


public abstract class AbstractAppManagerFragment extends Fragment {
    public static final String ACTION_REFRESH_APPLIST
            = "nodomain.freeyourgadget.gadgetbridge.appmanager.action.refresh_applist";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAppManagerFragment.class);

    private ItemTouchHelper appManagementTouchHelper;

    protected abstract List<GBDeviceApp> getSystemAppsInCategory();

    protected abstract String getSortFilename();

    protected abstract boolean isCacheManager();

    protected abstract boolean filterApp(GBDeviceApp gbDeviceApp);

    public void startDragging(RecyclerView.ViewHolder viewHolder) {
        appManagementTouchHelper.startDrag(viewHolder);
    }

    protected void onChangedAppOrder() {
        List<UUID> uuidList = new ArrayList<>();
        for (GBDeviceApp gbDeviceApp : mGBDeviceAppAdapter.getAppList()) {
            uuidList.add(gbDeviceApp.getUUID());
        }
        AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuidList);
    }

    protected void refreshList() {
        appList.clear();
        ArrayList<UUID> uuids = AppManagerActivity.getUuidsFromFile(getSortFilename());
        List<GBDeviceApp> systemApps = getSystemAppsInCategory();
        boolean needsRewrite = false;
        for (GBDeviceApp systemApp : systemApps) {
            if (!uuids.contains(systemApp.getUUID())) {
                uuids.add(systemApp.getUUID());
                needsRewrite = true;
            }
        }
        if (needsRewrite) {
            AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuids);
        }
        appList.addAll(getCachedApps(uuids));
    }

    private void refreshListFromPebble(Intent intent) {
        appList.clear();
        int appCount = intent.getIntExtra("app_count", 0);
        for (int i = 0; i < appCount; i++) {
            String appName = intent.getStringExtra("app_name" + i);
            String appCreator = intent.getStringExtra("app_creator" + i);
            UUID uuid = UUID.fromString(intent.getStringExtra("app_uuid" + i));
            GBDeviceApp.Type appType = GBDeviceApp.Type.values()[intent.getIntExtra("app_type" + i, 0)];

            GBDeviceApp app = new GBDeviceApp(uuid, appName, appCreator, "", appType);
            app.setOnDevice(true);
            if (filterApp(app)) {
                appList.add(app);
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_REFRESH_APPLIST)) {
                if (intent.hasExtra("app_count")) {
                    LOG.info("got app info from pebble");
                    if (!isCacheManager()) {
                        LOG.info("will refresh list based on data from pebble");
                        refreshListFromPebble(intent);
                    }
                } else if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) >= 3 || isCacheManager()) {
                    refreshList();
                }
                mGBDeviceAppAdapter.notifyDataSetChanged();
            }
        }
    };

    protected final List<GBDeviceApp> appList = new ArrayList<>();
    private GBDeviceAppAdapter mGBDeviceAppAdapter;
    protected GBDevice mGBDevice = null;

    protected List<GBDeviceApp> getCachedApps(List<UUID> uuids) {
        List<GBDeviceApp> cachedAppList = new ArrayList<>();
        File cachePath;
        try {
            cachePath = PebbleUtils.getPbwCacheDir();
        } catch (IOException e) {
            LOG.warn("could not get external dir while reading pbw cache.");
            return cachedAppList;
        }

        File[] files;
        if (uuids == null) {
            files = cachePath.listFiles();
        } else {
            files = new File[uuids.size()];
            int index = 0;
            for (UUID uuid : uuids) {
                files[index++] = new File(uuid.toString() + ".pbw");
            }
        }
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
                        LOG.info("could not read json file for " + baseName);
                        //FIXME: this is really ugly, if we do not find system uuids in pbw cache add them manually. Also duplicated code
                        switch (baseName) {
                            case "8f3c8686-31a1-4f5f-91f5-01600c9bdc59":
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Tic Toc (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
                                break;
                            case "1f03293d-47af-4f28-b960-f2b02a6dd757":
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Music (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                break;
                            case "b2cae818-10f8-46df-ad2b-98ad2254a3c1":
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Notifications (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                break;
                            case "67a32d95-ef69-46d4-a0b9-854cc62f97f9":
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Alarms (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                break;
                            case "18e443ce-38fd-47c8-84d5-6d0c775fbe55":
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Watchfaces (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                break;
                            case "0863fc6a-66c5-4f62-ab8a-82ed00a98b5d":
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Send Text (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                break;
                        }
                        /*
                        else if (baseName.equals("4dab81a6-d2fc-458a-992c-7a1f3b96a970")) {
                            cachedAppList.add(new GBDeviceApp(UUID.fromString("4dab81a6-d2fc-458a-992c-7a1f3b96a970"), "Sports (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                        } else if (baseName.equals("cf1e816a-9db0-4511-bbb8-f60c48ca8fac")) {
                            cachedAppList.add(new GBDeviceApp(UUID.fromString("cf1e816a-9db0-4511-bbb8-f60c48ca8fac"), "Golf (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                        }
                        */
                        if (mGBDevice != null) {
                            if (PebbleUtils.hasHealth(mGBDevice.getModel())) {
                                if (baseName.equals(PebbleProtocol.UUID_PEBBLE_HEALTH.toString())) {
                                    cachedAppList.add(new GBDeviceApp(PebbleProtocol.UUID_PEBBLE_HEALTH, "Health (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    continue;
                                }
                            }
                            if (PebbleUtils.hasHRM(mGBDevice.getModel())) {
                                if (baseName.equals(PebbleProtocol.UUID_WORKOUT.toString())) {
                                    cachedAppList.add(new GBDeviceApp(PebbleProtocol.UUID_WORKOUT, "Workout (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    continue;
                                }
                            }
                            if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) >= 4) {
                                if (baseName.equals("3af858c3-16cb-4561-91e7-f1ad2df8725f")) {
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Kickstart (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
                                }
                                if (baseName.equals(PebbleProtocol.UUID_WEATHER.toString())) {
                                    cachedAppList.add(new GBDeviceApp(PebbleProtocol.UUID_WEATHER, "Weather (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                }
                            }
                        }
                        if (uuids == null) {
                            cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), baseName, "N/A", "", GBDeviceApp.Type.UNKNOWN));
                        }
                    }
                }
            }
        }
        return cachedAppList;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGBDevice = ((AppManagerActivity) getActivity()).getGBDevice();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REFRESH_APPLIST);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);

        if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) < 3) {
            GBApplication.deviceService().onAppInfoReq();
            if (isCacheManager()) {
                refreshList();
            }
        } else {
            refreshList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final FloatingActionButton appListFab = ((FloatingActionButton) getActivity().findViewById(R.id.fab));
        View rootView = inflater.inflate(R.layout.activity_appmanager, container, false);

        RecyclerView appListView = (RecyclerView) (rootView.findViewById(R.id.appListView));

        appListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    appListFab.hide();
                } else if (dy < 0) {
                    appListFab.show();
                }
            }
        });
        appListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mGBDeviceAppAdapter = new GBDeviceAppAdapter(appList, R.layout.item_pebble_watchapp, this);
        appListView.setAdapter(mGBDeviceAppAdapter);

        ItemTouchHelper.Callback appItemTouchHelperCallback = new AppItemTouchHelperCallback(mGBDeviceAppAdapter);
        appManagementTouchHelper = new ItemTouchHelper(appItemTouchHelperCallback);

        appManagementTouchHelper.attachToRecyclerView(appListView);
        return rootView;
    }

    protected void sendOrderToDevice(String concatFilename) {
        ArrayList<UUID> uuids = new ArrayList<>();
        for (GBDeviceApp gbDeviceApp : mGBDeviceAppAdapter.getAppList()) {
            uuids.add(gbDeviceApp.getUUID());
        }
        if (concatFilename != null) {
            ArrayList<UUID> concatUuids = AppManagerActivity.getUuidsFromFile(concatFilename);
            uuids.addAll(concatUuids);
        }
        GBApplication.deviceService().onAppReorder(uuids.toArray(new UUID[uuids.size()]));
    }

    public boolean openPopupMenu(View view, GBDeviceApp deviceApp) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.appmanager_context, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();
        final GBDeviceApp selectedApp = deviceApp;

        if (!selectedApp.isInCache()) {
            menu.removeItem(R.id.appmanager_app_reinstall);
            menu.removeItem(R.id.appmanager_app_delete_cache);
        }
        if (!PebbleProtocol.UUID_PEBBLE_HEALTH.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_health_activate);
            menu.removeItem(R.id.appmanager_health_deactivate);
        }
        if (!PebbleProtocol.UUID_WORKOUT.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_hrm_activate);
            menu.removeItem(R.id.appmanager_hrm_deactivate);
        }
        if (!PebbleProtocol.UUID_WEATHER.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_weather_activate);
            menu.removeItem(R.id.appmanager_weather_deactivate);
            menu.removeItem(R.id.appmanager_weather_install_provider);
        }
        if (selectedApp.getType() == GBDeviceApp.Type.APP_SYSTEM || selectedApp.getType() == GBDeviceApp.Type.WATCHFACE_SYSTEM) {
            menu.removeItem(R.id.appmanager_app_delete);
        }
        if (!selectedApp.isConfigurable()) {
            menu.removeItem(R.id.appmanager_app_configure);
        }

        if (PebbleProtocol.UUID_WEATHER.equals(selectedApp.getUUID())) {
            PackageManager pm = getActivity().getPackageManager();
            try {
                pm.getPackageInfo("ru.gelin.android.weather.notification", PackageManager.GET_ACTIVITIES);
                menu.removeItem(R.id.appmanager_weather_install_provider);
            } catch (PackageManager.NameNotFoundException e) {
                menu.removeItem(R.id.appmanager_weather_activate);
                menu.removeItem(R.id.appmanager_weather_deactivate);
            }
        }

        switch (selectedApp.getType()) {
            case WATCHFACE:
            case APP_GENERIC:
            case APP_ACTIVITYTRACKER:
                break;
            default:
                menu.removeItem(R.id.appmanager_app_openinstore);
        }
        //menu.setHeaderTitle(selectedApp.getName());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                 public boolean onMenuItemClick(MenuItem item) {
                                                     return onContextItemSelected(item, selectedApp);
                                                 }
                                             }
        );

        popupMenu.show();
        return true;
    }

    private boolean onContextItemSelected(MenuItem item, GBDeviceApp selectedApp) {
        switch (item.getItemId()) {
            case R.id.appmanager_app_delete_cache:
                File pbwCacheDir;
                try {
                    pbwCacheDir = PebbleUtils.getPbwCacheDir();
                } catch (IOException e) {
                    LOG.warn("could not get external dir while trying to access pbw cache.");
                    return true;
                }
                String baseName = selectedApp.getUUID().toString();
                String[] suffixToDelete = new String[]{".pbw", ".json", "_config.js", "_preset.json"};

                for (String suffix : suffixToDelete) {
                    File fileToDelete = new File(pbwCacheDir,baseName + suffix);
                    if (!fileToDelete.delete()) {
                        LOG.warn("could not delete file from pbw cache: " + fileToDelete.toString());
                    } else {
                        LOG.info("deleted file: " + fileToDelete.toString());
                    }
                }
                AppManagerActivity.deleteFromAppOrderFile("pbwcacheorder.txt", selectedApp.getUUID()); // FIXME: only if successful
                // fall through
            case R.id.appmanager_app_delete:
                if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) >= 3) {
                    AppManagerActivity.deleteFromAppOrderFile(mGBDevice.getAddress() + ".watchapps", selectedApp.getUUID()); // FIXME: only if successful
                    AppManagerActivity.deleteFromAppOrderFile(mGBDevice.getAddress() + ".watchfaces", selectedApp.getUUID()); // FIXME: only if successful
                    Intent refreshIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(refreshIntent);
                }
                GBApplication.deviceService().onAppDelete(selectedApp.getUUID());
                return true;
            case R.id.appmanager_app_reinstall:
                File cachePath;
                try {
                    cachePath = new File(PebbleUtils.getPbwCacheDir(), selectedApp.getUUID() + ".pbw");
                } catch (IOException e) {
                    LOG.warn("could not get external dir while trying to access pbw cache.");
                    return true;
                }
                GBApplication.deviceService().onInstallApp(Uri.fromFile(cachePath));
                return true;
            case R.id.appmanager_health_activate:
                GBApplication.deviceService().onInstallApp(Uri.parse("fake://health"));
                return true;
            case R.id.appmanager_hrm_activate:
                GBApplication.deviceService().onInstallApp(Uri.parse("fake://hrm"));
                return true;
            case R.id.appmanager_weather_activate:
                GBApplication.deviceService().onInstallApp(Uri.parse("fake://weather"));
                return true;
            case R.id.appmanager_health_deactivate:
            case R.id.appmanager_hrm_deactivate:
            case R.id.appmanager_weather_deactivate:
                GBApplication.deviceService().onAppDelete(selectedApp.getUUID());
                return true;
            case R.id.appmanager_weather_install_provider:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/app/ru.gelin.android.weather.notification")));
                return true;
            case R.id.appmanager_app_configure:
                GBApplication.deviceService().onAppStart(selectedApp.getUUID(), true);

                Intent startIntent = new Intent(getContext().getApplicationContext(), ExternalPebbleJSActivity.class);
                startIntent.putExtra(DeviceService.EXTRA_APP_UUID, selectedApp.getUUID());
                startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
                startIntent.putExtra(ExternalPebbleJSActivity.SHOW_CONFIG, true);
                startActivity(startIntent);
                return true;
            case R.id.appmanager_app_openinstore:
                String url = "https://pebble-appstore.romanport.com/" + ((selectedApp.getType() == GBDeviceApp.Type.WATCHFACE) ? "watchfaces" : "watchapps") + "/0/?query=" + selectedApp.getName();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
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

    public class AppItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final GBDeviceAppAdapter gbDeviceAppAdapter;

        public AppItemTouchHelperCallback(GBDeviceAppAdapter gbDeviceAppAdapter) {
            this.gbDeviceAppAdapter = gbDeviceAppAdapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            //app reordering is not possible on old firmwares
            if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) < 3 && !isCacheManager()) {
                return 0;
            }
            //we only support up and down movement and only for moving, not for swiping apps away
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            gbDeviceAppAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            //nothing to do
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            onChangedAppOrder();
        }

    }
}
