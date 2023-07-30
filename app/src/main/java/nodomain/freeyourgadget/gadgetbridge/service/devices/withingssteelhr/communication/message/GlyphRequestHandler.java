/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.IconHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.WithingsSteelHRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.GlyphId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.ImageData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.ImageMetaData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.incoming.IncomingMessageHandler;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GlyphRequestHandler implements IncomingMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlyphRequestHandler.class);
    private final WithingsSteelHRDeviceSupport support;

    public GlyphRequestHandler(WithingsSteelHRDeviceSupport support) {
        this.support = support;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            GlyphId glyphId = message.getStructureByType(GlyphId.class);
            ImageMetaData imageMetaData = message.getStructureByType(ImageMetaData.class);
            Message reply = new WithingsMessage(WithingsMessageType.GET_UNICODE_GLYPH);
            reply.addDataStructure(glyphId);
            reply.addDataStructure(imageMetaData);
            ImageData imageData = new ImageData();
            imageData.setImageData(createUnicodeImage(glyphId.getUnicode(), imageMetaData));
            reply.addDataStructure(imageData);
            logger.info("Sending reply to glyph request: " + reply);
            support.sendToDevice(reply);
        } catch (Exception e) {
            logger.error("Failed to respond to glyph request.", e);
            GB.toast("Failed to respond to glyph request:" + e.getMessage(), Toast.LENGTH_LONG, GB.WARN);
        }
    }

    private byte[] createUnicodeImage(long unicode, ImageMetaData metaData) {
        String str = new String(Character.toChars((int)unicode));
        Paint paint = new Paint();
        paint.setTypeface(null);
        Rect rect = new Rect();
        paint.setTextSize(calculateTextsize(paint, metaData.getHeight()));
        paint.setAntiAlias(true);
        paint.getTextBounds(str, 0, str.length(), rect);
        paint.setColor(-1);
        Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
        int width = rect.width();
        if (width <= 0) {
            return new byte[0];
        }
        Bitmap createBitmap = Bitmap.createBitmap(width, metaData.getHeight(), Bitmap.Config.ARGB_8888);
        new Canvas(createBitmap).drawText(str, -rect.left, -fontMetricsInt.top, paint);
        return IconHelper.toByteArray(createBitmap);
    }

    private int calculateTextsize(Paint paint, int height) {
        Paint.FontMetricsInt fontMetricsInt;
        int textsize = 0;
        do {
            textsize++;
            paint.setTextSize(textsize);
            fontMetricsInt = paint.getFontMetricsInt();
        } while (fontMetricsInt.bottom - fontMetricsInt.top < height);
        return textsize - 1;
    }
}
