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

public class HybridHRWatchfaceWidget {
    private String widgetType;
    private int posX;
    private int posY;

    public HybridHRWatchfaceWidget(String widgetType, int posX, int posY) {
        this.widgetType = widgetType;
        this.posX = posX;
        this.posY = posY;
    }

    public static LinkedHashMap<String, String> getAvailableWidgetTypes(Context context) {
        LinkedHashMap<String, String> widgetTypes = new LinkedHashMap<>();
        widgetTypes.put("widgetDate", "Date");
        widgetTypes.put("widgetWeather", "Weather");
        return widgetTypes;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public Bitmap getPreviewImage(Context context) throws IOException {
        return BitmapFactory.decodeStream(context.getAssets().open("fossil_hr/" + widgetType + "_preview.png"));
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }
}
