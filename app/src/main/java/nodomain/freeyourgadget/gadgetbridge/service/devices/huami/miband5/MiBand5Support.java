/*  Copyright (C) 2019-2021 Andreas Shimokawa, Manuel Ruß, odavo32nof

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

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband5.MiBand5FWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband4.MiBand4Support;

public class MiBand5Support extends MiBand4Support {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand5Support.class);

    @Override
    protected MiBand5Support setDisplayItems(TransactionBuilder builder) {
        setDisplayItemsNew(builder, false, true, R.array.pref_miband5_display_items_default);
        return this;
    }

    @Override
    protected MiBand5Support setShortcuts(TransactionBuilder builder) {
        setDisplayItemsNew(builder, true, true, R.array.pref_miband5_shortcuts_default);
        return this;
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand5FWHelper(uri, context);
    }


    @Override
    public boolean supportsSunriseSunsetWindHumidity() {
        return true;
    }

    @Override
    public int getActivitySampleSize() {
        return 8;
    }
}
