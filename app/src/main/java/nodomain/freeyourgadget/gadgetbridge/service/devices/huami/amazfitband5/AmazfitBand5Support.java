/*  Copyright (C) 2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitband5;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitband5.AmazfitBand5FWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband5.MiBand5Support;

public class AmazfitBand5Support extends MiBand5Support {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBand5Support.class);

    @Override
    protected AmazfitBand5Support setDisplayItems(TransactionBuilder builder) {
        Map<String, Integer> keyIdMap = new LinkedHashMap<>();
        keyIdMap.put("status", 0x01);
        keyIdMap.put("pai", 0x19);
        keyIdMap.put("hr", 0x02);
        keyIdMap.put("spo2", 0x24);
        keyIdMap.put("notifications", 0x06);
        keyIdMap.put("breathing", 0x33);
        keyIdMap.put("eventreminder", 0x15);
        keyIdMap.put("weather", 0x04);
        keyIdMap.put("workout", 0x03);
        keyIdMap.put("more", 0x07);
        keyIdMap.put("stress", 0x1c);
        keyIdMap.put("period", 0x1d);

        setDisplayItemsNew(builder, false, R.array.pref_amazfitband5_display_items_default, keyIdMap);
        return this;
    }

    @Override
    protected AmazfitBand5Support setShortcuts(TransactionBuilder builder) {
        Map<String, Integer> keyIdMap = new LinkedHashMap<>();
        keyIdMap.put("notifications", 0x06);
        keyIdMap.put("weather", 0x04);
        keyIdMap.put("music", 0x0b);
        keyIdMap.put("timer", 0x0d);
        keyIdMap.put("alarm", 0x09);
        keyIdMap.put("findphone", 0x0e);
        keyIdMap.put("worldclock", 0x1a);
        keyIdMap.put("status", 0x01);
        keyIdMap.put("pai", 0x19);
        keyIdMap.put("hr", 0x02);
        keyIdMap.put("spo2", 0x24);
        keyIdMap.put("stress", 0x1c);
        keyIdMap.put("eventreminder", 0x15);
        keyIdMap.put("dnd", 0x08);
        keyIdMap.put("stopwatch", 0x0c);
        keyIdMap.put("workout", 0x03);
        keyIdMap.put("mutephone", 0x0f);
        keyIdMap.put("period", 0x1d);
        keyIdMap.put("takephoto", 0x0a);
        keyIdMap.put("alexa", 0x39);
        setDisplayItemsNew(builder, true, R.array.pref_amazfitband5_shortcuts_default, keyIdMap);

        return this;
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitBand5FWHelper(uri, context);
    }
}
