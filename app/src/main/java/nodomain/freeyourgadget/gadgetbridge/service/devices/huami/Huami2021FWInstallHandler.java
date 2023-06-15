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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021FWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class Huami2021FWInstallHandler extends AbstractMiBandFWInstallHandler {
    private final String deviceName;
    private final Set<Integer> deviceSources;
    private final DeviceType deviceType;
    private final Map<Integer, String> crcMap;

    public Huami2021FWInstallHandler(final Uri uri,
                                     final Context context,
                                     final String deviceName,
                                     final Set<Integer> deviceSources,
                                     final DeviceType deviceType,
                                     final Map<Integer, String> crcMap) {
        super(uri, context);
        this.deviceName = deviceName;
        this.deviceSources = deviceSources;
        this.deviceType = deviceType;
        this.crcMap = crcMap;
    }

    @Override
    public Huami2021FWHelper getHelper() {
        return (Huami2021FWHelper) helper;
    }

    protected boolean isSupportedDeviceType(final GBDevice device) {
        return device.getType() == deviceType;
    }

    @Override
    protected String getFwUpgradeNotice() {
        return mContext.getString(
                R.string.fw_upgrade_notice_zip,
                helper.getHumanFirmwareVersion(),
                mContext.getString(deviceType.getName())
        );
    }

    @Override
    protected Huami2021FWHelper createHelper(final Uri uri, final Context context) throws IOException {
        return new Huami2021FWHelper(
                uri,
                context,
                deviceName,
                deviceSources,
                deviceType,
                crcMap
        );
    }

    @Override
    public void onStartInstall(final GBDevice device) {
        // Unset the firmware bytes
        // Huami2021 firmwares are large (> 130MB). With the current architecture, the update operation
        // will re-read them to memory, and we run out-of-memory.
        helper.unsetFwBytes();
    }
}
