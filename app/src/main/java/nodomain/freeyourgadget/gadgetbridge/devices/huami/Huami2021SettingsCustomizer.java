/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.os.Parcel;
import android.text.InputType;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.mobeta.android.dslv.DragSortListPreference;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021MenuType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class Huami2021SettingsCustomizer extends HuamiSettingsCustomizer {
    public Huami2021SettingsCustomizer(final GBDevice device) {
        super(device);
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs) {
        super.customizeSettings(handler, prefs);

        setupDisplayItemsPref(
                HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE,
                HuamiConst.PREF_ALL_DISPLAY_ITEMS,
                Huami2021MenuType.displayItemNameLookup,
                handler,
                prefs
        );

        setupDisplayItemsPref(
                HuamiConst.PREF_SHORTCUTS_SORTABLE,
                HuamiConst.PREF_ALL_SHORTCUTS,
                Huami2021MenuType.shortcutsNameLookup,
                handler,
                prefs
        );
    }

    private void setupDisplayItemsPref(final String prefKey,
                                       final String allItemsPrefKey,
                                       final Map<String, Integer> nameLookup,
                                       final DeviceSpecificSettingsHandler handler,
                                       final Prefs prefs) {
        final DragSortListPreference pref = handler.findPreference(prefKey);
        if (pref == null) {
            return;
        }
        final List<String> allDisplayItems = prefs.getList(allItemsPrefKey, null);
        if (allDisplayItems == null || allDisplayItems.isEmpty()) {
            return;
        }

        final CharSequence[] entries = new CharSequence[allDisplayItems.size()];
        final CharSequence[] values = new CharSequence[allDisplayItems.size()];
        for (int i = 0; i < allDisplayItems.size(); i++) {
            final String screenId = allDisplayItems.get(i);
            final String screenName;
            if (screenId.equals("more")) {
                screenName = handler.getContext().getString(R.string.menuitem_more);
            } else if (nameLookup.containsKey(screenId)) {
                screenName = handler.getContext().getString(nameLookup.get(screenId));
            } else {
                screenName = handler.getContext().getString(R.string.menuitem_unknown_app, screenId);
            }

            entries[i] = screenName;
            values[i] = screenId;
        }

        pref.setEntries(entries);
        pref.setEntryValues(values);
    }
}
