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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSettingsService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.XDatePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher;

public class GarminRealtimeSettingsFragment extends AbstractPreferenceFragment {
    private static final Logger LOG = LoggerFactory.getLogger(GarminRealtimeSettingsFragment.class);

    public static final String EXTRA_SCREEN_ID = "screenId";
    public static final String PREF_DEBUG = "garmin_rt_debug_mode";

    public static final int ROOT_SCREEN_ID = 36352;

    static final String FRAGMENT_TAG = "GARMIN_REALTIME_SETTINGS_FRAGMENT";

    private GBDevice device;
    private int screenId = ROOT_SCREEN_ID;

    private GdiSettingsService.ScreenDefinition screenDefinition;
    private GdiSettingsService.ScreenState screenState;

    public static final String EXTRA_PROTOBUF = "protobuf";

    public static final String ACTION_SCREEN_DEFINITION = "nodomain.freeyourgadget.gadgetbridge.garmin.realtime_settings.screen_definition";
    public static final String ACTION_SCREEN_STATE = "nodomain.freeyourgadget.gadgetbridge.garmin.realtime_settings.screen_state";
    public static final String ACTION_CHANGE = "nodomain.freeyourgadget.gadgetbridge.garmin.realtime_settings.change";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                LOG.error("Got null action");
                return;
            }

            switch (action) {
                case ACTION_SCREEN_DEFINITION:
                    final GdiSettingsService.ScreenDefinition incomingScreen;
                    try {
                        incomingScreen = GdiSettingsService.ScreenDefinition.parseFrom(intent.getByteArrayExtra(EXTRA_PROTOBUF));
                    } catch (final InvalidProtocolBufferException e) {
                        // should never happen
                        LOG.error("Failed to parse protobuf for screen definition on {}", screenId, e);
                        return;
                    }
                    if (incomingScreen.getScreenId() != screenId) {
                        return;
                    }
                    LOG.debug("Got screen definition for screenId={}", screenId);
                    screenDefinition = incomingScreen;
                    break;
                case ACTION_SCREEN_STATE:
                    final GdiSettingsService.ScreenState incomingState;
                    try {
                        incomingState = GdiSettingsService.ScreenState.parseFrom(intent.getByteArrayExtra(EXTRA_PROTOBUF));
                    } catch (final InvalidProtocolBufferException e) {
                        // should never happen
                        LOG.error("Failed to parse protobuf for screen state on {}", screenId, e);
                        return;
                    }
                    if (incomingState.getScreenId() != screenId) {
                        return;
                    }
                    LOG.debug("Got screen state for screenId={}", screenId);
                    screenState = incomingState;
                    break;
                case ACTION_CHANGE:
                    final GdiSettingsService.ChangeResponse incomingChange;
                    try {
                        incomingChange = GdiSettingsService.ChangeResponse.parseFrom(intent.getByteArrayExtra(EXTRA_PROTOBUF));
                    } catch (final InvalidProtocolBufferException e) {
                        // should never happen
                        LOG.error("Failed to parse protobuf for change", e);
                        return;
                    }
                    if (incomingChange.getState().getScreenId() != screenId) {
                        return;
                    }

                    if (incomingChange.getShouldReturn()) {
                        LOG.debug("Returning from {}", screenId);
                        requireActivity().finish();
                        return;
                    }

                    LOG.debug("Got screen change for screenId={}", screenId);

                    GBApplication.deviceService(device).onReadConfiguration("screenId:" + screenId);
                    return;
                default:
                    LOG.error("Unknown action {}", action);
                    return;
            }

            reload();
        }
    };

    private void setDevice(final GBDevice device) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("device", device);
        setArguments(args);
    }

    private void setScreenId(final int screenId) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putInt("screenId", screenId);
        setArguments(args);
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        this.device = arguments.getParcelable(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            return;
        }
        this.screenId = arguments.getInt(EXTRA_SCREEN_ID, ROOT_SCREEN_ID);
        if (screenId == 0) {
            return;
        }

        LOG.info("Opened realtime preferences screen for {}", screenId);

        getPreferenceManager().setSharedPreferencesName("garmin_rt_" + device.getAddress());
        setPreferencesFromResource(R.xml.garmin_realtime_settings, rootKey);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SCREEN_DEFINITION);
        filter.addAction(ACTION_SCREEN_STATE);
        filter.addAction(ACTION_CHANGE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filter);

        GBApplication.deviceService(device).onReadConfiguration("screenId:" + screenId);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    static GarminRealtimeSettingsFragment newInstance(final GBDevice device, final int screenId) {
        final GarminRealtimeSettingsFragment fragment = new GarminRealtimeSettingsFragment();
        fragment.setDevice(device);
        fragment.setScreenId(screenId);

        return fragment;
    }

    void refreshFromDevice() {
        screenDefinition = null;
        screenState = null;
        reload();
        GBApplication.deviceService(device).onReadConfiguration("screenId:" + screenId);
    }

    void reload() {
        final boolean debug = GBApplication.getDevicePrefs(device).getBoolean(PREF_DEBUG, BuildConfig.DEBUG);

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            LOG.error("Activity is null");
            return;
        }

        final PreferenceScreen prefScreen = findPreference(GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS);
        if (prefScreen == null) {
            LOG.error("Preference screen for {} is null", GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS);
            activity.finish();
            return;
        }

        if (screenDefinition == null || screenState == null) {
            ((GarminRealtimeSettingsActivity) activity).setActionBarTitle(activity.getString(R.string.loading));

            // Disable all existing preferences while loading
            for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
                prefScreen.getPreference(i).setEnabled(false);
            }

            return;
        }

        prefScreen.removeAll();

        if (debug) {
            final Preference pref = new PreferenceCategory(activity);
            pref.setIconSpaceReserved(false);
            pref.setTitle("Screen ID: " + screenId);
            pref.setPersistent(false);
            pref.setKey("rt_pref_header_" + screenId);
            prefScreen.addPreference(pref);
        }

        // Update the screen title, if any
        if (screenDefinition.hasTitle()) {
            final String title = screenDefinition.getTitle().getText();

            ((GarminRealtimeSettingsActivity) activity).setActionBarTitle(title);
        }

        final Map<Integer, GdiSettingsService.EntryState> stateById = new HashMap<>();
        for (final GdiSettingsService.EntryState state : screenState.getStateList()) {
            stateById.put(state.getId(), state);
        }

        for (final GdiSettingsService.ScreenEntry entry : screenDefinition.getEntryList()) {
            final GdiSettingsService.EntryState state = stateById.get(entry.getId());

            final Preference pref;
            boolean supported = true;

            if (entry.hasTarget()) {
                switch (entry.getTarget().getType()) {
                    case 0: // subscreen
                    case 9: // subscreen with options for a specific preference
                        pref = new Preference(activity);
                        pref.setOnPreferenceClickListener(preference -> {
                            final Intent newIntent = new Intent(requireContext(), GarminRealtimeSettingsActivity.class);
                            newIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                            newIntent.putExtra(GarminRealtimeSettingsActivity.EXTRA_SCREEN_ID, entry.getTarget().getSubscreen());
                            activity.startActivityForResult(newIntent, 0);
                            return true;
                        });
                        break;
                    case 1: // list preference
                        pref = new ListPreference(activity);
                        final CharSequence[] entries = new String[entry.getTarget().getOptions().getOptionList().size()];
                        int optionIndex = 0;
                        for (final GdiSettingsService.TargetOptionEntry option : entry.getTarget().getOptions().getOptionList()) {
                            entries[optionIndex++] = option.getTitle().getText();
                        }
                        final ListPreference listPreference = (ListPreference) pref;
                        listPreference.setEntries(entries);
                        listPreference.setEntryValues(entries);
                        listPreference.setValue(entries[Objects.requireNonNull(state).getSummary().getValueList().getIndex()].toString());
                        listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                            int newValueIdx = -1;
                            for (int i = 0; i < entries.length; i++) {
                                if (entries[i].equals(newValue.toString())) {
                                    newValueIdx = i;
                                    break;
                                }
                            }
                            if (newValueIdx < 0) {
                                LOG.error("Failed to find index for {}", newValue);
                                return false;
                            }

                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setOption(GdiSettingsService.ChangeRequest.Option.newBuilder()
                                                    .setIndex(newValueIdx)
                                            )
                            );
                            return true;
                        });
                        break;

                    case 3: // time
                        pref = new XTimePreference(activity, null);
                        ((XTimePreference) pref).setValue(
                                Objects.requireNonNull(state).getSummary().getValueTime().getSeconds() / 3600,
                                (Objects.requireNonNull(state).getSummary().getValueTime().getSeconds() % 3600) / 60
                        );
                        if (state.getSummary().getValueTime().hasTimeFormat()) {
                            final int timeFormat = state.getSummary().getValueTime().getTimeFormat();
                            switch (timeFormat) {
                                case 0: // 12h
                                    ((XTimePreference) pref).setFormat(XTimePreference.Format.FORMAT_12H);
                                    break;
                                case 1: // 24h
                                    ((XTimePreference) pref).setFormat(XTimePreference.Format.FORMAT_24H);
                                    break;
                            }
                        }
                        pref.setSummary(state.getSummary().getValueDate().getSubtitle().getText());
                        pref.setOnPreferenceChangeListener((preference, newValue) -> {
                            final String[] pieces = newValue.toString().split(":");

                            final int hour = Integer.parseInt(pieces[0]);
                            final int minute = Integer.parseInt(pieces[1]);

                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setTime(GdiSettingsService.ChangeRequest.Time.newBuilder()
                                                    .setSeconds(hour * 3600 + minute * 60)
                                            )
                            );
                            return true;
                        });
                        break;
                    case 5: // number picker
                        pref = new EditTextPreference(activity);
                        ((EditTextPreference) pref).setText(String.valueOf(state.getSummary().getValueNumber().getValue()));
                        ((EditTextPreference) pref).setSummary(state.getSummary().getValueNumber().getSubtitle().getText());

                        ((EditTextPreference) pref).setOnBindEditTextListener(p -> {
                            p.setInputType(InputType.TYPE_CLASS_NUMBER);
                            int minValue = Integer.MIN_VALUE;
                            int maxValue = Integer.MAX_VALUE;
                            if (entry.getTarget().getNumberPicker().hasMin()) {
                                minValue = entry.getTarget().getNumberPicker().getMin();
                            }
                            if (entry.getTarget().getNumberPicker().hasMax()) {
                                maxValue = entry.getTarget().getNumberPicker().getMax();
                            }
                            p.addTextChangedListener(new MinMaxTextWatcher(p, minValue, maxValue));
                            p.setSelection(p.getText().length());
                        });
                        ((EditTextPreference) pref).setOnPreferenceChangeListener((preference, newValue) -> {
                            final int newValueInt = Integer.parseInt(newValue.toString());

                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setNumber(GdiSettingsService.ChangeRequest.Number.newBuilder()
                                                    .setValue(newValueInt)
                                            )
                            );
                            return true;
                        });
                        break;
                    case 6: // activity
                        switch (entry.getTarget().getActivity()) {
                            case 2: // garmin pay
                            case 7: // text responses
                            case 8: // music providers
                                pref = new Preference(activity);
                                pref.setVisible(debug);
                                pref.setEnabled(false);
                                break;
                            default:
                                supported = false;
                                pref = new Preference(activity);
                                break;
                        }

                        break;
                    case 7: // hidden?
                        pref = new Preference(activity);
                        pref.setVisible(debug);
                        pref.setEnabled(false);
                        break;
                    case 10: // date picker
                        pref = new XDatePreference(activity, null);
                        ((XDatePreference) pref).setValue(
                                Objects.requireNonNull(state).getSummary().getValueDate().getCurrentDate().getYear(),
                                Objects.requireNonNull(state).getSummary().getValueDate().getCurrentDate().getMonth(),
                                Objects.requireNonNull(state).getSummary().getValueDate().getCurrentDate().getDay()
                        );
                        if (state.getSummary().getValueDate().hasMinDate()) {
                            final Calendar calendar = GregorianCalendar.getInstance();
                            calendar.set(
                                    state.getSummary().getValueDate().getMinDate().getYear(),
                                    state.getSummary().getValueDate().getMinDate().getMonth() - 1,
                                    state.getSummary().getValueDate().getMinDate().getDay()
                            );
                            ((XDatePreference) pref).setMinDate(calendar.getTimeInMillis());
                        }
                        if (state.getSummary().getValueDate().hasMaxDate()) {
                            final Calendar calendar = GregorianCalendar.getInstance();
                            calendar.set(
                                    state.getSummary().getValueDate().getMaxDate().getYear(),
                                    state.getSummary().getValueDate().getMaxDate().getMonth() - 1,
                                    state.getSummary().getValueDate().getMaxDate().getDay()
                            );
                            ((XDatePreference) pref).setMaxDate(calendar.getTimeInMillis());
                        }
                        pref.setSummary(state.getSummary().getValueDate().getSubtitle().getText());
                        pref.setOnPreferenceChangeListener((preference, newValue) -> {
                            final String[] pieces = newValue.toString().split("-");

                            final int year = Integer.parseInt(pieces[0]);
                            final int month = Integer.parseInt(pieces[1]);
                            final int day = Integer.parseInt(pieces[2]);

                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setNewDate(GdiSettingsService.ChangeRequest.NewDate.newBuilder()
                                                    .setValue(GdiSettingsService.Date.newBuilder()
                                                            .setYear(year).setMonth(month).setDay(day)
                                                    )
                                            )
                            );

                            return true;
                        });
                        break;
                    case 12: // Connect IQ Store
                        pref = new Preference(activity);
                        pref.setVisible(debug);
                        pref.setEnabled(false);
                        break;
                    case 13: // height
                        pref = new EditTextPreference(activity);
                        ((EditTextPreference) pref).setText(String.valueOf(state.getSummary().getValueHeight().getValue()));
                        ((EditTextPreference) pref).setSummary(state.getSummary().getValueHeight().getSubtitle().getText());
                        if (state.getSummary().getValueHeight().getUnit() == 0) {
                            ((EditTextPreference) pref).setDialogTitle(R.string.activity_prefs_height_cm);
                            ((EditTextPreference) pref).setTitle(R.string.activity_prefs_height_cm);
                        } else {
                            ((EditTextPreference) pref).setDialogTitle(R.string.activity_prefs_height_inches);
                            ((EditTextPreference) pref).setTitle(R.string.activity_prefs_height_inches);
                        }
                        ((EditTextPreference) pref).setOnBindEditTextListener(p -> {
                            p.setInputType(InputType.TYPE_CLASS_NUMBER);
                            p.addTextChangedListener(new MinMaxTextWatcher(p, 0, 300));
                            p.setSelection(p.getText().length());
                        });
                        ((EditTextPreference) pref).setOnPreferenceChangeListener((preference, newValue) -> {
                            final int newValueInt = Integer.parseInt(newValue.toString());

                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setHeight(GdiSettingsService.ChangeRequest.Height.newBuilder()
                                                    .setValue(newValueInt)
                                                    .setUnit(state.getSummary().getValueHeight().getUnit())
                                            )
                            );
                            return true;
                        });
                        break;
                    default:
                        supported = false;
                        pref = new Preference(activity);
                }
            } else { // No target
                switch (entry.getType()) {
                    case 0: // notice
                        pref = new Preference(activity);
                        pref.setSummary(entry.getTitle().getText());
                        break;
                    case 1: // category
                        pref = new PreferenceCategory(activity);
                        break;
                    case 2: // space
                        pref = new PreferenceCategory(activity);
                        pref.setTitle("");
                        break;
                    case 3: // switch
                        pref = new SwitchPreferenceCompat(activity);
                        pref.setLayoutResource(R.layout.preference_checkbox);
                        ((SwitchPreferenceCompat) pref).setChecked(Objects.requireNonNull(state).getSwitch().getEnabled());
                        ((SwitchPreferenceCompat) pref).setSummary(Objects.requireNonNull(state).getSwitch().getTitle().getText());
                        pref.setOnPreferenceChangeListener((preference, newValue) -> {
                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setSwitch(GdiSettingsService.ChangeRequest.Switch.newBuilder()
                                                    .setValue((Boolean) newValue)
                                            )
                            );
                            return true;
                        });

                        break;
                    case 4: // single line + optional icon
                    case 5: // double line
                        pref = new Preference(activity);
                        break;
                    case 18: // single line with action (eg. glances)
                    case 7: // single line, normally in list for selection?
                        pref = new Preference(activity);
                        pref.setOnPreferenceClickListener(preference -> {
                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                            );
                            return true;
                        });
                        break;
                    case 8: // device + status?
                    case 9: // finish setup
                    case 10: // find my device
                    case 11: // preferred activity tracker
                    case 13: // help & info
                        pref = new Preference(activity);
                        pref.setVisible(debug);
                        pref.setEnabled(false);
                        break;
                    case 15: // sortable + delete
                        // Add all sortable items and then continue
                        final String moveUpStr = activity.getString(R.string.widget_move_up);
                        final String moveDownStr = activity.getString(R.string.widget_move_down);
                        final String deleteStr = activity.getString(R.string.appmananger_app_delete);

                        for (int i = 0; i < entry.getSortOptions().getEntriesCount(); i++) {
                            final GdiSettingsService.SortEntry sortEntry = entry.getSortOptions().getEntries(i);
                            final List<String> sortableOptions = new ArrayList<>(3);
                            if (i > 0) {
                                sortableOptions.add(moveUpStr);
                            }
                            if (i < entry.getSortOptions().getEntriesCount() - 1) {
                                sortableOptions.add(moveDownStr);
                            }
                            sortableOptions.add(deleteStr);
                            final ArrayAdapter<String> sortOptionsAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, sortableOptions);
                            final int iFinal = i;
                            final Preference sortPref = new Preference(activity);
                            sortPref.setTitle(sortEntry.getTitle().getText());
                            sortPref.setPersistent(false);
                            sortPref.setIconSpaceReserved(false);
                            sortPref.setKey("rt_pref_" + screenId + "_" + entry.getId() + "__" + sortEntry.getId());
                            sortPref.setOnPreferenceClickListener(preference -> {
                                new MaterialAlertDialogBuilder(activity)
                                        .setTitle(sortPref.getTitle())
                                        .setAdapter(sortOptionsAdapter, (dialogInterface, j) -> {
                                            final String option = sortableOptions.get(j);
                                            int moveOffset = 0;
                                            if (option.equals(moveUpStr)) {
                                                moveOffset = -1;
                                            } else if (option.equals(moveDownStr)) {
                                                moveOffset = 1;
                                            }

                                            if (moveOffset != 0) {
                                                sortPref.setEnabled(false);
                                                sendChangeRequest(
                                                        GdiSettingsService.ChangeRequest.newBuilder()
                                                                .setScreenId(screenId)
                                                                .setEntryId(sortEntry.getId())
                                                                .setPosition(GdiSettingsService.ChangeRequest.Position.newBuilder()
                                                                        .setIndex(iFinal + moveOffset)
                                                                )
                                                );
                                                return;
                                            }

                                            if (option.equals(deleteStr)) {
                                                sortPref.setEnabled(false);
                                                sendChangeRequest(
                                                        GdiSettingsService.ChangeRequest.newBuilder()
                                                                .setScreenId(screenId)
                                                                .setEntryId(sortEntry.getId())
                                                                .setPosition(GdiSettingsService.ChangeRequest.Position.newBuilder()
                                                                        .setDelete(true)
                                                                )
                                                );
                                            }
                                        }).setNegativeButton(android.R.string.cancel, null)
                                        .create().show();
                                return true;
                            });
                            prefScreen.addPreference(sortPref);
                        }

                        continue; // We already added all options above, continue
                    case 16: // text
                        pref = new EditTextPreference(activity);

                        ((EditTextPreference) pref).setOnBindEditTextListener(p -> {
                            int maxValue = Integer.MAX_VALUE;
                            if (entry.getTextOption().hasLimits() && entry.getTextOption().getLimits().hasMaxLength()) {
                                p.setFilters(new InputFilter[]{new InputFilter.LengthFilter(entry.getTextOption().getLimits().getMaxLength())});
                            }
                            p.setSelection(p.getText().length());
                        });
                        ((EditTextPreference) pref).setOnPreferenceChangeListener((preference, newValue) -> {
                            if (StringUtils.isNullOrEmpty(newValue.toString())) {
                                return true;
                            }
                            pref.setEnabled(false);
                            sendChangeRequest(
                                    GdiSettingsService.ChangeRequest.newBuilder()
                                            .setScreenId(screenId)
                                            .setEntryId(entry.getId())
                                            .setText(GdiSettingsService.ChangeRequest.Text.newBuilder()
                                                    .setValue(newValue.toString())
                                            )
                            );
                            return true;
                        });
                        break;
                    default:
                        supported = false;
                        pref = new Preference(activity);
                }
            }

            if (StringUtils.isNullOrEmpty(pref.getTitle()) && entry.getType() != 0 && entry.getType() != 2) {
                pref.setTitle(!StringUtils.isEmpty(entry.getTitle().getText()) ? entry.getTitle().getText() : activity.getString(R.string.unknown));

                if (pref instanceof DialogPreference) {
                    ((DialogPreference) pref).setDialogTitle(pref.getTitle());
                }
            }

            final int icon = getIcon(entry);
            if (icon != 0) {
                pref.setIcon(icon);
            } else {
                pref.setIconSpaceReserved(false);
            }

            if (state != null && !StringUtils.isEmpty(state.getSummary().getTitle().getText())) {
                pref.setSummary(state.getSummary().getTitle().getText());
            }

            if (state != null && state.hasState()) {
                switch (state.getState()) {
                    case 1:
                        pref.setVisible(false);
                        break;
                    case 2:
                        pref.setEnabled(false);
                        break;
                    default:
                        LOG.warn("Unknown state value {}", state.getState());
                }
            }

            if (!supported) {
                pref.setEnabled(false);

                if (StringUtils.isNullOrEmpty(pref.getSummary())) {
                    pref.setSummary(R.string.unsupported);
                } else {
                    pref.setSummary(activity.getString(R.string.menuitem_unsupported, pref.getSummary()));
                }
            }

            if (debug) {
                final StringBuilder sb = new StringBuilder();

                if (pref.getSummary() != null && pref.getSummary().length() != 0) {
                    sb.append(pref.getSummary()).append("\n");
                }

                sb.append("id=").append(entry.getId());
                sb.append(", type=").append(entry.getType());

                if (entry.hasTarget()) {
                    sb.append(", targetType=").append(entry.getTarget().getType());
                }

                pref.setSummary(sb.toString());
            }

            pref.setPersistent(false);
            pref.setKey("rt_pref_" + screenId + "_" + entry.getId());
            prefScreen.addPreference(pref);
        }

        // If no preferences after the last visible preference category are visible, hide it
        for (int i = prefScreen.getPreferenceCount() - 1; i >= 0; i--) {
            final Preference lastVisiblePreference = prefScreen.getPreference(i);
            if (lastVisiblePreference.isVisible()) {
                break;
            }
            if (lastVisiblePreference instanceof PreferenceCategory) {
                lastVisiblePreference.setVisible(false);
                break;
            }
        }
    }

    @DrawableRes
    private int getIcon(final GdiSettingsService.ScreenEntry entry) {
        if (entry.hasIcon()) {
            switch (entry.getIcon()) {
                //
                // Main menu
                case 20: // Garmin Pay
                    return 0;
                case 21: // Text Responses
                    return R.drawable.ic_reply;
                case 4: // Clocks
                    return R.drawable.ic_access_time;
                case 2: // Glances
                    return R.drawable.ic_widgets;
                case 3: // Controls
                    return R.drawable.ic_menu;
                case 1: // Activities / Apps, have the same icon
                    return R.drawable.ic_activity_unknown_small;
                case 39: // Shortcut
                    return R.drawable.ic_shortcut;
                case 27: // Notifications & Alerts
                    return R.drawable.ic_notifications;
                case 46: // Watch Sensors
                    return R.drawable.ic_sensor_calibration;
                case 47: // Accessories
                    return R.drawable.ic_bluetooth_searching;
                case 7: // Music
                    return R.drawable.ic_music_note;
                case 13: // Audio Prompts
                    return R.drawable.ic_volume_up;
                case 14: // User Profile
                    return R.drawable.ic_person;
                case 15: // Safety & Tracking
                    return R.drawable.ic_health;
                case 16: // Activity Tracking
                    return R.drawable.ic_activity_unknown_small;
                case 19: // System
                    return R.drawable.ic_settings;

                //
                // Sortable screens (glances, apps, etc)
                case 33:
                    return R.drawable.ic_add_gray;
            }
        }

        return 0;
    }

    void toggleDebug() {
        final Prefs prefs = GBApplication.getDevicePrefs(device);
        prefs.getPreferences().edit()
                .putBoolean(PREF_DEBUG, !prefs.getBoolean(PREF_DEBUG, BuildConfig.DEBUG))
                .apply();

        reload();
    }

    void shareDebug() {
        final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");

        final StringBuilder sb = new StringBuilder();

        sb.append("screenId: ").append(screenId);
        sb.append("\n");

        sb.append("settingsScreen: ");
        if (screenDefinition != null) {
            sb.append(GB.hexdump(screenDefinition.toByteArray()));
        } else {
            sb.append("null");
        }
        sb.append("\n");

        sb.append("settingsState: ");
        if (screenState != null) {
            sb.append(GB.hexdump(screenState.toByteArray()));
        } else {
            sb.append("null");
        }
        sb.append("\n");

        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Garmin Settings Screen " + screenId);
        intent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());

        try {
            startActivity(Intent.createChooser(intent, "Share debug info"));
        } catch (final ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Failed to share text", Toast.LENGTH_LONG).show();
        }
    }

    private void sendChangeRequest(final GdiSettingsService.ChangeRequest.Builder changeRequest) {
        screenDefinition = null;
        screenState = null;
        final GdiSmartProto.Smart smart = GdiSmartProto.Smart.newBuilder()
                .setSettingsService(GdiSettingsService.SettingsService.newBuilder()
                        .setChangeRequest(changeRequest)
                ).build();
        GBApplication.deviceService(device).onSendConfiguration("protobuf:" + GB.hexdump(smart.toByteArray()));
    }
}
