/*  Copyright (C) 2018-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband3;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3FWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;

public class MiBand3Support extends AmazfitBipSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand3Support.class);

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    protected MiBand3Support setDisplayItems(TransactionBuilder builder) {
        Map<String, Integer> keyPosMap = new LinkedHashMap<>();
        keyPosMap.put("notifications", 1);
        keyPosMap.put("weather", 2);
        keyPosMap.put("activity", 3);
        keyPosMap.put("more", 4);
        keyPosMap.put("status", 5);
        keyPosMap.put("heart_rate", 6);
        keyPosMap.put("timer", 7);
        keyPosMap.put("nfc", 8);

        setDisplayItemsOld(builder, false, R.array.pref_miband3_display_items_default, keyPosMap);
        return this;
    }

    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);
        LOG.info("phase2Initialize...");
        if (HuamiCoordinator.getOverwriteSettingsOnConnection(getDevice().getAddress())) {
            setLanguage(builder);
            setBandScreenUnlock(builder);
            setNightMode(builder);
            setDateFormat(builder);
        }
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand3FWHelper(uri, context);
    }
}
