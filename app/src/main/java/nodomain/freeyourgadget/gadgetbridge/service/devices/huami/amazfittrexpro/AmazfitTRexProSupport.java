/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Dmytro Bielik

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfittrexpro;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfittrexpro.AmazfitTRexProFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts.AmazfitGTSSupport;

public class AmazfitTRexProSupport extends AmazfitGTSSupport {

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitTRexProFWHelper(uri, context);
    }

    @Override
    protected AmazfitTRexProSupport setDisplayItems(TransactionBuilder builder) {
        setDisplayItemsNew(builder, false, true, R.array.pref_trex_pro_display_items_default);
        return this;
    }

}
