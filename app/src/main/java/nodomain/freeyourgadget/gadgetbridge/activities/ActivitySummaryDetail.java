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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryItems;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.SwipeEvents;

public class ActivitySummaryDetail extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryDetail.class);
    BaseActivitySummary currentItem = null;
    private GBDevice gbDevice;
    private boolean show_raw_data = false;
    private int alternateColor;
    private Menu mOptionsMenu;
    List<String> filesGpxList = new ArrayList<>();
    int selectedGpxIndex;
    String selectedGpxFile;
    File export_path = null;

    public static int getAlternateColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.alternate_row_background, typedValue, true);
        return typedValue.data;
    }

    public static Bitmap getScreenShot(View view, int height, int width, Context context) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(GBApplication.getWindowBackgroundColor(context));
        view.draw(canvas);
        return bitmap;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_summary_details);
        }


        Intent intent = getIntent();

        Bundle bundle = intent.getExtras();
        gbDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        final int position = bundle.getInt("position", 0);
        final int activityFilter = bundle.getInt("activityFilter", 0);
        final long dateFromFilter = bundle.getLong("dateFromFilter", 0);
        final long dateToFilter = bundle.getLong("dateToFilter", 0);
        final long deviceFilter = bundle.getLong("deviceFilter", 0);
        final String nameContainsFilter = bundle.getString("nameContainsFilter");
        final List itemsFilter = (List<Long>) bundle.getSerializable("itemsFilter");

        final ActivitySummaryItems items = new ActivitySummaryItems(this, gbDevice, activityFilter, dateFromFilter, dateToFilter, nameContainsFilter, deviceFilter, itemsFilter);
        final ScrollView layout = findViewById(R.id.activity_summary_detail_scroll_layout);
        //final LinearLayout layout = findViewById(R.id.activity_summary_detail_relative_layout);
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

        final ActivitySummariesChartFragment activitySummariesChartFragment = new ActivitySummariesChartFragment();
        final ActivitySummariesGpsFragment activitySummariesGpsFragment = new ActivitySummariesGpsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.chartsFragmentHolder, activitySummariesChartFragment)
                .replace(R.id.gpsFragmentHolder, activitySummariesGpsFragment)
                .commit();

        layout.setOnTouchListener(new SwipeEvents(this) {
            @Override
            public void onSwipeRight() {
                BaseActivitySummary newItem = items.getNextItem();
                if (newItem != null) {
                    currentItem = newItem;
                    makeSummaryHeader(newItem);
                    makeSummaryContent(newItem);
                    activitySummariesChartFragment.setDateAndGetData(getGBDevice(currentItem.getDevice()), currentItem.getStartTime().getTime() / 1000, currentItem.getEndTime().getTime() / 1000);
                    if (get_gpx_file() != null) {
                        showCanvas();
                        activitySummariesGpsFragment.set_data(get_gpx_file());
                    } else {
                        hideCanvas();
                    }

                    layout.startAnimation(animFadeRight);
                    show_hide_gpx_menu();
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
                    activitySummariesChartFragment.setDateAndGetData(getGBDevice(currentItem.getDevice()), currentItem.getStartTime().getTime() / 1000, currentItem.getEndTime().getTime() / 1000);
                    if (get_gpx_file() != null) {
                        showCanvas();
                        activitySummariesGpsFragment.set_data(get_gpx_file());
                    } else {
                        hideCanvas();
                    }


                    layout.startAnimation(animFadeLeft);
                    show_hide_gpx_menu();
                } else {
                    layout.startAnimation(animBounceLeft);
                }
            }
        });

        currentItem = items.getItem(position);
        if (currentItem != null) {
            makeSummaryHeader(currentItem);
            makeSummaryContent(currentItem);
            activitySummariesChartFragment.setDateAndGetData(getGBDevice(currentItem.getDevice()), currentItem.getStartTime().getTime() / 1000, currentItem.getEndTime().getTime() / 1000);
            if (get_gpx_file() != null) {
                showCanvas();
                activitySummariesGpsFragment.set_data(get_gpx_file());
            } else {
                hideCanvas();
            }
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
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
                                if (name.length() < 1) name = null;
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
        ImageView activity_summary_detail_edit_gps = findViewById(R.id.activity_summary_detail_edit_gps);
        activity_summary_detail_edit_gps.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                export_path = get_path();
                filesGpxList = get_gpx_file_list();

                AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySummaryDetail.this);
                builder.setTitle(R.string.activity_summary_detail_select_gpx_track);
                ArrayAdapter<String> directory_listing = new ArrayAdapter<String>(ActivitySummaryDetail.this, android.R.layout.simple_list_item_1, filesGpxList);
                builder.setSingleChoiceItems(directory_listing, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedGpxIndex = which;
                        selectedGpxFile = export_path + "/" + filesGpxList.get(selectedGpxIndex);
                        String message = String.format("%s %s?", getString(R.string.set), filesGpxList.get(selectedGpxIndex));
                        if (selectedGpxIndex == 0) {
                            selectedGpxFile = null;
                            message = String.format("%s?", getString(R.string.activity_summary_detail_clear_gpx_track));
                        }

                        new AlertDialog.Builder(ActivitySummaryDetail.this)
                                .setCancelable(true)
                                .setIcon(R.drawable.ic_warning)
                                .setTitle(R.string.activity_summary_detail_editing_gpx_track)
                                .setMessage(message)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        currentItem.setGpxTrack(selectedGpxFile);
                                        currentItem.update();
                                        if (get_gpx_file() != null) {
                                            showCanvas();
                                            activitySummariesGpsFragment.set_data(get_gpx_file());
                                        } else {
                                            hideCanvas();
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void makeSummaryHeader(BaseActivitySummary item) {
        //make view of data from main part of item
        String activitykindname = ActivityKind.asString(item.getActivityKind(), getApplicationContext());
        String activityname = item.getName();
        Date starttime = item.getStartTime();
        Date endtime = item.getEndTime();
        String starttimeS = String.format("%s, %s", DateTimeUtils.formatDate(starttime), DateTimeUtils.formatTime(starttime.getHours(), starttime.getMinutes()));
        String endtimeS = String.format("%s, %s", DateTimeUtils.formatDate(endtime), DateTimeUtils.formatTime(endtime.getHours(), endtime.getMinutes()));
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

    private File get_path() {
        File path = null;
        try {
            path = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            LOG.error("Error getting path", e);
        }
        return path;
    }

    private List<String> get_gpx_file_list() {
        List<String> list = new ArrayList<>();

        File[] fileListing = export_path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getPath().toLowerCase().endsWith(".gpx");
            }
        });

        if (fileListing != null && fileListing.length > 1) {
            Arrays.sort(fileListing, new Comparator<File>() {
                @Override
                public int compare(File fileA, File fileB) {
                    if (fileA.lastModified() < fileB.lastModified()) {
                        return 1;
                    }
                    if (fileA.lastModified() > fileB.lastModified()) {
                        return -1;
                    }
                    return 0;
                }
            });
        }

        list.add(getString(R.string.activity_summary_detail_clear_gpx_track));

        for (File file : fileListing) {
            list.add(file.getName());
        }
        return list;
    }

    private void makeSummaryContent(BaseActivitySummary item) {
        //make view of data from summaryData of item
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        String UNIT_IMPERIAL = GBApplication.getContext().getString(R.string.p_unit_imperial);

        TableLayout fieldLayout = findViewById(R.id.summaryDetails);
        fieldLayout.removeAllViews(); //remove old widgets
        ActivitySummaryJsonSummary activitySummaryJsonSummary = new ActivitySummaryJsonSummary(item);
        JSONObject data = activitySummaryJsonSummary.getSummaryGroupedList(); //get list, grouped by groups
        if (data == null) return;

        Iterator<String> keys = data.keys();
        DecimalFormat df = new DecimalFormat("#.##");

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONArray innerList = (JSONArray) data.get(key);

                TableRow label_row = new TableRow(ActivitySummaryDetail.this);
                TextView label_field = new TextView(ActivitySummaryDetail.this);
                label_field.setTextSize(16);
                label_field.setPadding(0, 10, 0, 0);
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
                            //special casing here + imperial units handling
                            switch (unit) {
                                case "cm":
                                    if (units.equals(UNIT_IMPERIAL)) {
                                        value = value * 0.0328084;
                                        unit = "ft";
                                    }
                                    break;
                                case "meters_second":
                                    if (units.equals(UNIT_IMPERIAL)) {
                                        value = value * 2.236936D;
                                        unit = "mi_h";
                                    } else { //metric
                                        value = value * 3.6;
                                        unit = "km_h";
                                    }
                                    break;
                                case "seconds_m":
                                    if (units.equals(UNIT_IMPERIAL)) {
                                        value = value * (1609.344 / 60D);
                                        unit = "minutes_mi";
                                    } else { //metric
                                        value = value * (1000 / 60D);
                                        unit = "minutes_km";
                                    }
                                    break;
                                case "seconds_km":
                                    if (units.equals(UNIT_IMPERIAL)) {
                                        value = value / 60D * 1.609344;
                                        unit = "minutes_mi";
                                    } else { //metric
                                        value = value / 60D;
                                        unit = "minutes_km";
                                    }
                                    break;
                                case "meters":
                                    if (units.equals(UNIT_IMPERIAL)) {
                                        value = value * 3.28084D;
                                        unit = "ft";
                                        if (value > 6000) {
                                            value = value * 0.0001893939D;
                                            unit = "mi";
                                        }
                                    } else { //metric
                                        if (value > 2000) {
                                            value = value / 1000;
                                            unit = "km";
                                        }
                                    }
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
            case R.id.activity_action_take_screenshot:
                take_share_screenshot(ActivitySummaryDetail.this);
                return true;
            case R.id.activity_action_share_gpx:
                share_gpx_track(ActivitySummaryDetail.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void take_share_screenshot(Context context) {
        final ScrollView layout = findViewById(R.id.activity_summary_detail_scroll_layout);
        int width = layout.getChildAt(0).getHeight();
        int height = layout.getChildAt(0).getWidth();
        Bitmap screenShot = getScreenShot(layout, width, height, context);

        String fileName = FileUtils.makeValidFileName("Screenshot-" + ActivityKind.asString(currentItem.getActivityKind(), context).toLowerCase() + "-" + DateTimeUtils.formatIso8601(currentItem.getStartTime()) + ".png");
        try {
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            shareScreenshot(targetFile, context);
            GB.toast(getApplicationContext(), "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting screenshot", e);
        }
    }

    private void shareScreenshot(File targetFile, Context context) {
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", targetFile);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        String shareBody = "Sports Activity";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sports Activity");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        try {
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_no_app_for_png, Toast.LENGTH_LONG).show();
        }
    }

    private void share_gpx_track(Context context) {
        final String gpxTrack = currentItem.getGpxTrack();

        if (gpxTrack != null) {
            try {
                AndroidUtils.viewFile(gpxTrack, Intent.ACTION_VIEW, context);
            } catch (IOException e) {
                GB.toast(getApplicationContext(), "Unable to display GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }
        } else {
            GB.toast(getApplicationContext(), "No GPX track in this activity", Toast.LENGTH_LONG, GB.INFO);
        }
    }

    private void show_hide_gpx_menu() {
        String gpxTrack = null;
        if (currentItem != null) {
            gpxTrack = currentItem.getGpxTrack();
        }
        if (gpxTrack == null) {
            mOptionsMenu.findItem(R.id.activity_detail_overflowMenu).getSubMenu().findItem(R.id.activity_action_share_gpx).setVisible(false);
        } else {
            mOptionsMenu.findItem(R.id.activity_detail_overflowMenu).getSubMenu().findItem(R.id.activity_action_share_gpx).setVisible(true);
        }
    }

    private void showCanvas() {
        View gpsView = findViewById(R.id.gpsFragmentHolder);
        ViewGroup.LayoutParams params = gpsView.getLayoutParams();
        params.height = (int) (300 * getApplicationContext().getResources().getDisplayMetrics().density);
        gpsView.setLayoutParams(params);
    }

    private void hideCanvas() {
        View gpsView = findViewById(R.id.gpsFragmentHolder);
        ViewGroup.LayoutParams params = gpsView.getLayoutParams();
        params.height = 0;
        gpsView.setLayoutParams(params);
    }

    private File get_gpx_file() {
        final String gpxTrack = currentItem.getGpxTrack();
        if (gpxTrack != null) {
            File file = new File(gpxTrack);
            if (file.exists()) {
                return file;
            } else {
                return null;
            }
        }
        return null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.activity_take_screenshot_menu, menu);
        show_hide_gpx_menu();
        return true;
    }

    private GBDevice getGBDevice(Device findDevice) {
        DaoSession daoSession;
        GBApplication gbApp = (GBApplication) getApplicationContext();
        List<? extends GBDevice> devices = gbApp.getDeviceManager().getDevices();

        try (DBHandler handler = GBApplication.acquireDB()) {
            daoSession = handler.getDaoSession();
            for (GBDevice device : devices) {
                Device dbDevice = DBHelper.findDevice(device, daoSession);
                if (dbDevice.equals(findDevice)) return device;
            }
        } catch (Exception e) {
            LOG.debug("Error getting device: " + e);
        }
        return null;
    }

}
