/*  Copyright (C) 2021-2024 Arjan Schrijver, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;

public class HybridHRWatchfaceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(HybridHRWatchfaceFactory.class);
    private String watchfaceName;
    private HybridHRWatchfaceSettings settings;
    private Bitmap background;
    private Bitmap previewImage;
    private static final int PREVIEW_WIDTH = 192;
    private static final int PREVIEW_HEIGHT = 192;
    private ArrayList<JSONObject> widgets = new ArrayList<>();
    private JSONObject menuStructure = new JSONObject();

    public HybridHRWatchfaceFactory(String name) {
        watchfaceName = name.replaceAll("[^-A-Za-z0-9]", "");
        if (watchfaceName.equals("")) throw new AssertionError("name cannot be empty");
        if (watchfaceName.endsWith("App")) watchfaceName += "Watchface";
    }

    public void setSettings(HybridHRWatchfaceSettings settings) {
        this.settings = settings;
    }

    public void setBackground(Bitmap background) {
        if ((background.getWidth() == 240) && (background.getHeight() == 240)) {
            this.background = background;
        } else {
            this.background = Bitmap.createScaledBitmap(background, 240, 240, true);
        }
    }

    public void addWidget(HybridHRWatchfaceWidget widgetDesc) {
        JSONObject widget = new JSONObject();
        try {
            switch (widgetDesc.getWidgetType()) {
                case "widgetCustom":
                    if (widgetDesc.getExtraConfigInt("update_timeout", -1) >= 0) {
                        JSONObject data = new JSONObject();
                        data.put("update_timeout", widgetDesc.getExtraConfigInt("update_timeout", -1));
                        data.put("timeout_hide_text", widgetDesc.getExtraConfigBoolean("timeout_hide_text", true));
                        data.put("timeout_show_circle", widgetDesc.getExtraConfigBoolean("timeout_show_circle", true));
                        if (widgetDesc.getBackground() != "") {
                            data.put("background", widgetDesc.getBackground() + widgetDesc.getColor() + ".rle");
                        }
                        widget.put("data", data);
                    }
                    // fall through
                case "widget2ndTZ":
                    if (widgetDesc.getExtraConfigString("tzName", null) != null) {
                        JSONObject data = new JSONObject();
                        TimeZone tz = TimeZone.getTimeZone(widgetDesc.getExtraConfigString("tzName", null));
                        String tzShortName = widgetDesc.getExtraConfigString("tzName", null).replaceAll(".*/", "");
                        int tzOffsetMins = tz.getOffset(Calendar.getInstance().getTimeInMillis()) / 1000 / 60;
                        data.put("tzName", widgetDesc.getExtraConfigString("tzName", null));
                        data.put("loc", tzShortName);
                        data.put("utc", tzOffsetMins);
                        data.put("timeout_secs", widgetDesc.getExtraConfigInt("timeout_secs", 0));
                        widget.put("data", data);
                    }
                    // fall through
                case "widgetDate":
                case "widgetWeather":
                case "widgetSteps":
                case "widgetHR":
                case "widgetBattery":
                case "widgetCalories":
                case "widgetActiveMins":
                case "widgetChanceOfRain":
                case "widgetUV":
                case "widgetSpO2":
                    widget.put("type", "comp");
                    widget.put("name", widgetDesc.getWidgetType());
                    widget.put("goal_ring", false);
                    widget.put("color", widgetDesc.getColor() == HybridHRWatchfaceWidget.COLOR_WHITE ? "white" : "black");
                    if (!widgetDesc.getBackground().equals("")) {
                        widget.put("bg", widgetDesc.getBackground() + widgetDesc.getColor() + ".rle");
                    }
                    break;
                default:
                    LOG.warn("Invalid widget name: " + widgetDesc.getWidgetType());
                    return;
            }
            JSONObject size = new JSONObject();
            size.put("w", widgetDesc.getWidth());
            size.put("h", widgetDesc.getHeight());
            widget.put("size", size);
            JSONObject pos = new JSONObject();
            pos.put("x", widgetDesc.getPosX());
            pos.put("y", widgetDesc.getPosY());
            widget.put("pos", pos);
            widgets.add(widget);
        } catch (JSONException e) {
            LOG.warn("JSON error", e);
        }
    }

    public void setMenuStructure(JSONObject menuStructure){
        this.menuStructure = menuStructure;
    }

    public void addWidgets(ArrayList<HybridHRWatchfaceWidget> widgets) {
        for (HybridHRWatchfaceWidget widget : widgets) {
            addWidget(widget);
        }
    }

    private int includeWidget(String name) {
        int count = 0;
        for (JSONObject widget : this.widgets) {
            try {
                if (widget.get("name").equals(name)) {
                    count++;
                }
            } catch (JSONException e) {
            }
        }
        return count;
    }

    private Boolean includeBackground(String name, int color) {
        for (JSONObject widget : this.widgets) {
            try {
                if (widget.get("bg").toString().startsWith(name + color)) {
                    return true;
                }
            } catch (JSONException e) {
            }
        }
        return false;
    }

    private InputStream getWidgetBackgroundStream(Context context, String name, Boolean invert) throws IOException {
        Bitmap bgImage = BitmapFactory.decodeStream(context.getAssets().open("fossil_hr/" + name + ".png"));
        if (invert) {
            bgImage = BitmapUtil.invertBitmapColors(bgImage);
        }
        return new ByteArrayInputStream(ImageConverter.encodeToRLEImage(ImageConverter.get2BitsRLEImageBytes(bgImage), QHybridConstants.HYBRID_HR_WATCHFACE_WIDGET_SIZE, QHybridConstants.HYBRID_HR_WATCHFACE_WIDGET_SIZE));
    }

    public byte[] getWapp(Context context) throws IOException {
        byte[] backgroundBytes = ImageConverter.encodeToRawImage(ImageConverter.get2BitsRAWImageBytes(background));
        InputStream backgroundStream = new ByteArrayInputStream(backgroundBytes);
        byte[] previewBytes = ImageConverter.encodeToRLEImage(ImageConverter.get2BitsRLEImageBytes(Bitmap.createScaledBitmap(getPreviewImage(context), PREVIEW_WIDTH, PREVIEW_HEIGHT, true)), PREVIEW_HEIGHT, PREVIEW_WIDTH);
        InputStream previewStream = new ByteArrayInputStream(previewBytes);
        LinkedHashMap<String, InputStream> code = new LinkedHashMap<>();
        try {
            code.put(watchfaceName, context.getAssets().open("fossil_hr/openSourceWatchface.bin"));
            if (includeWidget("widgetDate") > 0) code.put("widgetDate", context.getAssets().open("fossil_hr/widgetDate.bin"));
            if (includeWidget("widgetWeather") > 0) code.put("widgetWeather", context.getAssets().open("fossil_hr/widgetWeather.bin"));
            if (includeWidget("widgetSteps") > 0) code.put("widgetSteps", context.getAssets().open("fossil_hr/widgetSteps.bin"));
            if (includeWidget("widgetHR") > 0) code.put("widgetHR", context.getAssets().open("fossil_hr/widgetHR.bin"));
            if (includeWidget("widgetBattery") > 0) code.put("widgetBattery", context.getAssets().open("fossil_hr/widgetBattery.bin"));
            if (includeWidget("widgetCalories") > 0) code.put("widgetCalories", context.getAssets().open("fossil_hr/widgetCalories.bin"));
            if (includeWidget("widgetActiveMins") > 0) code.put("widgetActiveMins", context.getAssets().open("fossil_hr/widgetActiveMins.bin"));
            if (includeWidget("widgetChanceOfRain") > 0) code.put("widgetChanceOfRain", context.getAssets().open("fossil_hr/widgetChanceOfRain.bin"));
            if (includeWidget("widgetUV") > 0) code.put("widgetUV", context.getAssets().open("fossil_hr/widgetUV.bin"));
            if (includeWidget("widgetSpO2") > 0) code.put("widgetSpO2", context.getAssets().open("fossil_hr/widgetSpO2.bin"));
            for (int i=0; i<includeWidget("widget2ndTZ"); i++) {
                code.put("widget2ndTZ" + i, context.getAssets().open("fossil_hr/widget2ndTZ.bin"));
            }
            for (int i=0; i<includeWidget("widgetCustom"); i++) {
                code.put("widgetCustom" + i, context.getAssets().open("fossil_hr/widgetCustom.bin"));
            }
        } catch (IOException e) {
            LOG.warn("Unable to read asset file", e);
        }
        LinkedHashMap<String, InputStream> icons = new LinkedHashMap<>();
        try {
            icons.put("background.raw", backgroundStream);
            icons.put("!preview.rle", previewStream);
            icons.put("icTrophy", context.getAssets().open("fossil_hr/icTrophy.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthClearDay", context.getAssets().open("fossil_hr/icWthClearDay.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthClearNite", context.getAssets().open("fossil_hr/icWthClearNite.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthCloudy", context.getAssets().open("fossil_hr/icWthCloudy.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthPartCloudyDay", context.getAssets().open("fossil_hr/icWthPartCloudyDay.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthPartCloudyNite", context.getAssets().open("fossil_hr/icWthPartCloudyNite.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthRainy", context.getAssets().open("fossil_hr/icWthRainy.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthSnowy", context.getAssets().open("fossil_hr/icWthSnowy.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthStormy", context.getAssets().open("fossil_hr/icWthStormy.rle"));
            if (includeWidget("widgetWeather") > 0) icons.put("icWthWindy", context.getAssets().open("fossil_hr/icWthWindy.rle"));
            if (includeWidget("widgetSteps") > 0) icons.put("icSteps", context.getAssets().open("fossil_hr/icSteps.rle"));
            if (includeWidget("widgetHR") > 0) icons.put("icHeart", context.getAssets().open("fossil_hr/icHeart.rle"));
            if (includeWidget("widgetBattery") > 0) icons.put("icBattCharging", context.getAssets().open("fossil_hr/icBattCharging.rle"));
            if (includeWidget("widgetBattery") > 0) icons.put("icBattEmpty", context.getAssets().open("fossil_hr/icBattEmpty.rle"));
            if (includeWidget("widgetBattery") > 0) icons.put("icBattery", context.getAssets().open("fossil_hr/icBattery.rle"));
            if (includeWidget("widgetCalories") > 0) icons.put("icCalories", context.getAssets().open("fossil_hr/icCalories.rle"));
            if (includeWidget("widgetActiveMins") > 0) icons.put("icActiveMins", context.getAssets().open("fossil_hr/icActiveMins.rle"));
            if (includeWidget("widgetChanceOfRain") > 0) icons.put("icRainChance", context.getAssets().open("fossil_hr/icRainChance.rle"));
            if (includeWidget("widgetSpO2") > 0) icons.put("icSpO2", context.getAssets().open("fossil_hr/icSpO2.rle"));
            if (includeWidget("widgetCustom") > 0) icons.put("widget_bg_error.rle", context.getAssets().open("fossil_hr/widget_bg_error.rle"));
            // Note: we have to check and invert every used widget background here,
            // because the watch doesn't invert the background image when the widget color is inverted
            if (includeBackground("widget_bg_thin_circle", HybridHRWatchfaceWidget.COLOR_WHITE)) {
                icons.put("widget_bg_thin_circle" + HybridHRWatchfaceWidget.COLOR_WHITE + ".rle", getWidgetBackgroundStream(context, "widget_bg_thin_circle", false));
            }
            if (includeBackground("widget_bg_thin_circle", HybridHRWatchfaceWidget.COLOR_BLACK)) {
                icons.put("widget_bg_thin_circle" + HybridHRWatchfaceWidget.COLOR_BLACK + ".rle", getWidgetBackgroundStream(context, "widget_bg_thin_circle", true));
            }
            if (includeBackground("widget_bg_double_circle", HybridHRWatchfaceWidget.COLOR_WHITE)) {
                icons.put("widget_bg_double_circle" + HybridHRWatchfaceWidget.COLOR_WHITE + ".rle", getWidgetBackgroundStream(context, "widget_bg_double_circle", false));
            }
            if (includeBackground("widget_bg_double_circle", HybridHRWatchfaceWidget.COLOR_BLACK)) {
                icons.put("widget_bg_double_circle" + HybridHRWatchfaceWidget.COLOR_BLACK + ".rle", getWidgetBackgroundStream(context, "widget_bg_double_circle", true));
            }
            if (includeBackground("widget_bg_dashed_circle", HybridHRWatchfaceWidget.COLOR_WHITE)) {
                icons.put("widget_bg_dashed_circle" + HybridHRWatchfaceWidget.COLOR_WHITE + ".rle", getWidgetBackgroundStream(context, "widget_bg_dashed_circle", false));
            }
            if (includeBackground("widget_bg_dashed_circle", HybridHRWatchfaceWidget.COLOR_BLACK)) {
                icons.put("widget_bg_dashed_circle" + HybridHRWatchfaceWidget.COLOR_BLACK + ".rle", getWidgetBackgroundStream(context, "widget_bg_dashed_circle", true));
            }
        } catch (IOException e) {
            LOG.warn("Unable to read asset file", e);
        }
        LinkedHashMap<String, InputStream> layout = new LinkedHashMap<>();
        layout.put("complication_layout", context.getAssets().open("fossil_hr/complication_layout.json"));
        layout.put("image_layout", context.getAssets().open("fossil_hr/image_layout.json"));
        layout.put("menu_layout", context.getAssets().open("fossil_hr/menu_layout.json"));

        if (includeWidget("widgetBattery") > 0) {
            layout.put("battery_layout", context.getAssets().open("fossil_hr/battery_layout.json"));
        }

        LinkedHashMap<String, String> displayName = new LinkedHashMap<>();
        displayName.put("display_name", watchfaceName);
        displayName.put("theme_class", "complications");
        LinkedHashMap<String, String> config = new LinkedHashMap<>();
        try {
            config.put("customWatchFace", getConfiguration());
        } catch (JSONException e) {
            LOG.warn("Could not generate configuration", e);
        }
        FossilAppWriter appWriter = new FossilAppWriter(context, QHybridConstants.HYBRIDHR_WATCHFACE_VERSION, code, icons, layout, displayName, config);
        return appWriter.getWapp();
    }

    private String getConfiguration() throws JSONException {
        JSONObject configuration = new JSONObject();

        JSONArray layout = new JSONArray();
        JSONObject background = new JSONObject();
        background.put("type", "image");
        background.put("name", "background.raw");
        JSONObject size = new JSONObject();
        size.put("w", 240);
        size.put("h", 240);
        background.put("size", size);
        JSONObject pos = new JSONObject();
        pos.put("x", 120);
        pos.put("y", 120);
        background.put("pos", pos);
        layout.put(background);
        int count_widget2ndTZ = 0;
        int count_widgetCustom = 0;
        for (JSONObject widget : widgets) {
            if (widget.get("name").equals("widget2ndTZ")) {
                widget.put("name", "widget2ndTZ" + count_widget2ndTZ);
                layout.put(widget);
                count_widget2ndTZ++;
            } else if (widget.get("name").equals("widgetCustom")) {
                widget.put("name", "widgetCustom" + count_widgetCustom);
                layout.put(widget);
                count_widgetCustom++;
            } else {
                layout.put(widget);
            }
        }
        configuration.put("layout", layout);

        JSONObject config = new JSONObject();
        config.put("timeout_display_full", settings.getDisplayTimeoutFull() * 60 * 1000);
        config.put("timeout_display_partial", settings.getDisplayTimeoutPartial() * 60 * 1000);
        config.put("wrist_flick_hands_relative", settings.isWristFlickHandsMoveRelative());
        config.put("wrist_flick_duration", settings.getWristFlickDuration());
        config.put("wrist_flick_move_hour", settings.getWristFlickMoveHour());
        config.put("wrist_flick_move_minute", settings.getWristFlickMoveMinute());
        config.put("toggle_widgets_event", settings.getToggleWidgetsEvent());
        config.put("toggle_backlight_event", settings.getToggleBacklightEvent());
        config.put("move_hands_event", settings.getMoveHandsEvent());
        config.put("powersave_display", settings.getPowersaveDisplay());
        config.put("powersave_hands", settings.getPowersaveHands());
        config.put("light_up_on_notification", settings.getLightUpOnNotification());
        configuration.put("config", config);

        configuration.put("menu_structure", menuStructure);

        return configuration.toString();
    }

    public static HybridHRWatchfaceWidget parseWidgetJSON(JSONObject widgetJSON) throws JSONException {
        HybridHRWatchfaceWidget parsedWidget = null;
        String widgetName = widgetJSON.getString("name");
        String widgetTimezone = null;
        int widgetUpdateTimeout = -1;
        boolean widgetTimeoutHideText = true;
        boolean widgetTimeoutShowCircle = true;
        switch (widgetName) {
            case "dateSSE":
                widgetName = "widgetDate";
                break;
            case "weatherSSE":
                widgetName = "widgetWeather";
                break;
            case "stepsSSE":
                widgetName = "widgetSteps";
                break;
            case "hrSSE":
                widgetName = "widgetHR";
                break;
            case "batterySSE":
                widgetName = "widgetBattery";
                break;
            case "caloriesSSE":
                widgetName = "widgetCalories";
                break;
            case "activeMinutesSSE":
                widgetName = "widgetActiveMins";
                break;
            case "chanceOfRainSSE":
                widgetName = "widgetChanceOfRain";
                break;
            case "timeZone2SSE":
                widgetName = "widget2ndTZ";
                break;
            case "spo2SSE":
                widgetName = "widgetSpO2";
                break;
        }
        if (widgetName.startsWith("widget2ndTZ")) {
            widgetName = "widget2ndTZ";
        } else if (widgetName.startsWith("widgetCustom")) {
            widgetName = "widgetCustom";
        }
        int widgetColor = widgetJSON.getString("color").equals("white") ? HybridHRWatchfaceWidget.COLOR_WHITE : HybridHRWatchfaceWidget.COLOR_BLACK;
        JSONObject widgetData = widgetJSON.optJSONObject("data");
        parsedWidget = new HybridHRWatchfaceWidget(widgetName,
                widgetJSON.getJSONObject("pos").getInt("x"),
                widgetJSON.getJSONObject("pos").getInt("y"),
                widgetJSON.getJSONObject("size").getInt("w"),
                widgetJSON.getJSONObject("size").getInt("h"),
                widgetColor,
                widgetData);
        String widgetBackground = widgetJSON.optString("bg", "");
        if (widgetBackground != "") {
            parsedWidget.setBackground(widgetBackground.replaceAll("[0-9]?\\.rle$", ""));
        }
        return parsedWidget;
    }

    public Bitmap getPreviewImage(Context context) {
        if (previewImage == null) {
            previewImage = BitmapUtil.getCircularBitmap(background);
            Canvas previewCanvas = new Canvas(previewImage);
            int widgetSize = QHybridConstants.HYBRID_HR_WATCHFACE_WIDGET_SIZE;
            float scaleFactor = previewImage.getWidth() / 240;
            for (int i=0; i<widgets.size(); i++) {
                try {
                    HybridHRWatchfaceWidget parsedWidget = parseWidgetJSON(widgets.get(i));
                    Bitmap widgetPreview = Bitmap.createScaledBitmap(parsedWidget.getPreviewImage(context), (int)(widgetSize * scaleFactor), (int)(widgetSize * scaleFactor), true);
                    int offsetFromCenter = (int)((widgetSize/2) * scaleFactor);
                    previewCanvas.drawBitmap(widgetPreview, parsedWidget.getPosX() - offsetFromCenter, parsedWidget.getPosY() - offsetFromCenter, null);
                } catch (JSONException e) {
                    LOG.warn("Couldn't parse widget JSON", e);
                } catch (IOException e) {
                    LOG.warn("Couldn't parse widget JSON", e);
                }
            }
        }
        return previewImage;
    }
}
