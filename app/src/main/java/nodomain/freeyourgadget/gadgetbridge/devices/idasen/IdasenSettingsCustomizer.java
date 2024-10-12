package nodomain.freeyourgadget.gadgetbridge.devices.idasen;

import android.os.Parcel;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.R;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

public class IdasenSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<IdasenSettingsCustomizer> CREATOR = new Creator<IdasenSettingsCustomizer>() {
        @Override
        public IdasenSettingsCustomizer createFromParcel(final Parcel in) {
            return new IdasenSettingsCustomizer();
        }

        @Override
        public IdasenSettingsCustomizer[] newArray(final int size) {
            return new IdasenSettingsCustomizer[size];
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(IdasenSettingsCustomizer.class);

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final EditTextPreference prefMidHeight = handler.findPreference(DeviceSettingsPreferenceConst.PREF_IDASEN_MID_HEIGHT);
        final EditTextPreference prefSitHeight = handler.findPreference(DeviceSettingsPreferenceConst.PREF_IDASEN_SIT_HEIGHT);
        final EditTextPreference prefStandHeight = handler.findPreference(DeviceSettingsPreferenceConst.PREF_IDASEN_STAND_HEIGHT);
        if (prefSitHeight == null || prefMidHeight == null || prefStandHeight == null) {
            return;
        }
        Preference.OnPreferenceChangeListener prefListener = (preference, newValue) -> {
            final double val = Double.parseDouble(newValue.toString());
            if (val > IdasenConstants.MAX_HEIGHT * 100F || val < IdasenConstants.MIN_HEIGHT * 100F) {
                toast(handler.getContext(), R.string.idasen_pref_value_warning, Toast.LENGTH_SHORT, 0);
                return false;
            }
            return true;
        };
        for (EditTextPreference pref: List.of(prefMidHeight, prefSitHeight, prefStandHeight)){
            pref.setOnBindEditTextListener(p -> {
                p.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                p.setSelection(p.getText().length());
            });
            pref.setOnPreferenceChangeListener(prefListener);
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
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}
