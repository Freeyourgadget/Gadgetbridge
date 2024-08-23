package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class QHybridSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference pref = handler.findPreference("pref_key_qhybrid_legacy");
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(handler.getContext(), QHybridConfigActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, handler.getDevice());
                handler.getContext().startActivity(intent);
                return true;
            });
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<QHybridSettingsCustomizer> CREATOR = new Creator<QHybridSettingsCustomizer>() {
        @Override
        public QHybridSettingsCustomizer createFromParcel(final Parcel in) {
            return new QHybridSettingsCustomizer();
        }

        @Override
        public QHybridSettingsCustomizer[] newArray(final int size) {
            return new QHybridSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
    }
}
