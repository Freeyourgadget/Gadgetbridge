/*  Copyright (C) 2020-2021 Petr Vaněk

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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Widget;

public class WidgetPreferenceStorage {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetPreferenceStorage.class);
    boolean isWidgetInPrefs = false;
    String PREFS_WIDGET_SETTINGS = "widget_settings";

    public String getSavedDeviceAddress(Context context, int appWidgetId) {
        String savedDeviceAddress = null;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedWidgetsPreferencesData = sharedPrefs.getString(PREFS_WIDGET_SETTINGS, "");
        JSONArray savedWidgetsPreferencesDataArray = null;
        try {
            savedWidgetsPreferencesDataArray = new JSONArray(savedWidgetsPreferencesData);
        } catch (
                JSONException e) {
            LOG.error(e.getMessage());
        }

        LOG.debug("widget JSON loaded: " + savedWidgetsPreferencesDataArray);
        if (savedWidgetsPreferencesDataArray == null) {
            return null;
        }
        for (int i = 0; i < savedWidgetsPreferencesDataArray.length(); i++) {
            try {
                JSONArray a = savedWidgetsPreferencesDataArray.getJSONArray(i);
                if (appWidgetId == a.getInt(0)) {
                    isWidgetInPrefs = true;
                    savedDeviceAddress = a.getString(1);
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage());
            }
        }
        return savedDeviceAddress;
    }

    public void removeWidgetById(Context context, int appWidgetId) {
        LOG.debug("widget trying to remove: " + appWidgetId);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedWidgetsPreferencesData = sharedPrefs.getString(PREFS_WIDGET_SETTINGS, "");
        JSONArray savedWidgetsPreferencesDataArray = null;
        try {
            savedWidgetsPreferencesDataArray = new JSONArray(savedWidgetsPreferencesData);
        } catch (
                JSONException e) {
            LOG.error(e.getMessage());
        }

        if (savedWidgetsPreferencesDataArray == null) {
            return;
        }
        for (int i = 0; i < savedWidgetsPreferencesDataArray.length(); i++) {
            try {
                JSONArray a = savedWidgetsPreferencesDataArray.getJSONArray(i);
                if (appWidgetId == a.getInt(0)) {
                    GB.toast("Removing widget preferences: " + appWidgetId, Toast.LENGTH_SHORT, GB.INFO);
                    savedWidgetsPreferencesDataArray.remove(i);
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage());
            }
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREFS_WIDGET_SETTINGS, savedWidgetsPreferencesDataArray.toString());
        editor.commit();
        editor.apply();
    }

    public void saveWidgetPrefs(Context context, String AppWidgetId, String HwAddress) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedWidgetsPreferencesData = sharedPrefs.getString(PREFS_WIDGET_SETTINGS, "");
        JSONArray savedWidgetsPreferencesDataArray = null;
        try {
            savedWidgetsPreferencesDataArray = new JSONArray(savedWidgetsPreferencesData);
        } catch (
                JSONException e) {
            LOG.error(e.getMessage());
        }

        if (savedWidgetsPreferencesDataArray == null) {
            savedWidgetsPreferencesDataArray = new JSONArray();
        }


        try {
            savedWidgetsPreferencesDataArray.put(new JSONArray(new String[]{AppWidgetId, HwAddress}));
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }


        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREFS_WIDGET_SETTINGS, savedWidgetsPreferencesDataArray.toString());
        editor.commit();
        editor.apply();
    }

    public boolean isWidgetInPrefs() {
        return isWidgetInPrefs;
    }

    public void deleteWidgetsPrefs(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREFS_WIDGET_SETTINGS, "");
        editor.commit();
        editor.apply();
    }

    public void showAppWidgetsPrefs(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedWidgetsPreferencesData = sharedPrefs.getString(PREFS_WIDGET_SETTINGS, "");
        JSONArray savedWidgetsPreferencesDataArray = null;
        try {
            savedWidgetsPreferencesDataArray = new JSONArray(savedWidgetsPreferencesData);
        } catch (
                JSONException e) {
            LOG.error(e.getMessage());
        }
        GB.toast("Saved app widget preferences: " + savedWidgetsPreferencesDataArray, Toast.LENGTH_SHORT, GB.INFO);
    }
}
