/*  Copyright (C) 2016-2024 Andreas Shimokawa, Andrzej Surowiec, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Ganblejs,
    gfwilliams, Gordon Williams, Johannes Tysiak, José Rebelo, marco.altomonte,
    Petr Vaněk, Taavi Eomäe

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;

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
import android.view.MotionEvent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;
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
    private boolean isLanguageInvalid = false;
    private boolean isThemeInvalid = false;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    private SwipeRefreshLayout swipeLayout;
    private static PhoneStateListener fakeStateListener;
    private AlertDialog clDialog;

    //needed for KK compatibility
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

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
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    handleRealtimeSample(device, intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                case ACTION_REQUEST_PERMISSIONS:
                    checkAndRequestPermissions();
                    break;
                case ACTION_REQUEST_LOCATION_PERMISSIONS:
                    checkAndRequestLocationPermissions();
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null && !dev.isBusy()) {
                        swipeLayout.setRefreshing(false);
                    }
                    break;
            }
        }
    };
    private boolean pesterWithPermissions = true;
    private final Map<GBDevice, ActivitySample> currentHRSample = new HashMap<>();

    public ActivitySample getCurrentHRSample(final GBDevice device) {
        return currentHRSample.get(device);
    }

    private void setCurrentHRSample(final GBDevice device, ActivitySample sample) {
        if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
            currentHRSample.put(device, sample);
        }
    }

    private void handleRealtimeSample(final GBDevice device, Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            setCurrentHRSample(device, sample);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Prefs prefs = GBApplication.getPrefs();

        // Determine availability of device with activity tracking functionality
        boolean activityTrackerAvailable = false;
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice dev : devices) {
            if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                activityTrackerAvailable = true;
                break;
            }
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("cl")) {
                GBChangeLog cl = GBChangeLog.createChangeLog(this);
                try {
                    if (cl.hasChanges(false)) {
                        clDialog = cl.getMaterialLogDialog();
                    } else {
                        clDialog = cl.getMaterialFullLogDialog();
                    }
                    clDialog.show();
                } catch (Exception ignored) {
                    GB.toast(getBaseContext(), getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR);
                }
            }
        }


        // Initialize drawer
        NavigationView drawerNavigationView = findViewById(R.id.nav_view);
        drawerNavigationView.setNavigationItemSelectedListener(this);

        // Initialize bottom navigation
        BottomNavigationView navigationView = findViewById(R.id.bottom_nav_bar);
        if (activityTrackerAvailable && prefs.getBoolean("display_bottom_navigation_bar", true)) {
            navigationView.setVisibility(View.VISIBLE);
        } else {
            navigationView.setVisibility(View.GONE);
        }
        navigationView.setOnItemSelectedListener(menuItem -> {
            final int itemId = menuItem.getItemId();
            if (itemId == R.id.bottom_nav_dashboard) {
                viewPager.setCurrentItem(0, true);
            } else if (itemId == R.id.bottom_nav_devices) {
                viewPager.setCurrentItem(1, true);
            }
            return true;
        });

        // Initialize actionbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.controlcenter_navigation_drawer_open, R.string.controlcenter_navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        if (GBApplication.areDynamicColorsEnabled()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
            @ColorInt int toolbarBackground = typedValue.data;
            toolbar.setBackgroundColor(toolbarBackground);
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.primarydark_light));
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        }

        // Configure ViewPager2 with fragment adapter and default fragment
        viewPager = findViewById(R.id.dashboard_viewpager);
        pagerAdapter = new MainFragmentsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        if (!prefs.getBoolean("dashboard_as_default_view", true) || !activityTrackerAvailable) {
            viewPager.setCurrentItem(1, false);
            navigationView.getMenu().getItem(1).setChecked(true);
        }

        // Sync ViewPager changes with BottomNavigationView
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private MenuProvider existingMenuProvider = null;

            @Override
            public void onPageSelected(int position) {
                navigationView.getMenu().getItem(position).setChecked(true);

                // Ensure the menu provider is set to the current fragment
                if (existingMenuProvider != null) {
                    ControlCenterv2.this.removeMenuProvider(existingMenuProvider);
                }
                final Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                if (fragment instanceof MenuProvider) {
                    existingMenuProvider = (MenuProvider) fragment;
                    ControlCenterv2.this.addMenuProvider(existingMenuProvider);
                }
            }
        });

        // Make sure the SwipeRefreshLayout doesn't interfere with the ViewPager2
        viewPager.getChildAt(0).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                swipeLayout.setEnabled(false);
            } else {
                swipeLayout.setEnabled(true);
            }
            return false;
        });

        // Set pull-down-to-refresh action
        swipeLayout = findViewById(R.id.dashboard_swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            // Fetch activity for all connected devices
            GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);
            // Hide 'refreshing' animation immediately if no health devices are connected
            List<GBDevice> devices1 = GBApplication.app().getDeviceManager().getDevices();
            for (GBDevice dev : devices1) {
                if (dev.getDeviceCoordinator().supportsActivityTracking() && dev.isInitialized()) {
                    return;
                }
            }
            swipeLayout.setRefreshing(false);
            GB.toast(getString(R.string.info_no_devices_connected), Toast.LENGTH_LONG, GB.WARN);
        });

        // Set up local intent listener
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_LANGUAGE_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_THEME_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        filterLocal.addAction(ACTION_REQUEST_PERMISSIONS);
        filterLocal.addAction(ACTION_REQUEST_LOCATION_PERMISSIONS);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        /*
         * Ask for permission to intercept notifications on first run.
         */
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

        GBChangeLog cl = GBChangeLog.createChangeLog(this);
        boolean showChangelog = prefs.getBoolean("show_changelog", true);
        if (showChangelog && cl.isFirstRun() && cl.hasChanges(cl.isFirstRunEver())) {
            try {
                cl.getMaterialLogDialog().show();
            } catch (Exception ignored) {
                GB.toast(this, getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR);
            }
        }

        GBApplication.deviceService().requestDeviceInfo();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
       if (clDialog != null){
           outState.putBoolean("cl", clDialog.isShowing());
       }
       super.onSaveInstanceState(outState);
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        final int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            final Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, MENU_REFRESH_CODE);
            return false;
        } else if (itemId == R.id.action_debug) {
            final Intent debugIntent = new Intent(this, DebugActivity.class);
            startActivity(debugIntent);
            return false;
        } else if (itemId == R.id.action_data_management) {
            final Intent dbIntent = new Intent(this, DataManagementActivity.class);
            startActivity(dbIntent);
            return false;
        } else if (itemId == R.id.device_action_discover) {
            launchDiscoveryActivity();
            return false;
        } else if (itemId == R.id.action_quit) {
            GBApplication.quit();
            return false;
        } else if (itemId == R.id.donation_link) {
            final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://liberapay.com/Gadgetbridge")); //TODO: centralize if ever used somewhere else
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return false;
        } else if (itemId == R.id.external_changelog) {
            final GBChangeLog cl = GBChangeLog.createChangeLog(this);
            try {
                if (cl.hasChanges(false)) {
                    clDialog = cl.getMaterialLogDialog();
                } else {
                    clDialog = cl.getMaterialFullLogDialog();
                }
                clDialog.show();
            } catch (Exception ignored) {
                GB.toast(getBaseContext(), getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR);
            }
            return false;
        } else if (itemId == R.id.about) {
            final Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return false;
        }

        return false;  // we do not want the drawer menu item to get selected
    }


    private void launchDiscoveryActivity() {
        startActivity(new Intent(this, DiscoveryActivityV2.class));
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

    private void checkAndRequestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access background location!");
            GB.toast(getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
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

    private class MainFragmentsPagerAdapter extends FragmentStateAdapter {
        public MainFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new DashboardFragment();
            } else {
                return new DevicesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
