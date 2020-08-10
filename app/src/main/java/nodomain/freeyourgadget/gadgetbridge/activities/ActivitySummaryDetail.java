/*  Copyright (C) 2015-2020 abettenburg, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Lem Dulfo

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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ActivitySummaryDetail extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryDetail.class);
    private GBDevice mGBDevice;
    private JSONObject groupData = setGroups();
    private boolean show_raw_data = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_details);
        Intent intent = getIntent();
        mGBDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);

        final String gpxTrack = intent.getStringExtra("GpxTrack");
        Button show_track_btn = (Button) findViewById(R.id.showTrack);
        show_track_btn.setVisibility(View.GONE);

        if (gpxTrack != null) {
            show_track_btn.setVisibility(View.VISIBLE);
            show_track_btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        AndroidUtils.viewFile(gpxTrack, Intent.ACTION_VIEW, ActivitySummaryDetail.this);
                    } catch (IOException e) {
                        GB.toast(getApplicationContext(), "Unable to display GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
                    }
                }
            });
        }
        String activitykindname = ActivityKind.asString(intent.getIntExtra("ActivityKind",0), getApplicationContext());
        Date starttime = (Date) intent.getSerializableExtra("StartTime");
        Date endtime = (Date) intent.getSerializableExtra("EndTime");
        String starttimeS = DateTimeUtils.formatDateTime(starttime);
        String endtimeS = DateTimeUtils.formatDateTime(endtime);
        String durationhms = DateTimeUtils.formatDurationHoursMinutes((endtime.getTime() - starttime.getTime()), TimeUnit.MILLISECONDS);

        ImageView activity_icon = (ImageView) findViewById(R.id.item_image);
        activity_icon.setImageResource(ActivityKind.getIconId(intent.getIntExtra("ActivityKind",0)));
        TextView activity_kind = (TextView) findViewById(R.id.activitykind);
        activity_kind.setText(activitykindname);
        TextView start_time = (TextView) findViewById(R.id.starttime);
        start_time.setText(starttimeS);
        TextView end_time = (TextView) findViewById(R.id.endtime);
        end_time.setText(endtimeS);
        TextView activity_duration = (TextView) findViewById(R.id.duration);
        activity_duration.setText(durationhms);

        JSONObject summaryData = null;
        String sumData = intent.getStringExtra("SummaryData");
        if (sumData != null) {
            try {
                summaryData = new JSONObject(sumData);
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }

        if (summaryData == null) return;

        JSONObject listOfSummaries = makeSummaryList(summaryData);
        makeSummaryContent(listOfSummaries);

        final JSONObject finalSummaryData = summaryData;
        activity_icon.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                show_raw_data=!show_raw_data;
                TableLayout fieldLayout = findViewById(R.id.summaryDetails);
                fieldLayout.removeAllViews();
                JSONObject listOfSummaries = makeSummaryList(finalSummaryData);
                makeSummaryContent(listOfSummaries);
                return false;
            }
        });

    }

    private void makeSummaryContent (JSONObject data){
        //build view, use localized names
        Iterator<String> keys = data.keys();
        DecimalFormat df = new DecimalFormat("#.##");

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                LOG.error("SportsActivity:" + key + ": " + data.get(key) + "\n");
                JSONArray innerList = (JSONArray) data.get(key);

                TableLayout fieldLayout = findViewById(R.id.summaryDetails);
                TableRow label_row = new TableRow(ActivitySummaryDetail.this);
                TextView label_field = new TextView(ActivitySummaryDetail.this);
                label_field.setTextSize(16);
                label_field.setTypeface(null, Typeface.BOLD);
                label_field.setText(String.format("%s", getStringResourceByName(key)));
                label_row.addView(label_field);
                fieldLayout.addView(label_row);

                for (int i = 0; i < innerList.length(); i++) {
                    JSONObject innerData = innerList.getJSONObject(i);
                    double value =  innerData.getDouble("value");
                    String unit =  innerData.getString("unit");
                    String name = innerData.getString("name");

                    if (!show_raw_data) {
                        //special casing here:
                        switch (unit) {
                            case "meters_second":
                                value = value * 3.6;
                                unit = "km_h";
                                break;
                            case "seconds_m":
                                value = 3.6 / value;
                                unit = "minutes_km";
                                break;
                            case "seconds_km":
                                value = value / 60;
                                unit = "minutes_km";
                                break;

                        }
                    }
                    TableRow field_row = new TableRow(ActivitySummaryDetail.this);
                    if (i % 2 == 0) field_row.setBackgroundColor(Color.rgb(237,237,237));

                    TextView name_field = new TextView(ActivitySummaryDetail.this);
                    TextView value_field = new TextView(ActivitySummaryDetail.this);
                    name_field.setGravity(Gravity.START);
                    value_field.setGravity(Gravity.END);

                    if (unit.equals("seconds") && !show_raw_data) { //rather then plain seconds, show formatted duration
                        value_field.setText(DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS));
                    }else {
                        value_field.setText(String.format("%s %s", df.format(value), getStringResourceByName(unit)));
                    }

                    name_field.setText(getStringResourceByName(name));
                    TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                    value_field.setLayoutParams(params);

                    field_row.addView(name_field);
                    field_row.addView(value_field);
                    fieldLayout.addView(field_row);
                }
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }
    }

    private JSONObject setGroups(){
        String groupDefinitions = "{'Strokes':['averageStrokeDistance','averageStrokesPerSecond','strokes'], " +
                "'Swimming':['swolfIndex','swimStyle'], " +
                "'Elevation':['ascentMeters','descentMeters','maxAltitude','minAltitude','ascentSeconds','descentSeconds','flatSeconds'], " +
                "'Speed':['maxSpeed','minPace','maxPace','averageKMPaceSeconds'], " +
                "'Activity':['distanceMeters','steps','activeSeconds','caloriesBurnt','totalStride'," +
                "'averageHR','averageStride'], " +
                "'Laps':['averageLapPace','laps']}";
        JSONObject data = null;
        try {
            data = new JSONObject(groupDefinitions);
        } catch (JSONException e) {
            LOG.error("SportsActivity", e);
        }
        return data;
    }

    private String getGroup(String searchItem) {
        String defaultGroup = "Activity";
        if (groupData == null) return defaultGroup;
        Iterator<String> keys = groupData.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONArray itemList = (JSONArray) groupData.get(key);
                for (int i = 0; i < itemList.length(); i++) {
                    if (itemList.getString(i).equals(searchItem)) {
                        return key;
                    }
                }
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }
    return defaultGroup;
}

    private JSONObject makeSummaryList(JSONObject summaryData){
        //make dictionary with data for each group
        JSONObject list = new JSONObject();
        Iterator<String> keys = summaryData.keys();
        LOG.error("SportsActivity JSON:" + summaryData + keys);

        while (keys.hasNext()) {
            String key = keys.next();

            try {
                LOG.error("SportsActivity:" + key + ": " + summaryData.get(key) + "\n");
                JSONObject innerData = (JSONObject) summaryData.get(key);
                Object value = innerData.get("value");
                String unit = innerData.getString("unit");
                String group = getGroup(key);

                if (!list.has(group)) {
                    list.put(group,new JSONArray());
                }

                JSONArray tmpl = (JSONArray) list.get(group);
                JSONObject innernew = new JSONObject();
                innernew.put("name", key);
                innernew.put("value", value);
                innernew.put("unit", unit);
                tmpl.put(innernew);
                list.put(group, tmpl);
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }
        return list;
    }

    private String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "string", packageName);
        if (resId==0){
            LOG.warn("SportsActivity " + "Missing string in strings:" + aString);
            return aString;
        }else{
            return getString(resId);
        }
    }

}
