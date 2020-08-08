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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
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

        ActivitySummary summary = (ActivitySummary) intent.getSerializableExtra("summary");

        final String gpxTrack = summary.getGpxTrack();
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

        LOG.debug("petr summary: " + summary + "," + summary.getSummaryData());
        String activitykind = ActivityKind.asString(summary.getActivityKind(), getApplicationContext());

        String starttime = DateTimeUtils.formatDateTime(summary.getStartTime());
        String endtime = DateTimeUtils.formatDateTime(summary.getEndTime());
        long startTs = summary.getStartTime().getTime() / 1000;
        long endTs = summary.getEndTime().getTime() / 1000;
        long durationms = (summary.getEndTime().getTime() - summary.getStartTime().getTime());
        String durationhms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(durationms),
                TimeUnit.MILLISECONDS.toMinutes(durationms) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(durationms) % TimeUnit.MINUTES.toSeconds(1));

        int steps = getSteps((int) startTs, (int) endTs);

        ImageView activity_icon = (ImageView) findViewById(R.id.item_image);
        activity_icon.setImageResource(ActivityKind.getIconId(summary.getActivityKind()));
        TextView activity_kind = (TextView) findViewById(R.id.activitykind);
        activity_kind.setText(activitykind);
        TextView start_time = (TextView) findViewById(R.id.starttime);
        start_time.setText(starttime);
        TextView end_time = (TextView) findViewById(R.id.endtime);
        end_time.setText(endtime);
        TextView activity_duration = (TextView) findViewById(R.id.duration);
        activity_duration.setText(durationhms);

        JSONObject summaryData = null;
        String sumData = summary.getSummaryData();
        if (sumData != null) {
            try {
                summaryData = new JSONObject(sumData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        StringBuilder summaryDatastr = new StringBuilder();

        if (summaryData == null) return;

        Iterator<String> keys = summaryData.keys();
        LOG.debug("petr summary JSON:" + summaryData + keys);

        while (keys.hasNext()) {
            String key = keys.next();
            LOG.debug("petr key:" + key);

            try {
                LOG.debug("petr" + key + ": " + summaryData.get(key) + "\n");
                DecimalFormat df = new DecimalFormat("#.##");
                JSONObject innerData = (JSONObject) summaryData.get(key);
                Object value = innerData.get("value");
                String unit = innerData.getString("unit");
                summaryDatastr.append(String.format("%s: %s %s\n", key, df.format(value), unit));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        TextView details = (TextView) findViewById(R.id.details);
        details.setText(summaryDatastr.toString());
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
