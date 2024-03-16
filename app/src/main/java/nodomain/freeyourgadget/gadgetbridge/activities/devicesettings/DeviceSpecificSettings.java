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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class that contains the device-specific settings screens for a device. All the integers in this
 * class correspond to xml resources for preferences.
 * <p>
 * This class contains 2 types of screens:
 * - Root screens - the ones that are displayed in the first page of the device settings activity. These can be
 * normal preference screens / preferences, or dummy root screens (see {@link DeviceSpecificSettingsScreen}.
 * - Sub-screens - a screen that is opened when one of the {@link DeviceSpecificSettingsScreen} is clicked.
 * <p>
 * There can be an arbitrary number of nested sub-screens, as long as they are all mapped by key in the
 * subScreens map.
 * <p>
 * See the XiaomiCoordinator and ZeppOsCoordinator for example usages.
 */
public class DeviceSpecificSettings implements Parcelable {
    private final List<Integer> rootScreens = new ArrayList<>();
    private final Map<String, List<Integer>> subScreens = new LinkedHashMap<>();

    public DeviceSpecificSettings() {
    }

    public DeviceSpecificSettings(final int[] rootScreens) {
        for (final int setting : rootScreens) {
            this.rootScreens.add(setting);
        }
    }

    public void addRootScreen(@XmlRes final int screen) {
        if (!rootScreens.contains(screen)) {
            rootScreens.add(screen);
        }
    }

    public List<Integer> addRootScreen(final DeviceSpecificSettingsScreen screen) {
        if (!rootScreens.contains(screen.getXml())) {
            rootScreens.add(screen.getXml());
        }

        return addSubScreen(screen, screen.getXml());
    }

    public List<Integer> addRootScreen(final DeviceSpecificSettingsScreen screen, final int... subScreens) {
        final List<Integer> subScreenScreens = addRootScreen(screen);
        for (final int subScreen : subScreens) {
            subScreenScreens.add(subScreen);
        }
        return subScreenScreens;
    }

    public void addRootScreen(final int index, @XmlRes final int screen) {
        rootScreens.add(index, screen);
    }

    public List<Integer> addSubScreen(final DeviceSpecificSettingsScreen rootScreen, final int... screens) {
        return addSubScreen(rootScreen.getKey(), screens);
    }

    private List<Integer> addSubScreen(final String key, final int... screens) {
        if (!subScreens.containsKey(key)) {
            subScreens.put(key, new ArrayList<>());
        }

        final List<Integer> subscreenPages = Objects.requireNonNull(subScreens.get(key));

        for (final int screen : screens) {
            if (!subscreenPages.contains(screen)) {
                subscreenPages.add(screen);
            }
        }

        return subscreenPages;
    }

    public void mergeFrom(final DeviceSpecificSettings deviceSpecificSettings) {
        for (final Integer rootScreen : deviceSpecificSettings.rootScreens) {
            addRootScreen(rootScreen);
        }
        for (final Map.Entry<String, List<Integer>> e : deviceSpecificSettings.subScreens.entrySet()) {
            if (!subScreens.containsKey(e.getKey())) {
                subScreens.put(e.getKey(), new ArrayList<>());
            }

            for (final int screen : e.getValue()) {
                Objects.requireNonNull(subScreens.get(e.getKey())).add(screen);
            }
        }
    }

    public List<Integer> getRootScreens() {
        return rootScreens;
    }

    @Nullable
    public List<Integer> getScreen(@NonNull final String key) {
        return subScreens.get(key);
    }

    public List<Integer> getAllScreens() {
        final List<Integer> allScreens = new ArrayList<>(rootScreens);
        for (final List<Integer> screens : subScreens.values()) {
            allScreens.addAll(screens);
        }
        return allScreens;
    }

    public static final Creator<DeviceSpecificSettings> CREATOR = new Creator<DeviceSpecificSettings>() {
        @Override
        public DeviceSpecificSettings createFromParcel(final Parcel in) {
            final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
            final int numRootScreens = in.readInt();
            for (int i = 0; i < numRootScreens; i++) {
                deviceSpecificSettings.addRootScreen(in.readInt());
            }
            final int numSubScreens = in.readInt();
            for (int i = 0; i < numSubScreens; i++) {
                final String key = in.readString();
                final int numScreens = in.readInt();
                final int[] screens = new int[numScreens];
                for (int j = 0; j < numScreens; j++) {
                    screens[j] = in.readInt();
                }
                deviceSpecificSettings.addSubScreen(key, screens);
            }
            return deviceSpecificSettings;
        }

        @Override
        public DeviceSpecificSettings[] newArray(final int size) {
            return new DeviceSpecificSettings[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(rootScreens.size());
        for (final Integer rootScreen : rootScreens) {
            dest.writeInt(rootScreen);
        }
        dest.writeInt(subScreens.size());
        for (final Map.Entry<String, List<Integer>> e : subScreens.entrySet()) {
            dest.writeString(e.getKey());
            dest.writeInt(e.getValue().size());
            for (final Integer s : e.getValue()) {
                dest.writeInt(s);
            }
        }
    }
}
