/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class UpdateFirmwareOperation2021 extends UpdateFirmwareOperation2020 {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperation2021.class);

    public UpdateFirmwareOperation2021(final Uri uri, final HuamiSupport support) {
        super(uri, support);
    }

    @Override
    AbstractHuamiFirmwareInfo createFwInfo(final Uri uri, final Context context) throws IOException {
        return super.createFwInfo(uri, context);

        /*  This does not actually seem to be needed, but it's what the official app does
        final HuamiFWHelper fwHelper = getSupport().createFWHelper(uri, context);
        final AbstractHuamiFirmwareInfo firmwareInfo = fwHelper.getFirmwareInfo();

        if (!(firmwareInfo instanceof Huami2021FirmwareInfo)) {
            throw new IllegalArgumentException("Firmware not Huami2021FirmwareInfo");
        }

        final Huami2021FirmwareInfo firmwareInfo2021 = (Huami2021FirmwareInfo) firmwareInfo;
        if (firmwareInfo2021.getFirmwareType() == HuamiFirmwareType.FIRMWARE) {
            // This does not actually seem to be needed, but it's what the official app does
            return firmwareInfo2021.repackFirmwareInUIHH();
        }

        return firmwareInfo;
        */
    }

    @Override
    protected void handleNotificationNotif(byte[] value) {
        super.handleNotificationNotif(value);

        if (ArrayUtils.startsWith(value, new byte[]{HuamiService.RESPONSE, COMMAND_FINALIZE_UPDATE, HuamiService.SUCCESS})) {
            if (getFirmwareInfo().getFirmwareType() == HuamiFirmwareType.APP) {
                // After an app is installed, request the display items from the band (new app will be at the end)
                try {
                    TransactionBuilder builder = performInitialized("request display items");
                    getSupport().requestDisplayItems(builder);
                    builder.queue(getQueue());
                } catch (final IOException e) {
                    LOG.error("Failed to request display items after app install", e);
                }
            }
        }
    }

    @Override
    protected byte[] buildFirmwareInfoCommand() {
        final int fwSize = firmwareInfo.getSize();
        final int crc32 = firmwareInfo.getCrc32();

        final byte[] sizeBytes = BLETypeConversions.fromUint32(fwSize);
        final byte[] chunkSizeBytes = BLETypeConversions.fromUint16(mChunkLength);
        final byte[] crcBytes = BLETypeConversions.fromUint32(crc32);

        return new byte[]{
                COMMAND_SEND_FIRMWARE_INFO,
                getFirmwareInfo().getFirmwareType().getValue(),
                sizeBytes[0],
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3],
                crcBytes[0],
                crcBytes[1],
                crcBytes[2],
                crcBytes[3],
                chunkSizeBytes[0],
                chunkSizeBytes[1],
                0, // 0 to update in foreground, 1 for background
                (byte) 0xff, // ??
        };
    }
}
