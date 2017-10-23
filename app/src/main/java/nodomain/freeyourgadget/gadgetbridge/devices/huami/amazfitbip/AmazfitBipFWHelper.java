/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip.AmazfitBipFirmwareInfo;

public class AmazfitBipFWHelper extends AbstractMiBandFWHelper {

    public AmazfitBipFWHelper(Uri uri, Context context) throws IOException {
        super(uri, context);
    }

    private AmazfitBipFirmwareInfo firmwareInfo;

    @Override
    public String format(int version) {
        return AmazfitBipFirmwareInfo.toVersion(version);
    }

    @Override
    public int getFirmwareVersion() {
        return firmwareInfo.getFirmwareVersion();
    }

    @Override
    public int getFirmware2Version() {
        return 0;
    }

    @Override
    public String getHumanFirmwareVersion2() {
        return "";
    }

    @Override
    protected int[] getWhitelistedFirmwareVersions() {
        return AmazfitBipFirmwareInfo.getWhitelistedVersions();
    }

    @Override
    public boolean isFirmwareGenerallyCompatibleWith(GBDevice device) {
        return firmwareInfo.isGenerallyCompatibleWith(device);
    }

    @Override
    public boolean isSingleFirmware() {
        return true;
    }

    @NonNull
    @Override
    protected void determineFirmwareInfo(byte[] wholeFirmwareBytes) {
        firmwareInfo = new AmazfitBipFirmwareInfo(wholeFirmwareBytes);
        if (!firmwareInfo.isHeaderValid()) {
            throw new IllegalArgumentException("Not a an Amazifit Bip firmware");
        }
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        firmwareInfo.checkValid();
    }

    public AmazfitBipFirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }

}
