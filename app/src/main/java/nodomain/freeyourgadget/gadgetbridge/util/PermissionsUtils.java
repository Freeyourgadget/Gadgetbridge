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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;

public class PermissionsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionsUtils.class);

    public static final String CUSTOM_PERM_NOTIFICATION_LISTENER = "custom_perm_notifications_listener";
    public static final String CUSTOM_PERM_NOTIFICATION_SERVICE = "custom_perm_notifications_service";
    public static final String CUSTOM_PERM_DISPLAY_OVER = "custom_perm_display_over";

    public static final List<String> specialPermissions = new ArrayList<String>() {{
        add(CUSTOM_PERM_NOTIFICATION_LISTENER);
        add(CUSTOM_PERM_NOTIFICATION_SERVICE);
        add(CUSTOM_PERM_DISPLAY_OVER);
        add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }};

    public static ArrayList<PermissionDetails> getRequiredPermissionsList(Activity activity) {
        ArrayList<PermissionDetails> permissionsList = new ArrayList<>();
        permissionsList.add(new PermissionDetails(
                CUSTOM_PERM_NOTIFICATION_LISTENER,
                activity.getString(R.string.menuitem_notifications),
                activity.getString(R.string.permission_notifications_summary)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionsList.add(new PermissionDetails(
                    CUSTOM_PERM_NOTIFICATION_SERVICE,
                    activity.getString(R.string.permission_manage_dnd_title),
                    activity.getString(R.string.permission_manage_dnd_summary)));
            permissionsList.add(new PermissionDetails(
                    CUSTOM_PERM_DISPLAY_OVER,
                    activity.getString(R.string.permission_displayover_title),
                    activity.getString(R.string.permission_displayover_summary)));
        }
        permissionsList.add(new PermissionDetails(
                Manifest.permission.ACCESS_FINE_LOCATION,
                activity.getString(R.string.permission_fine_location_title),
                activity.getString(R.string.permission_fine_location_summary)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    activity.getString(R.string.permission_background_location_title),
                    activity.getString(R.string.permission_background_location_summary)));
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH,
                    activity.getString(R.string.permission_bluetooth_title),
                    activity.getString(R.string.permission_bluetooth_summary)));
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    activity.getString(R.string.permission_bluetooth_admin_title),
                    activity.getString(R.string.permission_bluetooth_admin_summary)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH_SCAN,
                    activity.getString(R.string.permission_bluetooth_scan_title),
                    activity.getString(R.string.permission_bluetooth_scan_summary)));
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    activity.getString(R.string.permission_bluetooth_connect_title),
                    activity.getString(R.string.permission_bluetooth_connect_summary)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.POST_NOTIFICATIONS,
                    activity.getString(R.string.permission_post_notification_title),
                    activity.getString(R.string.permission_post_notification_summary)));
        }
        if (BuildConfig.INTERNET_ACCESS) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.INTERNET,
                    activity.getString(R.string.permission_internet_access_title),
                    activity.getString(R.string.permission_internet_access_summary)));
        }
//        permissionsList.add(new PermissionDetails(  // NOTE: can't request this, it's only allowed for system apps
//                Manifest.permission.MEDIA_CONTENT_CONTROL,
//                "Media content control",
//                "Read and control media playback"));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_CONTACTS,
                activity.getString(R.string.permission_contacts_title),
                activity.getString(R.string.permission_contacts_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_CALENDAR,
                activity.getString(R.string.permission_calendar_title),
                activity.getString(R.string.permission_calendar_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.RECEIVE_SMS,
                activity.getString(R.string.permission_receive_sms_title),
                activity.getString(R.string.permission_receive_sms_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.SEND_SMS,
                activity.getString(R.string.permission_send_sms_title),
                activity.getString(R.string.permission_send_sms_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_CALL_LOG,
                activity.getString(R.string.permission_read_call_log_title),
                activity.getString(R.string.permission_read_call_log_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.READ_PHONE_STATE,
                activity.getString(R.string.permission_read_phone_state_title),
                activity.getString(R.string.permission_read_phone_state_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.CALL_PHONE,
                activity.getString(R.string.permission_call_phone_title),
                activity.getString(R.string.permission_call_phone_summary)));
        permissionsList.add(new PermissionDetails(
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                activity.getString(R.string.permission_process_outgoing_calls_title),
                activity.getString(R.string.permission_process_outgoing_calls_summary)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.ANSWER_PHONE_CALLS,
                    activity.getString(R.string.permission_answer_phone_calls_title),
                    activity.getString(R.string.permission_answer_phone_calls_summary)));
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    activity.getString(R.string.permission_external_storage_title),
                    activity.getString(R.string.permission_external_storage_summary)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionsList.add(new PermissionDetails(
                    Manifest.permission.QUERY_ALL_PACKAGES,
                    activity.getString(R.string.permission_query_all_packages_title),
                    activity.getString(R.string.permission_query_all_packages_summary)));
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

    public static void requestPermission(Activity activity, String permission) {
        if (permission.equals(CUSTOM_PERM_NOTIFICATION_LISTENER)) {
            showNotifyListenerPermissionsDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_NOTIFICATION_SERVICE) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            showNotifyPolicyPermissionsDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_DISPLAY_OVER)) {
            showDisplayOverOthersPermissionsDialog(activity);
        } else if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)) {
            showBackgroundLocationPermissionsDialog(activity);
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
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            Intent intent;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                                intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, new ComponentName(BuildConfig.APPLICATION_ID, NotificationListener.class.getName()).flattenToString());
                            } else {
                                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            }
                            String showArgs = BuildConfig.APPLICATION_ID + "/" + NotificationListener.class.getName();
                            intent.putExtra(":settings:fragment_args_key", showArgs);
                            Bundle bundle = new Bundle();
                            bundle.putString(":settings:fragment_args_key", showArgs);
                            intent.putExtra(":settings:show_fragment_args", bundle);
                            activity.startActivity(intent);
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
                        Intent enableIntent = new Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                        );
                        activity.startActivity(enableIntent);
                    }
                })
                .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static void showBackgroundLocationPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_location,
                        activity.getString(R.string.app_name),
                        activity.getPackageManager().getBackgroundPermissionOptionLabel()))
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
                })
                .show();
    }
}
