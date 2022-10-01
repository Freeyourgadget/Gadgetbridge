/*  Copyright (C) 2022 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import java.util.LinkedHashMap;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

public class HybridHRWatchfaceWidgetActivity extends AbstractSettingsActivity {
    private static int widgetIndex;
    private static HybridHRWatchfaceWidget widget;

    private static LinkedHashMap<String, String> widgetTypes;
    private static String[] widgetColors;

    private static final String WIDGET_2NDTZ_DEFAULT_TZ = "Etc/UTC";
    private static final int WIDGET_2NDTZ_DEFAULT_TIMEOUT = 15;
    private static final int WIDGET_CUSTOM_DEFAULT_TIMEOUT = 60;
    private static final Boolean WIDGET_CUSTOM_DEFAULT_HIDE_TEXT = true;
    private static final Boolean WIDGET_CUSTOM_DEFAULT_SHOW_CIRCLE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            widgetIndex = (int) bundle.getSerializable("widgetIndex");
            widget = (HybridHRWatchfaceWidget) bundle.getSerializable("widgetSettings");
        } else {
            throw new IllegalArgumentException("Must provide a widget object when invoking this activity");
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content, new HybridHRWatchfaceWidgetFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        // Hardware back button
        Intent output = new Intent();
        output.putExtra("widgetIndex", widgetIndex);
        output.putExtra("widgetSettings", widget);
        setResult(RESULT_OK, output);
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Action bar back button
            Intent output = new Intent();
            output.putExtra("widgetIndex", widgetIndex);
            output.putExtra("widgetSettings", widget);
            setResult(RESULT_OK, output);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class HybridHRWatchfaceWidgetFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener
    {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fossil_hr_widget_settings);

            widgetTypes = HybridHRWatchfaceWidget.getAvailableWidgetTypes(getActivity().getBaseContext());
            ListPreference widgetType = (ListPreference) findPreference("pref_hybridhr_widget_type");
            widgetType.setOnPreferenceChangeListener(this);
            widgetType.setEntries(widgetTypes.values().toArray(new String[0]));
            widgetType.setEntryValues(widgetTypes.keySet().toArray(new String[0]));
            widgetType.setValue(widget.getWidgetType());
            widgetType.setSummary(widgetTypes.get(widget.getWidgetType()));
            updateEnabledCategories();

            widgetColors = new String[]{getString(R.string.watchface_dialog_widget_color_white), getString(R.string.watchface_dialog_widget_color_black)};
            ListPreference widgetColor = (ListPreference) findPreference("pref_hybridhr_widget_color");
            widgetColor.setOnPreferenceChangeListener(this);
            widgetColor.setEntries(widgetColors);
            widgetColor.setEntryValues(new String[]{"0", "1"});
            widgetColor.setValueIndex(widget.getColor());
            widgetColor.setSummary(widgetColors[widget.getColor()]);

            ListPreference widgetBg = (ListPreference) findPreference("pref_hybridhr_widget_background");
            widgetBg.setOnPreferenceChangeListener(this);
            widgetBg.setValue(widget.getBackground());
            widgetBg.setSummary(widgetBg.getEntry());

            EditTextPreference posX = (EditTextPreference) findPreference("pref_hybridhr_widget_pos_x");
            posX.setOnPreferenceChangeListener(this);
            posX.setText(Integer.toString(widget.getPosX()));
            posX.setSummary(Integer.toString(widget.getPosX()));

            EditTextPreference posY = (EditTextPreference) findPreference("pref_hybridhr_widget_pos_y");
            posY.setOnPreferenceChangeListener(this);
            posY.setText(Integer.toString(widget.getPosY()));
            posY.setSummary(Integer.toString(widget.getPosY()));

            LinkedHashMap<String, String> positionPresets = new LinkedHashMap<String, String>();
            for (final HybridHRWidgetPosition position : widget.defaultPositions) {
                positionPresets.put(String.valueOf(position.hintStringResource), getString(position.hintStringResource));
            }
            ListPreference widgetPositionPreset = (ListPreference) findPreference("pref_hybridhr_widget_pos_preset");
            widgetPositionPreset.setOnPreferenceChangeListener(this);
            widgetPositionPreset.setEntries(positionPresets.values().toArray(new String[0]));
            widgetPositionPreset.setEntryValues(positionPresets.keySet().toArray(new String[0]));

            String[] timezonesList = TimeZone.getAvailableIDs();
            ListPreference widgetTimezone = (ListPreference) findPreference("pref_hybridhr_widget_timezone");
            widgetTimezone.setOnPreferenceChangeListener(this);
            widgetTimezone.setEntries(timezonesList);
            widgetTimezone.setEntryValues(timezonesList);
            widgetTimezone.setValue(widget.getExtraConfigString("tzName", WIDGET_2NDTZ_DEFAULT_TZ));
            widgetTimezone.setSummary(widget.getExtraConfigString("tzName", WIDGET_2NDTZ_DEFAULT_TZ));

            EditTextPreference timezoneDuration = (EditTextPreference) findPreference("pref_hybridhr_widget_timezone_timeout");
            timezoneDuration.setOnPreferenceChangeListener(this);
            timezoneDuration.setText(Integer.toString(widget.getExtraConfigInt("timeout_secs", WIDGET_2NDTZ_DEFAULT_TIMEOUT)));
            timezoneDuration.setSummary(Integer.toString(widget.getExtraConfigInt("timeout_secs", WIDGET_2NDTZ_DEFAULT_TIMEOUT)));

            EditTextPreference width = (EditTextPreference) findPreference("pref_hybridhr_widget_width");
            width.setOnPreferenceChangeListener(this);
            width.setText(Integer.toString(widget.getWidth()));
            width.setSummary(Integer.toString(widget.getWidth()));

            EditTextPreference customWidgetTimeout = (EditTextPreference) findPreference("pref_hybridhr_widget_custom_timeout");
            customWidgetTimeout.setOnPreferenceChangeListener(this);
            customWidgetTimeout.setText(Integer.toString(widget.getExtraConfigInt("update_timeout", WIDGET_CUSTOM_DEFAULT_TIMEOUT)));
            customWidgetTimeout.setSummary(Integer.toString(widget.getExtraConfigInt("update_timeout", WIDGET_CUSTOM_DEFAULT_TIMEOUT)));

            SwitchPreference customWidgetHideText = (SwitchPreference) findPreference("pref_hybridhr_widget_custom_hide_text");
            customWidgetHideText.setOnPreferenceChangeListener(this);
            customWidgetHideText.setChecked(widget.getExtraConfigBoolean("timeout_hide_text", WIDGET_CUSTOM_DEFAULT_HIDE_TEXT));

            SwitchPreference customWidgetShowCircle = (SwitchPreference) findPreference("pref_hybridhr_widget_custom_show_circle");
            customWidgetShowCircle.setOnPreferenceChangeListener(this);
            customWidgetShowCircle.setChecked(widget.getExtraConfigBoolean("timeout_show_circle", WIDGET_CUSTOM_DEFAULT_SHOW_CIRCLE));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case "pref_hybridhr_widget_type":
                    widget.setWidgetType(newValue.toString());
                    preference.setSummary(widgetTypes.get(widget.getWidgetType()));
                    updateEnabledCategories();
                    if (newValue.toString().equals("widget2ndTZ")) {
                        widget.setExtraConfigString("tzName", widget.getExtraConfigString("tzName", WIDGET_2NDTZ_DEFAULT_TZ));
                        widget.setExtraConfigInt("timeout_secs", widget.getExtraConfigInt("timeout_secs", WIDGET_2NDTZ_DEFAULT_TIMEOUT));
                    } else if (newValue.toString().equals("widgetCustom")) {
                        widget.setExtraConfigInt("update_timeout", widget.getExtraConfigInt("update_timeout", WIDGET_CUSTOM_DEFAULT_TIMEOUT));
                        widget.setExtraConfigBoolean("timeout_hide_text", widget.getExtraConfigBoolean("timeout_hide_text", WIDGET_CUSTOM_DEFAULT_HIDE_TEXT));
                        widget.setExtraConfigBoolean("timeout_show_circle", widget.getExtraConfigBoolean("timeout_show_circle", WIDGET_CUSTOM_DEFAULT_SHOW_CIRCLE));
                    }
                    break;
                case "pref_hybridhr_widget_color":
                    widget.setColor(Integer.parseInt(newValue.toString()));
                    preference.setSummary(widgetColors[widget.getColor()]);
                    break;
                case "pref_hybridhr_widget_background":
                    widget.setBackground(newValue.toString());
                    ((ListPreference)preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference)preference).getEntry());
                    break;
                case "pref_hybridhr_widget_pos_x":
                    widget.setPosX(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_widget_pos_y":
                    widget.setPosY(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_widget_pos_preset":
                    LinkedHashMap<String, String> positionPresets = new LinkedHashMap<String, String>();
                    for (final HybridHRWidgetPosition position : widget.defaultPositions){
                        if (newValue.toString().equals(String.valueOf(position.hintStringResource))) {
                            widget.setPosX(position.posX);
                            widget.setPosY(position.posY);
                            EditTextPreference prefPosX = (EditTextPreference) findPreference("pref_hybridhr_widget_pos_x");
                            EditTextPreference prefPosY = (EditTextPreference) findPreference("pref_hybridhr_widget_pos_y");
                            prefPosX.setSummary(String.valueOf(position.posX));
                            prefPosY.setSummary(String.valueOf(position.posY));
                        }
                    }
                    break;
                case "pref_hybridhr_widget_timezone":
                    widget.setExtraConfigString("tzName", newValue.toString());
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_widget_timezone_timeout":
                    widget.setExtraConfigInt("timeout_secs", Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_widget_width":
                    widget.setWidth(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_widget_custom_timeout":
                    widget.setExtraConfigInt("update_timeout", Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_widget_custom_hide_text":
                    widget.setExtraConfigBoolean("timeout_hide_text", (boolean) newValue);
                    break;
                case "pref_hybridhr_widget_custom_show_circle":
                    widget.setExtraConfigBoolean("timeout_show_circle", (boolean) newValue);
                    break;
            }
            return true;
        }

        private void updateEnabledCategories() {
            PreferenceCategory cat2ndTZ = (PreferenceCategory) findPreference("widget_pref_category_2nd_tz_widget");
            if (widget.getWidgetType().equals("widget2ndTZ")) {
                cat2ndTZ.setEnabled(true);
            } else {
                cat2ndTZ.setEnabled(false);
            }
            PreferenceCategory catCustom = (PreferenceCategory) findPreference("widget_pref_category_custom_widget");
            if (widget.getWidgetType().equals("widgetCustom")) {
                catCustom.setEnabled(true);
            } else {
                catCustom.setEnabled(false);
            }
        }
    }
}
