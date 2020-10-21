/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband5;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband5.MiBand5FWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband4.MiBand4Support;

public class MiBand5Support extends MiBand4Support {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand5Support.class);

    @Override
    protected MiBand5Support setDisplayItems(TransactionBuilder builder) {
        Map<String, Integer> keyIdMap = new LinkedHashMap<>();
        keyIdMap.put("status", 0x01);
        keyIdMap.put("pai", 0x19);
        keyIdMap.put("hr", 0x02);
        keyIdMap.put("notifications", 0x06);
        keyIdMap.put("breathing", 0x33);
        keyIdMap.put("eventreminder", 0x15);
        keyIdMap.put("weather", 0x04);
        keyIdMap.put("workout", 0x03);
        keyIdMap.put("more", 0x07);
        keyIdMap.put("stress", 0x1c);
        keyIdMap.put("cycles", 0x1d);

        setDisplayItemsNew(builder, false, R.array.pref_miband5_display_items_default, keyIdMap);
        return this;
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand5FWHelper(uri, context);
    }

    @Override
    public int getActivitySampleSize() {
        return 8;
    }
}
