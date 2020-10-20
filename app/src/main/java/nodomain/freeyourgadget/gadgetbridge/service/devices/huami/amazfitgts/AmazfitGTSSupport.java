/*  Copyright (C) 2019-2020 Andreas Shimokawa, Manuel Ruß

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts.AmazfitGTSFWHelper;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperationNew;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class AmazfitGTSSupport extends AmazfitBipSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitGTSSupport.class);

    @Override
    public byte getCryptFlags() {
        return (byte) 0x80;
    }

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        super.sendNotificationNew(notificationSpec, true);
    }


    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTSFWHelper(uri, context);
    }

    @Override
    public UpdateFirmwareOperationNew createUpdateFirmwareOperation(Uri uri) {
        return new UpdateFirmwareOperationNew(uri, this);
    }

    @Override
    protected AmazfitGTSSupport setDisplayItems(TransactionBuilder builder) {
        Map<String, Integer> keyIdMap = new LinkedHashMap<>();
        keyIdMap.put("status", 0x01);
        keyIdMap.put("pai", 0x19);
        keyIdMap.put("hr", 0x02);
        keyIdMap.put("workout", 0x03);
        keyIdMap.put("activity", 0x14);
        keyIdMap.put("weather", 0x04);
        keyIdMap.put("music", 0x0b);
        keyIdMap.put("notifications", 0x06);
        keyIdMap.put("alarm", 0x09);
        keyIdMap.put("eventreminder", 0x15);
        keyIdMap.put("more", 0x07);
        keyIdMap.put("settings", 0x13);

        setDisplayItemsNew(builder, R.array.pref_gts_display_items_default, keyIdMap);
        return this;
    }

    @Override
    protected void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        super.handleDeviceInfo(info);
        if (gbDevice.getFirmwareVersion() != null) {
            Version version = new Version(gbDevice.getFirmwareVersion());
            if (version.compareTo(new Version("0.0.9.00")) > 0) {
                mActivitySampleSize = 8;
            }
        }
    }
}
