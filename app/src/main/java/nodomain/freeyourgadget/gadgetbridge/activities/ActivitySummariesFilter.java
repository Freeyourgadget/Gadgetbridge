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
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class ActivitySummariesFilter extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesActivity.class);
    private static final String DATE_FILTER_FROM = "dateFromFilter";
    private static final String DATE_FILTER_TO = "dateToFilter";
    int activityFilter = 0;
    long dateFromFilter = 0;
    long dateToFilter = 0;
    String nameContainsFilter;
    HashMap<String, Integer> activityKindMap = new HashMap<>(1);
    int BACKGROUND_COLOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getIntent().getExtras();

        activityKindMap = (HashMap<String, Integer>) bundle.getSerializable("activityKindMap");
        activityFilter = bundle.getInt("activityFilter", 0);
        dateFromFilter = bundle.getLong("dateFromFilter", 0);
        dateToFilter = bundle.getLong("dateToFilter", 0);
        nameContainsFilter = bundle.getString("nameContainsFilter");

        Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.sport_activity_filter);
        }
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(appContext);

        //get spinner ready - assign data, set selected item...
        final Spinner filterKindSpinner = findViewById(R.id.select_kind);
        ArrayList<String> spinnerArray = new ArrayList<>(activityKindMap.keySet());
        //ensure that all items is always first in the list
        spinnerArray.remove(getString(R.string.activity_summaries_all_activities));
        spinnerArray.add(0, getString(R.string.activity_summaries_all_activities));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        filterKindSpinner.setAdapter(dataAdapter);
        filterKindSpinner.setSelection(dataAdapter.getPosition(getKeyByValue(activityFilter)));
        addListenerOnSpinnerItemSelection();

        final LinearLayout filterfrom = findViewById(R.id.filterfrom);
        final LinearLayout filterto = findViewById(R.id.filterto);
        final EditText nameContainsFilterdata = findViewById(R.id.textViewNameData);
        nameContainsFilterdata.setBackgroundDrawable(null);

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
                intent.putExtra("activityFilter", activityFilter);
                intent.putExtra("dateFromFilter", dateFromFilter);
                intent.putExtra("dateToFilter", dateToFilter);
                intent.putExtra("nameContainsFilter", nameContainsFilter);
                setResult(1, intent);
                finish();
            }
        });

        //set current values coming from parent
        update_filter_fields();

        filterfrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate(DATE_FILTER_FROM, dateFromFilter);

            }
        });

        filterto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate(DATE_FILTER_TO, dateToFilter);
            }
        });

    }

    public String getKeyByValue(Integer value) {
        for (Map.Entry<String, Integer> entry : activityKindMap.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }


    public void addListenerOnSpinnerItemSelection() {
        Spinner spinner = findViewById(R.id.select_kind);
        spinner.setOnItemSelectedListener(new CustomOnItemSelectedListener());
    }

    public void update_filter_fields() {
        TextView filterDateFromDataView = findViewById(R.id.textViewFromData);
        TextView filterDateToDataView = findViewById(R.id.textViewToData);
        Button reset_filter_button = findViewById(R.id.reset_filter_button);
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
        if (nameContainsFilter != null && !nameContainsFilter.equals(nameContainsFilterdata.getText().toString())) {
            nameContainsFilterdata.setText(nameContainsFilter);
        }
        if (dateToFilter != 0 || dateFromFilter != 0 || activityFilter != 0 || nameContainsFilterdata.length() > 0) {
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

                if (filter == DATE_FILTER_FROM) {
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

    public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            activityFilter = activityKindMap.get(parent.getItemAtPosition(pos));
            update_filter_fields();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

}
