/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.oppo;

import android.os.Parcel;
import android.util.Pair;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigSide;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigValue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class OppoHeadphonesSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private final Map<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> touchOptions;

    public static final Creator<OppoHeadphonesSettingsCustomizer> CREATOR = new Creator<OppoHeadphonesSettingsCustomizer>() {
        @Override
        public OppoHeadphonesSettingsCustomizer createFromParcel(final Parcel in) {
            final Map<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> touchOptions = new LinkedHashMap<>();
            final int numOptions = in.readInt();
            for (int i = 0; i < numOptions; i++) {
                final TouchConfigSide touchConfigSide = TouchConfigSide.valueOf(in.readString());
                final TouchConfigType touchConfigType = TouchConfigType.valueOf(in.readString());
                final List<TouchConfigValue> values = new ArrayList<>();
                in.readList(values, TouchConfigValue.class.getClassLoader());
                touchOptions.put(Pair.create(touchConfigSide, touchConfigType), values);
            }
            return new OppoHeadphonesSettingsCustomizer(touchOptions);
        }

        @Override
        public OppoHeadphonesSettingsCustomizer[] newArray(final int size) {
            return new OppoHeadphonesSettingsCustomizer[size];
        }
    };

    public OppoHeadphonesSettingsCustomizer(final Map<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> touchOptions) {
        this.touchOptions = touchOptions;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Set<TouchConfigSide> knownSides = new HashSet<>();
        final Set<TouchConfigType> knownTypes = new HashSet<>();

        for (final Map.Entry<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> e : touchOptions.entrySet()) {
            final TouchConfigSide side = e.getKey().first;
            final TouchConfigType type = e.getKey().second;
            final Set<TouchConfigValue> possibleValues = new HashSet<>(e.getValue());

            knownSides.add(side);
            knownTypes.add(type);

            final String key = OppoHeadphonesPreferences.getKey(side, type);
            final ListPreference pref = handler.findPreference(key);
            if (pref == null) {
                continue;
            }

            final CharSequence[] originalEntries = pref.getEntries();
            final CharSequence[] originalValues = pref.getEntryValues();
            final CharSequence[] entries = new CharSequence[possibleValues.size()];
            final CharSequence[] values = new CharSequence[possibleValues.size()];
            int j = 0;
            for (int i = 0; i < originalValues.length; i++) {
                if (possibleValues.contains(TouchConfigValue.valueOf(originalValues[i].toString().toUpperCase(Locale.ROOT)))) {
                    entries[j] = originalEntries[i];
                    values[j] = originalValues[i];
                    j++;
                }
            }

            pref.setEntries(entries);
            pref.setEntryValues(values);

            handler.addPreferenceHandlerFor(key);
        }

        for (final TouchConfigSide side : TouchConfigSide.values()) {
            if (!knownSides.contains(side)) {
                // Side not configurable, hide it completely
                final Preference header = handler.findPreference("oppo_touch_header_" + side.name().toLowerCase(Locale.ROOT));
                if (header != null) {
                    header.setVisible(false);
                    continue;
                }
            }

            for (final TouchConfigType type : TouchConfigType.values()) {
                if (!knownTypes.contains(type)) {
                    final String key = OppoHeadphonesPreferences.getKey(side, type);
                    final Preference pref = handler.findPreference(key);
                    if (pref != null) {
                        pref.setVisible(false);
                    }
                }
            }
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
        dest.writeInt(touchOptions.size());
        for (final Map.Entry<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> e : touchOptions.entrySet()) {
            dest.writeString(e.getKey().first.name());
            dest.writeString(e.getKey().second.name());
            dest.writeList(e.getValue());
        }
    }
}
