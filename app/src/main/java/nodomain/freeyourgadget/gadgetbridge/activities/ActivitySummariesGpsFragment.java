/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dikay900, Pavel Elagin

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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.GpxParser;

import static android.graphics.Bitmap.createBitmap;


public class ActivitySummariesGpsFragment extends AbstractGBFragment {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesGpsFragment.class);
    private ImageView gpsView;
    private int CANVAS_SIZE = 360;
    private File inputFile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gps, container, false);
        gpsView = rootView.findViewById(R.id.activitygpsview);
        if (inputFile != null) {
            processInBackgroundThread();
        }
        return rootView;
    }

    public void set_data(File inputFile) {
        this.inputFile = inputFile;
        if (gpsView != null) { //first fragment inflate is AFTER this is called
            processInBackgroundThread();
        }
    }

    private void processInBackgroundThread() {
        final Canvas canvas = createCanvas(gpsView);
        new Thread(new Runnable() {
            @Override
            public void run() {
                GpxParser gpxParser = null;
                FileInputStream inputStream = null;

                try {
                    inputStream = new FileInputStream(inputFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (inputStream != null) {
                    gpxParser = new GpxParser(inputStream);
                }

                if (gpxParser != null) {
                    if (gpxParser.getPoints().toArray().length > 0) {
                        drawTrack(canvas, gpxParser.getPoints());
                    }
                }
            }
        }).start();
    }

    private void drawTrack(Canvas canvas, List<GPSCoordinate> trackPoints) {
        double maxLat = (Collections.max(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double minLat = (Collections.min(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double maxLon = (Collections.max(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        double minLon = (Collections.min(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        double maxAlt = (Collections.max(trackPoints, new GPSCoordinate.compareElevation())).getAltitude();
        double minAlt = (Collections.min(trackPoints, new GPSCoordinate.compareElevation())).getAltitude();
        float scale_factor_w = (float) ((maxLat - minLat) / (maxLon - minLon));
        float scale_factor_h = (float) ((maxLon - minLon) / (maxLat - minLat));

        if (scale_factor_h > scale_factor_w) { //scaling to draw proportionally
            scale_factor_h = 1;
        } else {
            scale_factor_w = 1;
        }


        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(1);
        paint.setColor(getResources().getColor(R.color.chart_activity_light));

        for (GPSCoordinate p : trackPoints) {
            float lat = (float) ((p.getLatitude() - minLat) / (maxLat - minLat));
            float lon = (float) ((p.getLongitude() - minLon) / (maxLon - minLon));
            float alt = (float) ((p.getAltitude() - minAlt) / (maxAlt - minAlt));
            paint.setStrokeWidth(1 + alt); //make thicker with higher altitude, we could do more here
            canvas.drawPoint(CANVAS_SIZE * lat * scale_factor_w, CANVAS_SIZE * lon * scale_factor_h, paint);
        }
    }


    private Canvas createCanvas(ImageView imageView) {
        Bitmap bitmap = createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(GBApplication.getWindowBackgroundColor(getActivity()));
        //frame around, but it doesn't look so nice
        /*
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.chart_activity_light));
        canvas.drawRect(0,0,360,360,paint);
         */
        imageView.setImageBitmap(bitmap);
        imageView.setScaleY(-1f); //flip the canvas

        return canvas;
    }

    @Nullable
    @Override
    protected CharSequence getTitle() {
        return null;
    }

}

