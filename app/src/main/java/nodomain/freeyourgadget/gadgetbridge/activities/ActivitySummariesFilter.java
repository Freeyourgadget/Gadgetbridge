/*  Copyright (C) 2019-2020 vanous

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconItem;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;


public class ActivitySummariesFilter extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesActivity.class);
    private static final String DATE_FILTER_FROM = "dateFromFilter";
    private static final String DATE_FILTER_TO = "dateToFilter";
    public static long ALL_DEVICES = 999;
    int activityFilter = 0;
    long dateFromFilter = 0;
    long dateToFilter = 0;
    String nameContainsFilter;
    HashMap<String, Integer> activityKindMap = new HashMap<>(1);
    List<Long> itemsFilter;
    long deviceFilter;
    long initial_deviceFilter;
    int BACKGROUND_COLOR;
    LinkedHashMap<String, Pair<Long, Integer>> allDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getIntent().getExtras();

        activityKindMap = (HashMap<String, Integer>) bundle.getSerializable("activityKindMap");
        itemsFilter = (List<Long>) bundle.getSerializable("itemsFilter");
        activityFilter = bundle.getInt("activityFilter", 0);
        dateFromFilter = bundle.getLong("dateFromFilter", 0);
        dateToFilter = bundle.getLong("dateToFilter", 0);
        initial_deviceFilter = bundle.getLong("initial_deviceFilter", 0);
        deviceFilter = bundle.getLong("deviceFilter", 0);
        nameContainsFilter = bundle.getString("nameContainsFilter");

        Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.sport_activity_filter);
        }
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(appContext);

        allDevices = getAllDevices(appContext);

        //device filter spinner
        final Spinner deviceFilterSpinner = findViewById(R.id.select_device);
        ArrayList<SpinnerWithIconItem> filterDevicesArray = new ArrayList<>();
        for (Map.Entry<String, Pair<Long, Integer>> item : allDevices.entrySet()) {
            filterDevicesArray.add(new SpinnerWithIconItem(item.getKey(), item.getValue().first, item.getValue().second));
        }
        final SpinnerWithIconAdapter filterDevicesAdapter = new SpinnerWithIconAdapter(this,
                R.layout.spinner_with_image_layout, R.id.spinner_item_text, filterDevicesArray);
        deviceFilterSpinner.setAdapter(filterDevicesAdapter);
        deviceFilterSpinner.setSelection(filterDevicesAdapter.getItemPositionForSelection(getDeviceById(deviceFilter)));
        addListenerOnSpinnerDeviceSelection();

        //Kind filter spinner - assign data, set selected item...
        final Spinner filterKindSpinner = findViewById(R.id.select_kind);
        ArrayList<SpinnerWithIconItem> kindArray = new ArrayList<>();

        for (Map.Entry<String, Integer> item : activityKindMap.entrySet()) {
            if (item.getValue() == 0) continue; //do not put here All devices, but we do need them in the array
            kindArray.add(new SpinnerWithIconItem(item.getKey(), new Long(item.getValue()), ActivityKind.getIconId(item.getValue())));
        }

        //ensure that all items is always first in the list, this is an issue on old android
        SpinnerWithIconItem allActivities = new SpinnerWithIconItem(getString(R.string.activity_summaries_all_activities), new Long(0), ActivityKind.getIconId(0));
        kindArray.add(0, allActivities);

        SpinnerWithIconAdapter adapter = new SpinnerWithIconAdapter(this,
                R.layout.spinner_with_image_layout, R.id.spinner_item_text, kindArray);

        SpinnerWithIconItem selectedActivity = getKindByValue(activityFilter);
        int selectedPosition = adapter.getItemPositionForSelection(selectedActivity);

        filterKindSpinner.setAdapter(adapter);
        filterKindSpinner.setSelection(selectedPosition);
        addListenerOnSpinnerKindSelection();


        //quick date filter selection
        final Spinner quick_filter_period_select = findViewById(R.id.quick_filter_period_select);
        ArrayList<String> quickDateArray = new ArrayList<>(activityKindMap.keySet());

        ArrayList activity_filter_quick_filter_period_items = new ArrayList(Arrays.asList(getResources().getStringArray(R.array.activity_filter_quick_filter_period_items)));
        ArrayAdapter<String> filterDateAdapter = new ArrayAdapter<String>(this,
                R.layout.simple_spinner_item_themed, activity_filter_quick_filter_period_items);
        quick_filter_period_select.setAdapter(filterDateAdapter);
        addListenerOnQuickFilterSelection();

        //set current values coming from parent
        update_filter_fields();

        final LinearLayout filterfrom = findViewById(R.id.filterfrom);
        final LinearLayout filterto = findViewById(R.id.filterto);
        final EditText nameContainsFilterdata = findViewById(R.id.textViewNameData);

        final Button reset_filter_button = findViewById(R.id.reset_filter_button);
        final Button apply_filter_button = findViewById(R.id.apply_filter_button);

        nameContainsFilterdata.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                nameContainsFilter = s.toString();
                update_filter_fields();
            }
        });


        reset_filter_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityFilter = 0;
                dateFromFilter = 0;
                dateToFilter = 0;
                nameContainsFilter = "";
                filterKindSpinner.setSelection(0);
                itemsFilter = null;
                deviceFilterSpinner.setSelection(filterDevicesAdapter.getItemPositionForSelection(getDeviceById(initial_deviceFilter)));
                quick_filter_period_select.setSelection(0);
                update_filter_fields();
            }
        });

        apply_filter_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = nameContainsFilterdata.getText().toString();
                if (text != null && text.length() > 0) {
                    nameContainsFilter = text;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putInt("activityFilter", activityFilter);
                bundle.putSerializable("itemsFilter", (Serializable) itemsFilter);
                bundle.putLong("dateFromFilter", dateFromFilter);
                bundle.putLong("dateToFilter", dateToFilter);
                bundle.putLong("deviceFilter", deviceFilter);
                bundle.putString("nameContainsFilter", nameContainsFilter);
                intent.putExtras(bundle);
                setResult(1, intent);
                finish();
            }
        });


        filterfrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate(DATE_FILTER_FROM, dateFromFilter);
                quick_filter_period_select.setSelection(0);


            }
        });

        filterto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate(DATE_FILTER_TO, dateToFilter);
                quick_filter_period_select.setSelection(0);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addListenerOnSpinnerDeviceSelection() {
        Spinner spinner = findViewById(R.id.select_device);
        spinner.setOnItemSelectedListener(new CustomOnDeviceSelectedListener());
    }

    public void addListenerOnSpinnerKindSelection() {
        Spinner spinner = findViewById(R.id.select_kind);
        spinner.setOnItemSelectedListener(new CustomOnKindSelectedListener());
    }

    public void addListenerOnQuickFilterSelection() {
        Spinner spinner = findViewById(R.id.quick_filter_period_select);
        spinner.setOnItemSelectedListener(new CustomQuickFilterSelectionListener());
    }

    public void update_filter_fields() {
        TextView filterDateFromDataView = findViewById(R.id.textViewFromData);
        TextView filterDateToDataView = findViewById(R.id.textViewToData);
        Button reset_filter_button = findViewById(R.id.reset_filter_button);
        TextView textViewItemsData = findViewById(R.id.textViewItemsData);

        final EditText nameContainsFilterdata = findViewById(R.id.textViewNameData);

        if (dateFromFilter > 0) {
            filterDateFromDataView.setText(DateTimeUtils.formatDate(new Date(dateFromFilter)));
        } else {
            filterDateFromDataView.setText("");
        }

        if (dateToFilter > 0) {
            filterDateToDataView.setText(DateTimeUtils.formatDate(new Date(dateToFilter)));
        } else {
            filterDateToDataView.setText("");
        }

        if (dateToFilter < dateFromFilter && dateToFilter > 0) {
            filterDateFromDataView.setBackgroundColor(Color.RED);
            filterDateToDataView.setBackgroundColor(Color.RED);
        } else {
            filterDateFromDataView.setBackgroundColor(BACKGROUND_COLOR);
            filterDateToDataView.setBackgroundColor(BACKGROUND_COLOR);
        }

        if (itemsFilter != null) {
            textViewItemsData.setText(String.format("%s", itemsFilter.size()));
        } else {
            textViewItemsData.setText("0");
        }

        if (nameContainsFilter != null && !nameContainsFilter.equals(nameContainsFilterdata.getText().toString())) {
            nameContainsFilterdata.setText(nameContainsFilter);
        }
        if (dateToFilter != 0 || dateFromFilter != 0 || activityFilter != 0 || nameContainsFilterdata.length() > 0 || itemsFilter != null || deviceFilter != initial_deviceFilter) {
            reset_filter_button.getBackground().clearColorFilter();

        } else {
            reset_filter_button.getBackground().setColorFilter(new LightingColorFilter(0x0, 0x00888888));
        }
    }

    public void getDate(final String filter, long currentDatemillis) {
        Calendar currentDate = Calendar.getInstance();
        if (currentDatemillis > 0) {
            currentDate = GregorianCalendar.getInstance();
            currentDate.setTimeInMillis(currentDatemillis);
        }

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();

                if (filter.equals(DATE_FILTER_FROM)) {
                    date.set(year, monthOfYear, dayOfMonth, 0, 0);
                    dateFromFilter = date.getTimeInMillis();
                } else {
                    date.set(year, monthOfYear, dayOfMonth, 23, 59);
                    dateToFilter = date.getTimeInMillis();
                }
                update_filter_fields();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    private void setTimePeriodFilter(String selection) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        long firstdate;
        long lastdate;

        switch (selection) {
            case "thisweek":
                date.set(Calendar.DAY_OF_WEEK, date.getFirstDayOfWeek());
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "thismonth":
                date.set(Calendar.DAY_OF_MONTH, 1);
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "lastweek":
                int i = date.get(Calendar.DAY_OF_WEEK) - date.getFirstDayOfWeek();
                date.add(Calendar.DATE, -i - 7);
                firstdate = date.getTimeInMillis();
                date.add(Calendar.DATE, 6);
                lastdate = date.getTimeInMillis();
                break;
            case "lastmonth":
                date.set(Calendar.DATE, 1);
                date.add(Calendar.DAY_OF_MONTH, -1);
                lastdate = date.getTimeInMillis();
                date.set(Calendar.DATE, 1);
                firstdate = date.getTimeInMillis();
                break;
            case "7days":
                date.add(Calendar.DATE, -7);
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "30days":
                date.add(Calendar.DATE, -30);
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            default:
                return;
        }
        dateFromFilter = firstdate;
        dateToFilter = lastdate;
        update_filter_fields();
    }

    public LinkedHashMap getAllDevices(Context appContext) {
        DaoSession daoSession;
        GBApplication gbApp = (GBApplication) appContext;
        LinkedHashMap<String, Pair<Long, Integer>> newMap = new LinkedHashMap<>(1);
        List<? extends GBDevice> devices = gbApp.getDeviceManager().getDevices();
        newMap.put(getString(R.string.activity_summaries_all_devices), new Pair(ALL_DEVICES, R.drawable.ic_device_default_disabled));

        try (DBHandler handler = GBApplication.acquireDB()) {
            daoSession = handler.getDaoSession();
            for (GBDevice device : devices) {
                DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                Device dbDevice = DBHelper.findDevice(device, daoSession);
                int icon = device.isInitialized() ? device.getType().getIcon() : device.getType().getDisabledIcon();
                if (dbDevice != null && coordinator != null
                        && coordinator.supportsActivityTracks()
                        && !newMap.containsKey(device.getAliasOrName())) {
                    newMap.put(device.getAliasOrName(), new Pair(dbDevice.getId(), icon));
                }
            }

        } catch (Exception e) {
            LOG.debug("Error getting list of all devices: " + e);
        }
        return newMap;
    }

    public SpinnerWithIconItem getKindByValue(Integer value) {
        for (Map.Entry<String, Integer> entry : activityKindMap.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return new SpinnerWithIconItem(entry.getKey(),
                        new Long(entry.getValue()),
                        ActivityKind.getIconId(entry.getValue()));
            }
        }
        return null;
    }

    public SpinnerWithIconItem getDeviceById(long id) {
        for (Map.Entry<String, Pair<Long, Integer>> device : allDevices.entrySet()) {
            if (Objects.equals(id, device.getValue().first)) {
                return new SpinnerWithIconItem(device.getKey(),
                        device.getValue().first,
                        device.getValue().second);
            }
        }
        return null;
    }

    public class CustomOnKindSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
            String activity = selectedItem.getText();
            activityFilter = activityKindMap.get(activity);
            update_filter_fields();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

    public class CustomOnDeviceSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
            deviceFilter = selectedItem.getId();
            update_filter_fields();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

    public class CustomQuickFilterSelectionListener implements AdapterView.OnItemSelectedListener {
        ArrayList activity_filter_quick_filter_period_values = new ArrayList(Arrays.asList(getResources().getStringArray(R.array.activity_filter_quick_filter_period_values)));
        String selection;

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            selection = activity_filter_quick_filter_period_values.get(pos).toString();
            setTimePeriodFilter(selection);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

}
