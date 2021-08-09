/*  Copyright (C) 2021 Arjan Schrijver

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

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter;

public class HybridHRWatchfaceFactory {
    private final Logger LOG = LoggerFactory.getLogger(HybridHRWatchfaceFactory.class);
    private String watchfaceName;
    private HybridHRWatchfaceSettings settings;
    private Bitmap background;
    private ArrayList<JSONObject> widgets = new ArrayList<>();

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
                case "widgetDate":
                case "widgetWeather":
                case "widgetSteps":
                case "widgetHR":
                case "widgetBattery":
                case "widgetCalories":
                case "widgetActiveMins":
                case "widgetChanceOfRain":
                    widget.put("type", "comp");
                    widget.put("name", widgetDesc.getWidgetType());
                    widget.put("goal_ring", false);
                    widget.put("color", widgetDesc.getColor() == HybridHRWatchfaceWidget.COLOR_WHITE ? "white" : "black");
                    break;
                case "widget2ndTZ":
                    widget.put("type", "comp");
                    widget.put("name", widgetDesc.getWidgetType());
                    widget.put("goal_ring", false);
                    widget.put("color", widgetDesc.getColor() == HybridHRWatchfaceWidget.COLOR_WHITE ? "white" : "black");
                    if (widgetDesc.getTimezone() != null) {
                        JSONObject data = new JSONObject();
                        TimeZone tz = TimeZone.getTimeZone(widgetDesc.getTimezone());
                        String tzShortName = widgetDesc.getTimezone().replaceAll(".*/", "");
                        int tzOffsetMins = tz.getRawOffset() / 1000 / 60;
                        data.put("tzName", widgetDesc.getTimezone());
                        data.put("loc", tzShortName);
                        data.put("utc", tzOffsetMins);
                        widget.put("data", data);
                    }
                    break;
                default:
                    LOG.warn("Invalid widget name: " + widgetDesc.getWidgetType());
                    return;
            }
            JSONObject size = new JSONObject();
            size.put("w", 76);
            size.put("h", 76);
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

    public void addWidgets(ArrayList<HybridHRWatchfaceWidget> widgets) {
        for (HybridHRWatchfaceWidget widget : widgets) {
            addWidget(widget);
        }
    }

    private boolean includeWidget(String name) {
        for (JSONObject widget : this.widgets) {
            try {
                if (widget.get("name").equals(name)) {
                    return true;
                }
            } catch (JSONException e) {
            }
        }
        return false;
    }

    public byte[] getWapp(Context context) throws IOException {
        byte[] backgroundBytes = ImageConverter.encodeToRawImage(ImageConverter.get2BitsRAWImageBytes(background));
        InputStream backgroundStream = new ByteArrayInputStream(backgroundBytes);
        LinkedHashMap<String, InputStream> code = new LinkedHashMap<>();
        try {
            code.put(watchfaceName, context.getAssets().open("fossil_hr/openSourceWatchface.bin"));
            if (includeWidget("widgetDate")) code.put("widgetDate", context.getAssets().open("fossil_hr/widgetDate.bin"));
            if (includeWidget("widgetWeather")) code.put("widgetWeather", context.getAssets().open("fossil_hr/widgetWeather.bin"));
            if (includeWidget("widgetSteps")) code.put("widgetSteps", context.getAssets().open("fossil_hr/widgetSteps.bin"));
            if (includeWidget("widgetHR")) code.put("widgetHR", context.getAssets().open("fossil_hr/widgetHR.bin"));
            if (includeWidget("widgetBattery")) code.put("widgetBattery", context.getAssets().open("fossil_hr/widgetBattery.bin"));
            if (includeWidget("widgetCalories")) code.put("widgetCalories", context.getAssets().open("fossil_hr/widgetCalories.bin"));
            if (includeWidget("widgetActiveMins")) code.put("widgetActiveMins", context.getAssets().open("fossil_hr/widgetActiveMins.bin"));
            if (includeWidget("widgetChanceOfRain")) code.put("widgetChanceOfRain", context.getAssets().open("fossil_hr/widgetChanceOfRain.bin"));
            if (includeWidget("widget2ndTZ")) code.put("widget2ndTZ", context.getAssets().open("fossil_hr/widget2ndTZ.bin"));
        } catch (IOException e) {
            LOG.warn("Unable to read asset file", e);
        }
        LinkedHashMap<String, InputStream> icons = new LinkedHashMap<>();
        try {
            icons.put("background.raw", backgroundStream);
            icons.put("icTrophy", context.getAssets().open("fossil_hr/icTrophy.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthClearDay", context.getAssets().open("fossil_hr/icWthClearDay.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthClearNite", context.getAssets().open("fossil_hr/icWthClearNite.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthCloudy", context.getAssets().open("fossil_hr/icWthCloudy.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthPartCloudyDay", context.getAssets().open("fossil_hr/icWthPartCloudyDay.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthPartCloudyNite", context.getAssets().open("fossil_hr/icWthPartCloudyNite.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthRainy", context.getAssets().open("fossil_hr/icWthRainy.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthSnowy", context.getAssets().open("fossil_hr/icWthSnowy.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthStormy", context.getAssets().open("fossil_hr/icWthStormy.rle"));
            if (includeWidget("widgetWeather")) icons.put("icWthWindy", context.getAssets().open("fossil_hr/icWthWindy.rle"));
            if (includeWidget("widgetSteps")) icons.put("icSteps", context.getAssets().open("fossil_hr/icSteps.rle"));
            if (includeWidget("widgetHR")) icons.put("icHeart", context.getAssets().open("fossil_hr/icHeart.rle"));
            if (includeWidget("widgetBattery")) icons.put("icBattCharging", context.getAssets().open("fossil_hr/icBattCharging.rle"));
            if (includeWidget("widgetBattery")) icons.put("icBattEmpty", context.getAssets().open("fossil_hr/icBattEmpty.rle"));
            if (includeWidget("widgetBattery")) icons.put("icBattery", context.getAssets().open("fossil_hr/icBattery.rle"));
            if (includeWidget("widgetCalories")) icons.put("icCalories", context.getAssets().open("fossil_hr/icCalories.rle"));
            if (includeWidget("widgetActiveMins")) icons.put("icActiveMins", context.getAssets().open("fossil_hr/icActiveMins.rle"));
            if (includeWidget("widgetChanceOfRain")) icons.put("icRainChance", context.getAssets().open("fossil_hr/icRainChance.rle"));
        } catch (IOException e) {
            LOG.warn("Unable to read asset file", e);
        }
        LinkedHashMap<String, String> layout = new LinkedHashMap<>();
        try {
            layout.put("complication_layout", getComplicationLayout());
        } catch (JSONException e) {
            LOG.warn("Could not generate complication_layout", e);
        }
        try {
            layout.put("image_layout", getImageLayout());
        } catch (JSONException e) {
            LOG.warn("Could not generate image_layout", e);
        }
        try {
            if (includeWidget("widgetBattery")) layout.put("battery_layout", getBatteryLayout());
        } catch (JSONException e) {
            LOG.warn("Could not generate battery_layout", e);
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
        FossilAppWriter appWriter = new FossilAppWriter(context, "1.2.0.0", code, icons, layout, displayName, config);
        return appWriter.getWapp();
    }

    private String getBatteryLayout() throws JSONException {
        JSONArray batteryLayout = new JSONArray();

        JSONObject complicationBackground = new JSONObject();
        complicationBackground.put("id", 0);
        complicationBackground.put("type", "complication_background");
        complicationBackground.put("background", "#background");
        complicationBackground.put("visible", true);
        complicationBackground.put("inversion", false);
        JSONObject goalRing = new JSONObject();
        goalRing.put("is_enable", "#goal_ring");
        goalRing.put("end_angle", "#fi");
        goalRing.put("is_invert", "#$e");
        complicationBackground.put("goal_ring", goalRing);
        JSONObject dimension = new JSONObject();
        dimension.put("type", "rigid");
        dimension.put("width", "#size.w");
        dimension.put("height", "#size.h");
        complicationBackground.put("dimension", dimension);
        JSONObject placement = new JSONObject();
        placement.put("type", "absolute");
        placement.put("left", "#pos.Ue");
        placement.put("top", "#pos.Qe");
        complicationBackground.put("placement", placement);
        batteryLayout.put(complicationBackground);

        JSONObject complicationContent = new JSONObject();
        complicationContent.put("id", 1);
        complicationContent.put("parent_id", 0);
        complicationContent.put("type", "complication_content");
        complicationContent.put("icon", "icBattery");
        complicationContent.put("text_low", "#ci");
        complicationContent.put("visible", true);
        complicationContent.put("inversion", "#$e");
        dimension = new JSONObject();
        dimension.put("type", "rigid");
        dimension.put("width", 76);
        dimension.put("height", 76);
        complicationContent.put("dimension", dimension);
        placement = new JSONObject();
        placement.put("type", "relative");
        complicationContent.put("placement", placement);
        batteryLayout.put(complicationContent);

        JSONObject chargingStatus = new JSONObject();
        chargingStatus.put("id", 2);
        chargingStatus.put("parent_id", 1);
        chargingStatus.put("type", "solid");
        chargingStatus.put("color", "#nt");
        chargingStatus.put("visible", true);
        chargingStatus.put("inversion", false);
        dimension = new JSONObject();
        dimension.put("type", "rigid");
        dimension.put("width", "#it");
        dimension.put("height", 6);
        chargingStatus.put("dimension", dimension);
        placement = new JSONObject();
        placement.put("type", "absolute");
        placement.put("left", 29);
        placement.put("top", 23);
        chargingStatus.put("placement", placement);
        batteryLayout.put(chargingStatus);

        JSONObject image = new JSONObject();
        image.put("id", 3);
        image.put("parent_id", 1);
        image.put("type", "image");
        image.put("image_name", "icBattCharging");
        image.put("draw_mode", 1);
        image.put("visible", "#et");
        image.put("inversion", false);
        placement = new JSONObject();
        placement.put("type", "absolute");
        placement.put("left", 34);
        placement.put("top", 21);
        image.put("placement", placement);
        dimension = new JSONObject();
        dimension.put("width", 6);
        dimension.put("height", 9);
        image.put("dimension", dimension);
        batteryLayout.put(image);

        return batteryLayout.toString();
    }

    private String getComplicationLayout() throws JSONException {
        JSONArray complicationLayout = new JSONArray();

        JSONObject complicationBackground = new JSONObject();
        complicationBackground.put("id", 0);
        complicationBackground.put("type", "complication_background");
        complicationBackground.put("background", "#background");
        complicationBackground.put("visible", true);
        complicationBackground.put("inversion", false);
        JSONObject goalRing = new JSONObject();
        goalRing.put("is_enable", "#goal_ring");
        goalRing.put("end_angle", "#fi");
        goalRing.put("is_invert", "#$e");
        complicationBackground.put("goal_ring", goalRing);
        JSONObject dimension = new JSONObject();
        dimension.put("type", "rigid");
        dimension.put("width", "#size.w");
        dimension.put("height", "#size.h");
        complicationBackground.put("dimension", dimension);
        JSONObject placement = new JSONObject();
        placement.put("type", "absolute");
        placement.put("left", "#pos.Ue");
        placement.put("top", "#pos.Qe");
        complicationBackground.put("placement", placement);
        complicationLayout.put(complicationBackground);

        JSONObject complicationContent = new JSONObject();
        complicationContent.put("id", 1);
        complicationContent.put("parent_id", 0);
        complicationContent.put("type", "complication_content");
        complicationContent.put("icon", "#icon");
        complicationContent.put("text_high", "#dt");
        complicationContent.put("text_low", "#ci");
        complicationContent.put("visible", true);
        complicationContent.put("inversion", "#$e");
        dimension = new JSONObject();
        dimension.put("type", "rigid");
        dimension.put("width", 76);
        dimension.put("height", 76);
        complicationContent.put("dimension", dimension);
        placement = new JSONObject();
        placement.put("type", "relative");
        complicationContent.put("placement", placement);
        complicationLayout.put(complicationContent);

        return complicationLayout.toString();
    }

    private String getImageLayout() throws JSONException {
        JSONArray imageLayout = new JSONArray();

        JSONObject container = new JSONObject();
        container.put("id", 0);
        container.put("type", "container");
        container.put("direction", 1);
        container.put("main_alignment", 1);
        container.put("cross_alignment", 1);
        container.put("visible", true);
        container.put("inversion", false);
        JSONObject dimension = new JSONObject();
        dimension.put("type", "rigid");
        dimension.put("width", 240);
        dimension.put("height", 240);
        container.put("dimension", dimension);
        JSONObject placement = new JSONObject();
        placement.put("type", "absolute");
        placement.put("left", 0);
        placement.put("top", 0);
        container.put("placement", placement);
        imageLayout.put(container);

        JSONObject image = new JSONObject();
        image.put("id", 1);
        image.put("parent_id", 0);
        image.put("type", "image");
        image.put("image_name", "#name");
        image.put("draw_mode", 1);
        image.put("visible", true);
        image.put("inversion", false);
        placement = new JSONObject();
        placement.put("type", "absolute");
        placement.put("left", "#pos.Ue");
        placement.put("top", "#pos.Qe");
        image.put("placement", placement);
        dimension = new JSONObject();
        dimension.put("width", "#size.w");
        dimension.put("height", "#size.h");
        image.put("dimension", dimension);
        imageLayout.put(image);

        return imageLayout.toString();
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
        for (JSONObject widget : widgets) {
            layout.put(widget);
        }
        configuration.put("layout", layout);

        JSONObject config = new JSONObject();
        config.put("timeout_display_full", settings.getDisplayTimeoutFull() * 60 * 1000);
        config.put("timeout_display_partial", settings.getDisplayTimeoutPartial() * 60 * 1000);
        config.put("wrist_flick_hands_relative", settings.isWristFlickHandsMoveRelative());
        config.put("wrist_flick_duration", settings.getWristFlickDuration());
        config.put("wrist_flick_move_hour", settings.getWristFlickMoveHour());
        config.put("wrist_flick_move_minute", settings.getWristFlickMoveMinute());
        config.put("powersave_display", settings.getPowersaveDisplay());
        config.put("powersave_hands", settings.getPowersaveHands());
        configuration.put("config", config);

        return configuration.toString();
    }
}
