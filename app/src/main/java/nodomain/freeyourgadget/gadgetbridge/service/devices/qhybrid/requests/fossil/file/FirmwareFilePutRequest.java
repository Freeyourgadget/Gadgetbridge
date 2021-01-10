/*  Copyright (C) 2020-2021 Andreas Shimokawa, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRawRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FirmwareFilePutRequest extends FilePutRawRequest {
    public FirmwareFilePutRequest(byte[] firmwareBytes, FossilWatchAdapter adapter) {
        super((short) 0x00FF, firmwareBytes, adapter);
    }

    @Override
    public void onPacketWritten(TransactionBuilder transactionBuilder, int packetNr, int packetCount) {
        int progressPercent = (int) ((((float) packetNr) / packetCount) * 100);
        transactionBuilder.add(new SetProgressAction(GBApplication.getContext().getString(R.string.updatefirmwareoperation_update_in_progress), true, progressPercent, GBApplication.getContext()));
    }

    @Override
    public void onFilePut(boolean success) {
        Context context = GBApplication.getContext();
        if (success) {
            GB.updateInstallNotification(context.getString(R.string.updatefirmwareoperation_update_complete), false, 100, context);
        } else {
            GB.updateInstallNotification(context.getString(R.string.updatefirmwareoperation_write_failed), false, 0, context);
        }
    }
}
