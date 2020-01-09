/*  Copyright (C) 2019-2020 Andreas Shimokawa

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

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;

public class UpdateFirmwareOperationNew extends UpdateFirmwareOperation {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperationNew.class);


    public UpdateFirmwareOperationNew(Uri uri, HuamiSupport support) {
        super(uri, support);
    }


    public boolean sendFwInfo() {
        try {
            TransactionBuilder builder = performInitialized("send firmware info");
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.updating_firmware), getContext()));
            int fwSize = getFirmwareInfo().getSize();
            byte[] sizeBytes = BLETypeConversions.fromUint24(fwSize);
            byte[] bytes = new byte[10];
            int i = 0;
            bytes[i++] = HuamiService.COMMAND_FIRMWARE_INIT;
            bytes[i++] = getFirmwareInfo().getFirmwareType().getValue();
            bytes[i++] = sizeBytes[0];
            bytes[i++] = sizeBytes[1];
            bytes[i++] = sizeBytes[2];
            bytes[i++] = 0; // TODO: what is that?
            int crc32 = firmwareInfo.getCrc32();
            byte[] crcBytes = BLETypeConversions.fromUint32(crc32);
            bytes[i++] = crcBytes[0];
            bytes[i++] = crcBytes[1];
            bytes[i++] = crcBytes[2];
            bytes[i] = crcBytes[3];


            builder.write(fwCControlChar, bytes);
            builder.queue(getQueue());
            return true;
        } catch (IOException e) {
            LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
            return false;
        }
    }

    @Override
    protected void sendChecksum(HuamiFirmwareInfo firmwareInfo) throws IOException {
        TransactionBuilder builder = performInitialized("send firmware upload finished");
        builder.write(fwCControlChar, new byte[]{HuamiService.COMMAND_FIRMWARE_CHECKSUM});
        builder.queue(getQueue());
    }

    @Override
    protected byte[] getFirmwareStartCommand() {
        return new byte[]{HuamiService.COMMAND_FIRMWARE_START_DATA, 1};
    }
}
