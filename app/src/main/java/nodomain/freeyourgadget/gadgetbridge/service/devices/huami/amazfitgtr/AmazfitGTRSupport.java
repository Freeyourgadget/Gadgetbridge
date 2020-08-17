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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgtr;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgtr.AmazfitGTRFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts.AmazfitGTSSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class AmazfitGTRSupport extends AmazfitGTSSupport {

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTRFWHelper(uri, context);
    }

    @Override
    protected void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        super.handleDeviceInfo(info);
        if (gbDevice.getFirmwareVersion() != null) {
            Version version = new Version(gbDevice.getFirmwareVersion());
            if (version.compareTo(new Version("1.3.5.79")) >= 0 || // For GTR 47mm
                    (version.compareTo(new Version("1.0.0.00")) < 0 && version.compareTo(new Version("0.1.1.15")) >= 0)) { // for GTR 32mm with a different version scheme
                mActivitySampleSize = 8;
            }
        }
    }
}
