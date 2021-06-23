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

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter;

public class HybridHRWatchfaceFactory {
    private final Logger LOG = LoggerFactory.getLogger(HybridHRWatchfaceFactory.class);
    private String watchfaceName;
    private Bitmap background;
    private ArrayList<JSONObject> widgets = new ArrayList<>();

    public HybridHRWatchfaceFactory(String name) {
        watchfaceName = name.replaceAll("[^-A-Za-z0-9]", "");
        if (watchfaceName.equals("")) throw new AssertionError("name cannot be empty");
        if (watchfaceName.endsWith("App")) watchfaceName += "Watchface";
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
                    widget.put("type", "comp");
                    widget.put("name", widgetDesc.getWidgetType());
                    widget.put("goal_ring", false);
                    widget.put("color", "white");
                    widget.put("bg", "_00.rle");
                    break;
                case "widgetWeather":
                    widget.put("type", "comp");
                    widget.put("name", widgetDesc.getWidgetType());
                    widget.put("goal_ring", false);
                    widget.put("color", "white");
                    widget.put("bg", "_01.rle");
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

    public byte[] getWapp(Context context) throws IOException {
        byte[] backgroundBytes = ImageConverter.encodeToRawImage(ImageConverter.get2BitsRAWImageBytes(background));
        InputStream backgroundStream = new ByteArrayInputStream(backgroundBytes);
        LinkedHashMap<String, InputStream> code = new LinkedHashMap<>();
        try {
            code.put(watchfaceName, context.getAssets().open("fossil_hr/openSourceWatchface.bin"));
            code.put("widgetDate", context.getAssets().open("fossil_hr/widgetDate.bin"));
            code.put("widgetWeather", context.getAssets().open("fossil_hr/widgetWeather.bin"));
        } catch (IOException e) {
            LOG.warn("Unable to read asset file", e);
        }
        LinkedHashMap<String, InputStream> icons = new LinkedHashMap<>();
        try {
            icons.put("background.raw", backgroundStream);
            icons.put("icWthClearDay", context.getAssets().open("fossil_hr/icWthClearDay.rle"));
            icons.put("icWthClearNite", context.getAssets().open("fossil_hr/icWthClearNite.rle"));
            icons.put("icWthCloudy", context.getAssets().open("fossil_hr/icWthCloudy.rle"));
            icons.put("icWthPartCloudyDay", context.getAssets().open("fossil_hr/icWthPartCloudyDay.rle"));
            icons.put("icWthPartCloudyNite", context.getAssets().open("fossil_hr/icWthPartCloudyNite.rle"));
            icons.put("icWthRainy", context.getAssets().open("fossil_hr/icWthRainy.rle"));
            icons.put("icWthSnowy", context.getAssets().open("fossil_hr/icWthSnowy.rle"));
            icons.put("icWthStormy", context.getAssets().open("fossil_hr/icWthStormy.rle"));
            icons.put("icWthWindy", context.getAssets().open("fossil_hr/icWthWindy.rle"));
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
        return configuration.toString();
    }
}
