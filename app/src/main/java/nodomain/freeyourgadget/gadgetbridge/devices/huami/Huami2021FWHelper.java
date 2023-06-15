/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021FirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;

public class Huami2021FWHelper extends HuamiFWHelper {
    private final String deviceName;
    private final Set<Integer> deviceSources;
    private final DeviceType deviceType;
    private final Map<Integer, String> crcMap;

    public Huami2021FWHelper(final Uri uri,
                             final Context context,
                             final String deviceName,
                             final Set<Integer> deviceSources,
                             final DeviceType deviceType,
                             final Map<Integer, String> crcMap) throws IOException {
        super(uri, context);
        this.deviceName = deviceName;
        this.deviceSources = deviceSources;
        this.deviceType = deviceType;
        this.crcMap = crcMap;
    }

    @Override
    public Huami2021FirmwareInfo getFirmwareInfo() {
        return (Huami2021FirmwareInfo) firmwareInfo;
    }

    /**
     * The maximum expected file size, in bytes. Files larger than this are assumed to be invalid.
     */
    public long getMaxExpectedFileSize() {
        return 1024 * 1024 * 128; // 128.0MB
    }

    @Override
    protected void determineFirmwareInfo(final byte[] wholeFirmwareBytes) {
        firmwareInfo = new Huami2021FirmwareInfo(wholeFirmwareBytes, deviceName, deviceSources, deviceType, crcMap);
        if (firmwareInfo.getFirmwareType() == HuamiFirmwareType.INVALID) {
            throw new IllegalArgumentException("Not a " + deviceName + " firmware");
        }
    }
}
