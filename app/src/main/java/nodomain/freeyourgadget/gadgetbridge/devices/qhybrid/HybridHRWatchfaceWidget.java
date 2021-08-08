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
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.util.LinkedHashMap;

import nodomain.freeyourgadget.gadgetbridge.R;

import static nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil.invertBitmapColors;

public class HybridHRWatchfaceWidget {
    private String widgetType;
    private int posX;
    private int posY;
    private int color = 0;
    private String timezone;

    public static int COLOR_WHITE = 0;
    public static int COLOR_BLACK = 1;

    public HybridHRWatchfaceWidget(String widgetType, int posX, int posY, int color) {
        this.widgetType = widgetType;
        this.posX = posX;
        this.posY = posY;
        this.color = color;
    }
    public HybridHRWatchfaceWidget(String widgetType, int posX, int posY, int color, String timezone) {
        this(widgetType, posX, posY, color);
        this.timezone = timezone;
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
//        widgetTypes.put("widgetChanceOfRain", context.getString(R.string.watchface_widget_type_chance_rain));  // Disabled due to missing support in Gadgetbridge
        return widgetTypes;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public Bitmap getPreviewImage(Context context) throws IOException {
        Bitmap preview = BitmapFactory.decodeStream(context.getAssets().open("fossil_hr/" + widgetType + "_preview.png"));
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

    public int getColor() {
        return color;
    }

    public String getTimezone() {
        return timezone;
    }
}
