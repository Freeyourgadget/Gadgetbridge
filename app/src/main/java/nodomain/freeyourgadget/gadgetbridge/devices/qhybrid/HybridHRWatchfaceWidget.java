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

import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;

import static nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil.invertBitmapColors;

public class HybridHRWatchfaceWidget implements Serializable {
    private String widgetType;
    private int posX;
    private int posY;
    private int width;
    private int height;
    private int color;
    private String background;
    private String extraConfigJSON;

    public static int COLOR_WHITE = 0;
    public static int COLOR_BLACK = 1;

    static HybridHRWidgetPosition[] defaultPositions = new HybridHRWidgetPosition[]{
        new HybridHRWidgetPosition(120, 58, R.string.watchface_dialog_widget_preset_top),
        new HybridHRWidgetPosition(182, 120, R.string.watchface_dialog_widget_preset_right),
        new HybridHRWidgetPosition(120, 182, R.string.watchface_dialog_widget_preset_bottom),
        new HybridHRWidgetPosition(58, 120, R.string.watchface_dialog_widget_preset_left),
    };

    public HybridHRWatchfaceWidget(String widgetType, int posX, int posY, int width, int height, int color, JSONObject extraConfig) {
        this.widgetType = widgetType;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.color = color;
        this.background = "";
        try {
            this.extraConfigJSON = extraConfig.toString();
        } catch (Exception e) {
            this.extraConfigJSON = "{}";
        }
    }

    public static LinkedHashMap<String, String> getAvailableWidgetTypes(Context context) {
        LinkedHashMap<String, String> widgetTypes = new LinkedHashMap<>();
        widgetTypes.put("widgetDate", context.getString(R.string.watchface_widget_type_date));
        widgetTypes.put("widgetWeather", context.getString(R.string.watchface_widget_type_weather));
        widgetTypes.put("widgetSteps", context.getString(R.string.watchface_widget_type_steps));
        widgetTypes.put("widgetHR", context.getString(R.string.watchface_widget_type_heart_rate));
        widgetTypes.put("widgetBattery", context.getString(R.string.watchface_widget_type_battery));
        widgetTypes.put("widgetCalories", context.getString(R.string.watchface_widget_type_calories));
        widgetTypes.put("widget2ndTZ", context.getString(R.string.watchface_widget_type_2nd_tz));
        widgetTypes.put("widgetActiveMins", context.getString(R.string.watchface_widget_type_active_mins));
        widgetTypes.put("widgetChanceOfRain", context.getString(R.string.watchface_widget_type_chance_rain));
        widgetTypes.put("widgetUV", context.getString(R.string.watchface_widget_type_uv_index));
        widgetTypes.put("widgetSpO2", context.getString(R.string.watchface_widget_type_sp02));
        widgetTypes.put("widgetCustom", context.getString(R.string.watchface_widget_type_custom));
        return widgetTypes;
    }

    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }
    public String getWidgetType() {
        return widgetType;
    }


    public Bitmap getPreviewImage(Context context) throws IOException {
        Bitmap preview = BitmapFactory.decodeStream(context.getAssets().open("fossil_hr/" + widgetType + "_preview.png"));
        if (getBackground() != "") {
            try {
                Bitmap background = BitmapFactory.decodeStream(context.getAssets().open("fossil_hr/" + getBackground() + ".png"));
                preview = BitmapUtil.overlay(background, preview);
            } catch (Exception e) {
                // continue silently without background
            }
        }
        if (color == COLOR_WHITE) {
            return preview;
        } else {
            return invertBitmapColors(preview);
        }
    }

    public int getPosX() {
        return posX;
    }
    public int getPosY() {
        return posY;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }
    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }
    public void setHeight(int height) {
        this.height = height;
    }

    public int getColor() {
        return color;
    }
    public void setColor(int color) {
        this.color = color;
    }

    public String getBackground() {
        return background;
    }
    public void setBackground(String background) {
        this.background = background;
    }

    public int getExtraConfigInt(String name, int fallback) {
        try {
            return new JSONObject(extraConfigJSON).optInt(name, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }
    public String getExtraConfigString(String name, String fallback) {
        try {
            return new JSONObject(extraConfigJSON).optString(name, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }
    public Boolean getExtraConfigBoolean(String name, Boolean fallback) {
        try {
            return new JSONObject(extraConfigJSON).optBoolean(name, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    public void setExtraConfigInt(String name, int value) {
        JSONObject extraConfig;
        try {
            extraConfig = new JSONObject(extraConfigJSON);
        } catch (Exception e) {
            extraConfig = new JSONObject();
        }
        try {
            extraConfig.put(name, value);
            extraConfigJSON = extraConfig.toString();
        } catch (Exception e) {
            return;
        }
    }
    public void setExtraConfigString(String name, String value) {
        JSONObject extraConfig;
        try {
            extraConfig = new JSONObject(extraConfigJSON);
        } catch (Exception e) {
            extraConfig = new JSONObject();
        }
        try {
            extraConfig.put(name, value);
            extraConfigJSON = extraConfig.toString();
        } catch (Exception e) {
            return;
        }
    }
    public void setExtraConfigBoolean(String name, Boolean value) {
        JSONObject extraConfig;
        try {
            extraConfig = new JSONObject(extraConfigJSON);
        } catch (Exception e) {
            extraConfig = new JSONObject();
        }
        try {
            extraConfig.put(name, value);
            extraConfigJSON = extraConfig.toString();
        } catch (Exception e) {
            return;
        }
    }
}
