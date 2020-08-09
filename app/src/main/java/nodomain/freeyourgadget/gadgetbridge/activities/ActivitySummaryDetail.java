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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ActivitySummaryDetail extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryDetail.class);
    private GBDevice mGBDevice;

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
        long startTs = starttime.getTime() / 1000;
        long endTs = endtime.getTime() / 1000;
        String durationhms = DateTimeUtils.formatDurationHoursMinutes((endtime.getTime() - starttime.getTime()), TimeUnit.MILLISECONDS);
        //int steps = getSteps((int) startTs, (int) endTs);
        //unused now, as we use the more extensive summaryData

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
        TextView details = (TextView) findViewById(R.id.details);
        details.setText(makeSummaryContent(listOfSummaries));
    }

    private String makeSummaryContent (JSONObject data){
        //convert dictionary to pretty print string, use localized names
        StringBuilder content = new StringBuilder();
        Iterator<String> keys = data.keys();
        DecimalFormat df = new DecimalFormat("#.##");

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                LOG.error("SportsActivity:" + key + ": " + data.get(key) + "\n");
                JSONArray innerList = (JSONArray) data.get(key);
                content.append(String.format("\n%s\n", getStringResourceByName(key).toUpperCase()));

                for (int i = 0; i < innerList.length(); i++) {
                    JSONObject innerData = innerList.getJSONObject(i);
                    double value =  innerData.getDouble("value");
                    String unit =  innerData.getString("unit");
                    String name = innerData.getString("name");

                    //special casing here:
                    switch(unit){
                        case "meters_second":
                            value = value *3.6;
                            unit = "km_h";
                            break;
                        case "seconds_m":
                            value =  3.6/value;
                            unit = "minutes_km";
                            break;
                        case "seconds_km":
                            value =   value /60;
                            unit = "minutes_km";
                            break;
                    }

                    content.append(String.format("%s: %s %s\n", getStringResourceByName(name), df.format(value), getStringResourceByName(unit)));
                }
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }

        return content.toString();
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
                String group = innerData.getString("group");

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

    private int getSteps(int tsStart, int tsEnd) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            DailyTotals dt = new DailyTotals();
            ActivityAnalysis analysis = new ActivityAnalysis();
            ActivityAmounts amountsSteps;
            amountsSteps = analysis.calculateActivityAmounts(dt.getSamples(handler, mGBDevice, tsStart, tsEnd));
            return (int) dt.getTotalsStepsForActivityAmounts(amountsSteps);
        } catch (Exception e) {
            GB.toast("Error loading activity steps.", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        return 0;
    }
}
