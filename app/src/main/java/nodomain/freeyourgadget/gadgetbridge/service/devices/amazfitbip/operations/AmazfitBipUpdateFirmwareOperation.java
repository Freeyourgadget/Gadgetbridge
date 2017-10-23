/*  Copyright (C) 2017 Andreas Shimokawa, Carsten Pfeiffer

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

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.Mi2FirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations.UpdateFirmwareOperation;

public class AmazfitBipUpdateFirmwareOperation extends UpdateFirmwareOperation {
    public AmazfitBipUpdateFirmwareOperation(Uri uri, AmazfitBipSupport support) {
        super(uri, support);
    }


    protected Mi2FirmwareInfo createFwInfo(Uri uri, Context context) throws IOException {
        AmazfitBipFWHelper fwHelper = new AmazfitBipFWHelper(uri, getContext());
        return fwHelper.getFirmwareInfo();
    }
}
