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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryItems;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.SwipeEvents;

public class ActivitySummaryDetail extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryDetail.class);
    private GBDevice gbDevice;

    private boolean show_raw_data = false;
    BaseActivitySummary currentItem = null;
    private int alternateColor;
    //private Object BottomSheetBehavior;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_summary_details);
        }

        Intent intent = getIntent();
        gbDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
        final int filter = intent.getIntExtra("filter", 0);
        final int position = intent.getIntExtra("position", 0);
        final long dateFromFilter = intent.getLongExtra("dateFromFilter", 0);
        final long dateToFilter = intent.getLongExtra("dateToFilter", 0);
        final String nameContainsFilter = intent.getStringExtra("nameContainsFilter");

        final ActivitySummaryItems items = new ActivitySummaryItems(this, gbDevice, filter, dateFromFilter, dateToFilter, nameContainsFilter);
        final LinearLayout layout = findViewById(R.id.activity_summary_detail_relative_layout);
        alternateColor = getAlternateColor(this);

        final Animation animFadeRight;
        final Animation animFadeLeft;
        final Animation animBounceLeft;
        final Animation animBounceRight;

        animFadeRight = AnimationUtils.loadAnimation(
                this,
                R.anim.flyright);
        animFadeLeft = AnimationUtils.loadAnimation(
                this,
                R.anim.flyleft);
        animBounceLeft = AnimationUtils.loadAnimation(
                this,
                R.anim.bounceleft);
        animBounceRight = AnimationUtils.loadAnimation(
                this,
                R.anim.bounceright);


        layout.setOnTouchListener(new SwipeEvents(this) {
            @Override
            public void onSwipeRight() {
                BaseActivitySummary newItem = items.getNextItem();
                if (newItem != null) {
                    currentItem = newItem;
                    makeSummaryHeader(newItem);
                    makeSummaryContent(newItem);
                    layout.startAnimation(animFadeRight);

                } else {
                    layout.startAnimation(animBounceRight);
                }
            }

            @Override
            public void onSwipeLeft() {
                BaseActivitySummary newItem = items.getPrevItem();
                if (newItem != null) {
                    currentItem = newItem;
                    makeSummaryHeader(newItem);
                    makeSummaryContent(newItem);
                    layout.startAnimation(animFadeLeft);
                } else {
                    layout.startAnimation(animBounceLeft);
                }
            }
        });


        currentItem = items.getItem(position);
        if (currentItem != null) {
            makeSummaryHeader(currentItem);
            makeSummaryContent(currentItem);
        }

        //allows long-press.switch of data being in raw form or recalculated
        ImageView activity_icon = findViewById(R.id.item_image);
        activity_icon.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                show_raw_data = !show_raw_data;
                if (currentItem != null) {
                    makeSummaryHeader(currentItem);
                    makeSummaryContent(currentItem);
                }
                return false;
            }
        });

        ImageView activity_summary_detail_edit_name_image = findViewById(R.id.activity_summary_detail_edit_name);
        activity_summary_detail_edit_name_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(ActivitySummaryDetail.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                String name = currentItem.getName();
                input.setText((name != null) ? name : "");
                FrameLayout container = new FrameLayout(ActivitySummaryDetail.this);
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                input.setLayoutParams(params);
                container.addView(input);

                new AlertDialog.Builder(ActivitySummaryDetail.this)
                        .setView(container)
                        .setCancelable(true)
                        .setTitle(ActivitySummaryDetail.this.getString(R.string.activity_summary_edit_name_title))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String name = input.getText().toString();
                                currentItem.setName(name);
                                currentItem.update();
                                makeSummaryHeader(currentItem);
                                makeSummaryContent(currentItem);

                            }
                        })
                        .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .show();
            }
        });


    }

    private void makeSummaryHeader(BaseActivitySummary item) {
        //make view of data from main part of item
        final String gpxTrack = item.getGpxTrack();
        Button show_track_btn = findViewById(R.id.showTrack);
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
        String activitykindname = ActivityKind.asString(item.getActivityKind(), getApplicationContext());
        String activityname = item.getName();
        Date starttime = item.getStartTime();
        Date endtime = item.getEndTime();
        String starttimeS = DateTimeUtils.formatDateTime(starttime);
        String endtimeS = DateTimeUtils.formatDateTime(endtime);
        String durationhms = DateTimeUtils.formatDurationHoursMinutes((endtime.getTime() - starttime.getTime()), TimeUnit.MILLISECONDS);

        ImageView activity_icon = findViewById(R.id.item_image);
        activity_icon.setImageResource(ActivityKind.getIconId(item.getActivityKind()));

        TextView activity_kind = findViewById(R.id.activitykind);
        activity_kind.setText(activitykindname);

        TextView activity_name = findViewById(R.id.activityname);
        activity_name.setText(activityname);

        if (activityname == null || (activityname != null && activityname.length() < 1)) {
            activity_name.setVisibility(View.GONE);
        } else {
            activity_name.setVisibility(View.VISIBLE);
        }

        TextView start_time = findViewById(R.id.starttime);
        start_time.setText(starttimeS);
        TextView end_time = findViewById(R.id.endtime);
        end_time.setText(endtimeS);
        TextView activity_duration = findViewById(R.id.duration);
        activity_duration.setText(durationhms);

    }

    private void makeSummaryContent(BaseActivitySummary item) {
        //make view of data from summaryData of item

        TableLayout fieldLayout = findViewById(R.id.summaryDetails);
        fieldLayout.removeAllViews(); //remove old widgets
        ActivitySummaryJsonSummary activitySummaryJsonSummary = new ActivitySummaryJsonSummary(item);
        //JSONObject summarySubdata = activitySummaryJsonSummary.getSummaryData();
        JSONObject data = activitySummaryJsonSummary.getSummaryGroupedList(); //get list, grouped by groups
        if (data == null) return;

        Iterator<String> keys = data.keys();
        DecimalFormat df = new DecimalFormat("#.##");

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                LOG.error("SportsActivity:" + key + ": " + data.get(key) + "\n");
                JSONArray innerList = (JSONArray) data.get(key);

                TableRow label_row = new TableRow(ActivitySummaryDetail.this);
                TextView label_field = new TextView(ActivitySummaryDetail.this);
                label_field.setTextSize(16);
                label_field.setTypeface(null, Typeface.BOLD);
                label_field.setText(String.format("%s", getStringResourceByName(key)));
                label_row.addView(label_field);
                fieldLayout.addView(label_row);

                for (int i = 0; i < innerList.length(); i++) {
                    TextView name_field = new TextView(ActivitySummaryDetail.this);
                    TextView value_field = new TextView(ActivitySummaryDetail.this);
                    name_field.setGravity(Gravity.START);
                    value_field.setGravity(Gravity.END);

                    JSONObject innerData = innerList.getJSONObject(i);
                    String unit = innerData.getString("unit");
                    String name = innerData.getString("name");
                    if (!unit.equals("string")) {
                        double value = innerData.getDouble("value");

                        if (!show_raw_data) {
                            //special casing here:
                            switch (unit) {
                                case "meters_second":
                                    value = value * 3.6;
                                    unit = "km_h";
                                    break;
                                case "seconds_m":
                                    value = value * (1000 / 60);
                                    unit = "minutes_km";
                                    break;
                                case "seconds_km":
                                    value = value / 60;
                                    unit = "minutes_km";
                                    break;

                            }
                        }

                        if (unit.equals("seconds") && !show_raw_data) { //rather then plain seconds, show formatted duration
                            value_field.setText(DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS));
                        } else {
                            value_field.setText(String.format("%s %s", df.format(value), getStringResourceByName(unit)));
                        }
                    } else {
                        value_field.setText(getStringResourceByName(innerData.getString("value"))); //we could optimize here a bit and only do this for particular activities (swim at the moment...)
                    }

                    TableRow field_row = new TableRow(ActivitySummaryDetail.this);
                    if (i % 2 == 0) field_row.setBackgroundColor(alternateColor);

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


    public static int getAlternateColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.alternate_row_background, typedValue, true);
        return typedValue.data;
    }

    private String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "string", packageName);
        if (resId == 0) {
            //LOG.warn("SportsActivity " + "Missing string in strings:" + aString);
            return aString;
        } else {
            return getString(resId);
        }
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


}
