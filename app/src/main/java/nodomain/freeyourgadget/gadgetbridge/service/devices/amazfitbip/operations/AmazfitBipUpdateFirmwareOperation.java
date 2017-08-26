/*  Copyright (C) 2017 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip.operations;

import android.net.Uri;
import android.widget.Toast;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip.AmazfitBipFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AmazfitBipUpdateFirmwareOperation extends UpdateFirmwareOperation {
    public AmazfitBipUpdateFirmwareOperation(Uri uri, AmazfitBipSupport support) {
        super(uri, support);
    }

    @Override
    protected void doPerform() throws IOException {
        AmazfitBipFWHelper mFwHelper = new AmazfitBipFWHelper(uri, getContext());

        firmwareInfo = mFwHelper.getFirmwareInfo();
        if (!firmwareInfo.isGenerallyCompatibleWith(getDevice())) {
            throw new IOException("Firmware is not compatible with the given device: " + getDevice().getAddress());
        }

        if (!sendFwInfo()) {
            displayMessage(getContext(), "Error sending firmware info, aborting.", Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
        //the firmware will be sent by the notification listener if the band confirms that the metadata are ok.
    }
}
