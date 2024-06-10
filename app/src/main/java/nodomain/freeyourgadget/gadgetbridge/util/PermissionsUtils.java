/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.R;

public class PermissionsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionsUtils.class);

    private static final String CUSTOM_PERM_NOTIFICATION_LISTENER = "custom_perm_notifications_listener";
    private static final String CUSTOM_PERM_NOTIFICATION_SERVICE = "custom_perm_notifications_service";
    private static final String CUSTOM_PERM_DISPLAY_OVER = "custom_perm_display_over";

    public static ArrayList<PermissionDetails> getRequiredPermissionsList(Activity activity) {
        ArrayList<PermissionDetails> permissionsList = new ArrayList<>();
        permissionsList.add(new PermissionDetails(
                CUSTOM_PERM_NOTIFICATION_LISTENER,
                activity.getString(R.string.menuitem_notifications),
                "Forwarding notifications to connected gadgets"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionsList.add(new PermissionDetails(
                    CUSTOM_PERM_NOTIFICATION_SERVICE,
                    "Manage Do Not Disturb",
                    "Change DND notification policy"));
            permissionsList.add(new PermissionDetails(
                    CUSTOM_PERM_DISPLAY_OVER,
                    "Display over other apps",
                    "Used by Bangle.js to start apps and other functionality on your phone"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    "Background location",
                    "Required for scanning for Bluetooth devices"));
        }
        permissionsList.add(new PermissionDetails(
                Manifest.permission.ACCESS_FINE_LOCATION,
                "Fine location",
                "Send location to gadgets which don't have GPS"));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH,
                    "Bluetooth",
                    "Connect to Bluetooth devices"));
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    "Bluetooth admin",
                    "Discover and pair Bluetooth devices"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH_SCAN,
                    "Bluetooth scan",
                    "Scan for Bluetooth devices"));
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    "Bluetooth connect",
                    "Connect to Bluetooth devices"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "Post notifications",
                    "Post ongoing notification which keeps the service running"));
        }
        if (BuildConfig.INTERNET_ACCESS) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.INTERNET,
                    "Internet access",
                    "Synchronization with online resources"));
        }
//        permissionsList.add(new PermissionDetails(  // NOTE: can't request this, it's only allowed for system apps
//                Manifest.permission.MEDIA_CONTENT_CONTROL,
//                "Media content control",
//                "Read and control media playback"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_CONTACTS,
                "Contacts",
                "Send contacts to gadgets"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_CALENDAR,
                "Calendar",
                "Send calendar to gadgets"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.RECEIVE_SMS,
                "Receive SMS",
                "Forward SMS messages to gadgets"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.SEND_SMS,
                "Send SMS",
                "Send SMS (canned response) from gadgets"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_CALL_LOG,
                "Read call log",
                "Forward call log to gadgets"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_PHONE_STATE,
                "Read phone state",
                "Read status of ongoing calls"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.CALL_PHONE,
                "Call phone",
                "Initiate phone calls from gadgets"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                "Process outgoing calls",
                "Read the number of an outgoing call to display it on a gadget"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.ANSWER_PHONE_CALLS,
                    "Answer phone calls",
                    "Answer phone calls from gadgets"));
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    "External storage",
                    "Using images, ringtones, app files and more"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.QUERY_ALL_PACKAGES,
                    "Query all packages",
                    "Read names and icons of all installed apps"));
        }
        return permissionsList;
    }

    public static boolean checkPermission(Context context, String permission) {
        if (permission.equals(CUSTOM_PERM_NOTIFICATION_LISTENER)) {
            Set<String> set = NotificationManagerCompat.getEnabledListenerPackages(context);
            return set.contains(context.getPackageName());
        } else if (permission.equals(CUSTOM_PERM_NOTIFICATION_SERVICE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted();
        } else if (permission.equals(CUSTOM_PERM_DISPLAY_OVER) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_DENIED;
        }
    }

    public static boolean checkAllPermissions(Activity activity) {
        boolean result = true;
        for (PermissionDetails permission : getRequiredPermissionsList(activity)) {
            if (!checkPermission(activity, permission.getPermission())) {
                result = false;
            }
        }
        return result;
    }

    public static void requestAllPermissions(Activity activity) {
        List<PermissionDetails> wantedPermissions = getRequiredPermissionsList(activity);

        if (!wantedPermissions.isEmpty()) {
            ArrayList<String> wantedPermissionsStrings = new ArrayList<>();
            for (PermissionDetails wantedPermission : wantedPermissions) {
                wantedPermissionsStrings.add(wantedPermission.getPermission());
            }
            if (!wantedPermissionsStrings.isEmpty()) {
                if (wantedPermissionsStrings.contains(CUSTOM_PERM_NOTIFICATION_LISTENER) && !checkPermission(activity, CUSTOM_PERM_NOTIFICATION_LISTENER))
                    requestPermission(activity, CUSTOM_PERM_NOTIFICATION_LISTENER);
                if (wantedPermissionsStrings.contains(CUSTOM_PERM_NOTIFICATION_SERVICE) && !checkPermission(activity, CUSTOM_PERM_NOTIFICATION_SERVICE))
                    requestPermission(activity, CUSTOM_PERM_NOTIFICATION_SERVICE);
                if (wantedPermissionsStrings.contains(CUSTOM_PERM_DISPLAY_OVER) && !checkPermission(activity, CUSTOM_PERM_DISPLAY_OVER))
                    requestPermission(activity, CUSTOM_PERM_DISPLAY_OVER);
                ActivityCompat.requestPermissions(activity, wantedPermissionsStrings.toArray(new String[0]), 0);
            }
        }
    }

    public static void requestPermission(Activity activity, String permission) {
        if (permission.equals(CUSTOM_PERM_NOTIFICATION_LISTENER)) {
            showNotifyListenerPermissionsDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_NOTIFICATION_SERVICE) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            showNotifyPolicyPermissionsDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_DISPLAY_OVER)) {
            showDisplayOverOthersPermissionsDialog(activity);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, 0);
        }
    }

    public static class PermissionDetails {
        private String permission;
        private String title;
        private String summary;

        public PermissionDetails(String permission, String title, String summary) {
            this.permission = permission;
            this.title = title;
            this.summary = summary;
        }

        public String getPermission() {
            return permission;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }
    }

    private static void showNotifyListenerPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_notification_listener,
                        activity.getString(R.string.app_name),
                        activity.getString(R.string.ok)))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            activity.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        } catch (ActivityNotFoundException e) {
                            GB.toast(activity, "'Notification Listener Settings' activity not found", Toast.LENGTH_LONG, GB.ERROR);
                            LOG.error("'Notification Listener Settings' activity not found");
                        }
                    }
                })
                .show();
    }

    private static void showNotifyPolicyPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_notification_policy_access,
                        activity.getString(R.string.app_name),
                        activity.getString(R.string.ok)))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            activity.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                        } catch (ActivityNotFoundException e) {
                            GB.toast(activity, "'Notification Policy' activity not found", Toast.LENGTH_LONG, GB.ERROR);
                            LOG.error("'Notification Policy' activity not found");
                        }
                    }
                })
                .show();
    }

    private static void showDisplayOverOthersPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_display_over_other_apps,
                        activity.getString(R.string.app_name),
                        activity.getString(R.string.ok)))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int id) {
                        Intent enableIntent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        activity.startActivity(enableIntent);
                    }
                })
                .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();
    }
}
