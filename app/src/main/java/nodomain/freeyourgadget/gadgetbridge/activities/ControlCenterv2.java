/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Johannes Tysiak, Taavi Eomäe, vanous

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapterv2;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

//TODO: extend AbstractGBActivity, but it requires actionbar that is not available
public class ControlCenterv2 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ControlCenterv2.class);
    public static final int MENU_REFRESH_CODE = 1;
    public static final String ACTION_REQUEST_PERMISSIONS
            = "nodomain.freeyourgadget.gadgetbridge.activities.controlcenter.requestpermissions";
    public static final String ACTION_REQUEST_LOCATION_PERMISSIONS
            = "nodomain.freeyourgadget.gadgetbridge.activities.controlcenter.requestlocationpermissions";
    private static PhoneStateListener fakeStateListener;

    //needed for KK compatibility
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private DeviceManager deviceManager;
    private GBDeviceAdapterv2 mGBDeviceAdapter;
    private RecyclerView deviceListView;
    private FloatingActionButton fab;
    private boolean isLanguageInvalid = false;
    private boolean isThemeInvalid = false;
    List<GBDevice> deviceList;
    private  HashMap<String,long[]> deviceActivityHashMap = new HashMap();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBApplication.ACTION_LANGUAGE_CHANGE:
                    setLanguage(GBApplication.getLanguage(), true);
                    break;
                case GBApplication.ACTION_THEME_CHANGE:
                    isThemeInvalid = true;
                    break;
                case GBApplication.ACTION_QUIT:
                    finish();
                    break;
                case DeviceManager.ACTION_DEVICES_CHANGED:
                case GBApplication.ACTION_NEW_DATA:
                    createRefreshTask("get activity data", getApplication()).execute();
                    mGBDeviceAdapter.rebuildFolders();
                    refreshPairedDevices();
                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    handleRealtimeSample(intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                case ACTION_REQUEST_PERMISSIONS:
                    checkAndRequestPermissions();
                    break;
                case ACTION_REQUEST_LOCATION_PERMISSIONS:
                    checkAndRequestLocationPermissions();
                    break;

            }
        }
    };
    private boolean pesterWithPermissions = true;
    private ActivitySample currentHRSample;

    public ActivitySample getCurrentHRSample() {
        return currentHRSample;
    }

    private void setCurrentHRSample(ActivitySample sample) {
        if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
            currentHRSample = sample;
            refreshPairedDevices();
        }
    }

    private void handleRealtimeSample(Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            setCurrentHRSample(sample);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlcenterv2);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (GBApplication.areDynamicColorsEnabled()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            theme.resolveAttribute(R.attr.colorSurface, typedValue, true);
            @ColorInt int toolbarBackground = typedValue.data;
            toolbar.setBackgroundColor(toolbarBackground);
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.primarydark_light));
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.controlcenter_navigation_drawer_open, R.string.controlcenter_navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        /* This sucks but for the play store we're not allowed a donation link. Instead for
        the Bangle.js Play Store app we put a message in the About dialog via @string/about_description */
        if (BuildConfig.FLAVOR == "banglejs") {
            MenuItemImpl v = (MenuItemImpl) ((NavigationView) drawer.getChildAt(1)).getMenu().findItem(R.id.donation_link);
            if (v != null) v.setVisible(false);
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //end of material design boilerplate
        deviceManager = ((GBApplication) getApplication()).getDeviceManager();

        deviceListView = findViewById(R.id.deviceListView);
        deviceListView.setHasFixedSize(true);
        deviceListView.setLayoutManager(new LinearLayoutManager(this));

        deviceList = deviceManager.getDevices();
        mGBDeviceAdapter = new GBDeviceAdapterv2(this, deviceList, deviceActivityHashMap);
        mGBDeviceAdapter.setHasStableIds(true);

        // get activity data asynchronously, this fills the deviceActivityHashMap
        // and calls refreshPairedDevices() → notifyDataSetChanged
        createRefreshTask("get activity data", getApplication()).execute();

        deviceListView.setAdapter(this.mGBDeviceAdapter);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiscoveryActivity();
            }
        });

        showFabIfNeccessary();

        /* uncomment to enable fixed-swipe to reveal more actions

        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT , ItemTouchHelper.RIGHT) {
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if(dX>50)
                    dX = 50;
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                GB.toast(getBaseContext(), "onMove", Toast.LENGTH_LONG, GB.ERROR);

                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                GB.toast(getBaseContext(), "onSwiped", Toast.LENGTH_LONG, GB.ERROR);

            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                        RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                        int actionState, boolean isCurrentlyActive) {
            }
        });

        swipeToDismissTouchHelper.attachToRecyclerView(deviceListView);
        */

        registerForContextMenu(deviceListView);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_LANGUAGE_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_THEME_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        filterLocal.addAction(ACTION_REQUEST_PERMISSIONS);
        filterLocal.addAction(ACTION_REQUEST_LOCATION_PERMISSIONS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        refreshPairedDevices();

        /*
         * Ask for permission to intercept notifications on first run.
         */
        Prefs prefs = GBApplication.getPrefs();
        pesterWithPermissions = prefs.getBoolean("permission_pestering", true);

        boolean displayPermissionDialog = !prefs.getBoolean("permission_dialog_displayed", false);
        prefs.getPreferences().edit().putBoolean("permission_dialog_displayed", true).apply();


        Set<String> set = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (pesterWithPermissions) {
            if (!set.contains(this.getPackageName())) { // If notification listener access hasn't been granted
                // Put up a dialog explaining why we need permissions (Polite, but also Play Store policy)
                // When accepted, we open the Activity for Notification access
                DialogFragment dialog = new NotifyListenerPermissionsDialogFragment();
                dialog.show(getSupportFragmentManager(), "NotifyListenerPermissionsDialogFragment");
            }
        }

	/* We not put up dialogs explaining why we need permissions (Polite, but also Play Store policy).

           Rather than chaining the calls, we just open a bunch of dialogs. Last in this list = first
           on the page, and as they are accepted the permissions are requested in turn.

           When accepted, we request it or open the Activity for permission to display over other apps. */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           /* In order to be able to set ringer mode to silent in GB's PhoneCallReceiver
           the permission to access notifications is needed above Android M
           ACCESS_NOTIFICATION_POLICY is also needed in the manifest */
            if (pesterWithPermissions) {
                if (!((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()) {
                    // Put up a dialog explaining why we need permissions (Polite, but also Play Store policy)
                    // When accepted, we open the Activity for Notification access
                    DialogFragment dialog = new NotifyPolicyPermissionsDialogFragment();
                    dialog.show(getSupportFragmentManager(), "NotifyPolicyPermissionsDialogFragment");
                }
            }

            if (!Settings.canDrawOverlays(getApplicationContext())) {
                // If diplay over other apps access hasn't been granted
                // Put up a dialog explaining why we need permissions (Polite, but also Play Store policy)
                // When accepted, we open the Activity for permission to display over other apps.
                if (pesterWithPermissions) {
                    DialogFragment dialog = new DisplayOverOthersPermissionsDialogFragment();
                    dialog.show(getSupportFragmentManager(), "DisplayOverOthersPermissionsDialogFragment");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED) {
                if (pesterWithPermissions) {
                    DialogFragment dialog = new LocationPermissionsDialogFragment();
                    dialog.show(getSupportFragmentManager(), "LocationPermissionsDialogFragment");
                }
            }

            // Check all the other permissions that we need to for Android M + later
            if (getWantedPermissions().isEmpty())
                displayPermissionDialog = false;
            if (displayPermissionDialog && pesterWithPermissions) {
                DialogFragment dialog = new PermissionsDialogFragment();
                dialog.show(getSupportFragmentManager(), "PermissionsDialogFragment");
                // when 'ok' clicked, checkAndRequestPermissions() is called
            } else
                checkAndRequestPermissions();
        }

        GBChangeLog cl = createChangeLog();
        final boolean showChangelog = prefs.getBoolean("show_changelog", true);
        if (showChangelog && cl.isFirstRun() && cl.hasChanges(cl.isFirstRunEver())) {
            try {
                cl.getMaterialLogDialog().show();
            } catch (Exception ignored) {
                GB.toast(getBaseContext(), "Error showing Changelog", Toast.LENGTH_LONG, GB.ERROR);
            }
        }

        GBApplication.deviceService().start();

        if (GB.isBluetoothEnabled() && deviceList.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            launchDiscoveryActivity();
        } else {
            GBApplication.deviceService().requestDeviceInfo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleShortcut(getIntent());
        if (isLanguageInvalid || isThemeInvalid) {
            isLanguageInvalid = false;
            isThemeInvalid = false;
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterForContextMenu(deviceListView);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MENU_REFRESH_CODE) {
            showFabIfNeccessary();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsIntent, MENU_REFRESH_CODE);
                return false; //we do not want the drawer menu item to get selected
            case R.id.action_debug:
                Intent debugIntent = new Intent(this, DebugActivity.class);
                startActivity(debugIntent);
                return false;
            case R.id.action_data_management:
                Intent dbIntent = new Intent(this, DataManagementActivity.class);
                startActivity(dbIntent);
                return false;
            case R.id.action_notification_management:
                Intent blIntent = new Intent(this, NotificationManagementActivity.class);
                startActivity(blIntent);
                return false;
            case R.id.device_action_discover:
                launchDiscoveryActivity();
                return false;
            case R.id.action_quit:
                GBApplication.quit();
                return false;
            case R.id.donation_link:
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://liberapay.com/Gadgetbridge")); //TODO: centralize if ever used somewhere else
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return false;
            case R.id.external_changelog:
                GBChangeLog cl = createChangeLog();
                try {
                    if (cl.hasChanges(false)) {
                        cl.getMaterialLogDialog().show();
                    } else {
                        cl.getMaterialFullLogDialog().show();
                    }
                } catch (Exception ignored) {
                    GB.toast(getBaseContext(), "Error showing Changelog", Toast.LENGTH_LONG, GB.ERROR);
                }
                return false;
            case R.id.about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return false;
        }

        return false;
    }

    private GBChangeLog createChangeLog() {
        String css = GBChangeLog.DEFAULT_CSS;
        css += "body { "
                + "color: " + AndroidUtils.getTextColorHex(getBaseContext()) + "; "
                + "}";
        return new GBChangeLog(this, css);
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(this, DiscoveryActivityV2.class));
    }

    private void refreshPairedDevices() {
        mGBDeviceAdapter.notifyDataSetChanged();
    }

    private void showFabIfNeccessary() {
        if (GBApplication.getPrefs().getBoolean("display_add_device_fab", true)) {
            fab.show();
        } else {
            if (deviceManager.getDevices().size() < 1) {
                fab.show();
            } else {
                fab.hide();
            }
        }
    }

    private void checkAndRequestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access background location!");
            toast(ControlCenterv2.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private List<String> getWantedPermissions() {
        List<String> wantedPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.BLUETOOTH);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_CONTACTS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.CALL_PHONE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_CALL_LOG);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_PHONE_STATE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.RECEIVE_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.SEND_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_CALENDAR);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.MEDIA_CONTENT_CONTROL) == PackageManager.PERMISSION_DENIED)
                wantedPermissions.add(Manifest.permission.MEDIA_CONTENT_CONTROL);
        } catch (Exception ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (pesterWithPermissions) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_DENIED) {
                    wantedPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.QUERY_ALL_PACKAGES);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (BuildConfig.INTERNET_ACCESS) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
                wantedPermissions.add(Manifest.permission.INTERNET);
            }
        }

        return wantedPermissions;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermissions() {
        List<String> wantedPermissions = getWantedPermissions();

        if (!wantedPermissions.isEmpty()) {
            Prefs prefs = GBApplication.getPrefs();
            // If this is not the first run, we can rely on
            // shouldShowRequestPermissionRationale(String permission)
            // and ignore permissions that shouldn't or can't be requested again
            if (prefs.getBoolean("permissions_asked", false)) {
                // Don't request permissions that we shouldn't show a prompt for
                // e.g. permissions that are "Never" granted by the user or never granted by the system
                Set<String> shouldNotAsk = new HashSet<>();
                for (String wantedPermission : wantedPermissions) {
                    if (!shouldShowRequestPermissionRationale(wantedPermission)) {
                        shouldNotAsk.add(wantedPermission);
                    }
                }
                wantedPermissions.removeAll(shouldNotAsk);
            } else {
                // Permissions have not been asked yet, but now will be
                prefs.getPreferences().edit().putBoolean("permissions_asked", true).apply();
            }

            if (!wantedPermissions.isEmpty()) {
                GB.toast(this, getString(R.string.permission_granting_mandatory), Toast.LENGTH_LONG, GB.ERROR);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[0]), 0);
                } else {
                    requestMultiplePermissionsLauncher.launch(wantedPermissions.toArray(new String[0]));
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { // The enclosed hack in it's current state cause crash on Banglejs builds tarkgetSDK=31 on a Android 13 device.
            // HACK: On Lineage we have to do this so that the permission dialog pops up
            if (fakeStateListener == null) {
                fakeStateListener = new PhoneStateListener();
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                telephonyManager.listen(fakeStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                telephonyManager.listen(fakeStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }
    }

    public void setLanguage(Locale language, boolean invalidateLanguage) {
        if (invalidateLanguage) {
            isLanguageInvalid = true;
        }
        AndroidUtils.setLanguage(this, language);
    }

    private long[] getSteps(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();

        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db);
    }

    protected RefreshTask createRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
    }

    private void handleShortcut(Intent intent) {
        if(ACTION_CONNECT.equals(intent.getAction())) {
            String btDeviceAddress = intent.getStringExtra("device");
            if(btDeviceAddress!=null){
                GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                if (candidate != null && !candidate.isConnected()) {
                    GBApplication.deviceService(candidate).connect();
                }
            }
        }
    }
    public class RefreshTask extends DBAccess {
        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler db) {
            for (GBDevice gbDevice : deviceList) {
                final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
                if (coordinator.supportsActivityTracking()) {
                    long[] stepsAndSleepData = getSteps(gbDevice, db);
                    deviceActivityHashMap.put(gbDevice.getAddress(), stepsAndSleepData);
                }
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            refreshPairedDevices();
        }

    }

    /// Called from onCreate - this puts up a dialog explaining we need permissions, and goes to the correct Activity
    public static class NotifyPolicyPermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            final Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_notification_policy_access,
                    getContext().getString(R.string.app_name),
                    getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                            } catch (ActivityNotFoundException e) {
                                GB.toast(context, "'Notification Policy' activity not found", Toast.LENGTH_LONG, GB.ERROR);
                            }
                        }
                    });
            return builder.create();
        }
    }

    /// Called from onCreate - this puts up a dialog explaining we need permissions, and goes to the correct Activity
    public static class NotifyListenerPermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            final Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_notification_listener,
                                    getContext().getString(R.string.app_name),
                                    getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            } catch (ActivityNotFoundException e) {
                                GB.toast(context, "'Notification Listener Settings' activity not found", Toast.LENGTH_LONG, GB.ERROR);
                            }
                        }
                    });
            return builder.create();
        }
    }

    /// Called from onCreate - this puts up a dialog explaining we need permissions, and goes to the correct Activity
    public static class DisplayOverOthersPermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_display_over_other_apps,
                            getContext().getString(R.string.app_name),
                            getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        public void onClick(DialogInterface dialog, int id) {
                            Intent enableIntent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            startActivity(enableIntent);
                        }
                    }).setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {}
                    });
            return builder.create();
        }
    }


    /// Called from onCreate - this puts up a dialog explaining we need backgound location permissions, and then requests permissions when 'ok' pressed
    public static class LocationPermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_location,
                            getContext().getString(R.string.app_name),
                            getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(ACTION_REQUEST_LOCATION_PERMISSIONS);
                            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                        }
                    });
            return builder.create();
        }
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    // This is required here rather than where it is used because it'll cause a
    // "LifecycleOwners must call register before they are STARTED" if not called from onCreate
    public ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                if (isGranted.containsValue(true)) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    GB.toast(this, getString(R.string.permission_granting_mandatory), Toast.LENGTH_LONG, GB.ERROR);
                }
            });

    /// Called from onCreate - this puts up a dialog explaining we need permissions, and then requests permissions when 'ok' pressed
    public static class PermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_request,
                            getContext().getString(R.string.app_name),
                            getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(ACTION_REQUEST_PERMISSIONS);
                            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                        }
                    });
            return builder.create();
        }
    }
}
