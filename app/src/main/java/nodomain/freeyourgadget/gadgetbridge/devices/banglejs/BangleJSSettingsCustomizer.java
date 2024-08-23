/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants.PREF_BANGLEJS_ACTIVITY_FULL_SYNC_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants.PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STATUS;
import static nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants.PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STOP;
import static nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants.PREF_BANGLEJS_ACTIVITY_FULL_SYNC_TRIGGER;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class BangleJSSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private ProgressDialog activityFullSyncDialog;

    final GBDevice device;

    public BangleJSSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        // Handle full sync status
        if (preference.getKey().equals(PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STATUS)) {
            final EditTextPreference fullSyncStatusPreference = (EditTextPreference) preference;
            final String statusValue = fullSyncStatusPreference.getText();

            if (activityFullSyncDialog != null) {
                switch (statusValue) {
                    case "start":
                        activityFullSyncDialog.setMessage(handler.getContext().getString(R.string.busy_task_fetch_activity_data));
                        break;
                    case "end":
                        activityFullSyncDialog.dismiss();
                        activityFullSyncDialog = null;
                        break;
                    default:
                        activityFullSyncDialog.setMessage(statusValue);
                }
            }
        }
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference fullSyncPref = handler.findPreference(PREF_BANGLEJS_ACTIVITY_FULL_SYNC_TRIGGER);
        if (fullSyncPref != null) {
            fullSyncPref.setOnPreferenceClickListener(preference -> {
                if (activityFullSyncDialog != null) {
                    // Already syncing
                    return true;
                }

                final Context context = preference.getContext();

                new AlertDialog.Builder(context)
                        .setTitle(R.string.pref_activity_full_sync_trigger_title)
                        .setMessage(R.string.pref_activity_full_sync_trigger_warning)
                        .setIcon(R.drawable.ic_refresh)
                        .setPositiveButton(R.string.start, (dialog, whichButton) -> {
                            handler.notifyPreferenceChanged(PREF_BANGLEJS_ACTIVITY_FULL_SYNC_START);

                            activityFullSyncDialog = new ProgressDialog(context);
                            activityFullSyncDialog.setCancelable(false);
                            activityFullSyncDialog.setMessage(context.getString(R.string.sony_anc_optimizer_status_starting));
                            activityFullSyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            activityFullSyncDialog.setProgress(0);
                            activityFullSyncDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.Cancel), (dialog1, which) -> {
                                dialog1.dismiss();
                                activityFullSyncDialog = null;
                                handler.notifyPreferenceChanged(PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STOP);
                            });

                            activityFullSyncDialog.show();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

                return true;
            });
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, 0);
    }

    public static final Creator<BangleJSSettingsCustomizer> CREATOR = new Creator<BangleJSSettingsCustomizer>() {
        @Override
        public BangleJSSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(BangleJSSettingsCustomizer.class.getClassLoader());
            return new BangleJSSettingsCustomizer(device);
        }

        @Override
        public BangleJSSettingsCustomizer[] newArray(final int size) {
            return new BangleJSSettingsCustomizer[size];
        }
    };
}
