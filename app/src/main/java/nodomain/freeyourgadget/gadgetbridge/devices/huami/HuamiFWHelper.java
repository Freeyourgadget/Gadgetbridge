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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;

public abstract class HuamiFWHelper extends AbstractMiBandFWHelper {
    protected HuamiFirmwareInfo firmwareInfo;

    public HuamiFWHelper(Uri uri, Context context) throws IOException {
        super(uri, context);
    }

    @Override
    public String format(int version) {
        return firmwareInfo.toVersion(version);
    }

    @NonNull
    @Override
    public String getFirmwareKind() {
        int resId = R.string.kind_invalid;
        switch (getFirmwareInfo().getFirmwareType()) {
            case FONT:
            case FONT_LATIN:
                resId = R.string.kind_font;
                break;
            case GPS:
                resId = R.string.kind_gps;
                break;
            case GPS_ALMANAC:
                resId = R.string.kind_gps_almanac;
                break;
            case GPS_CEP:
                resId = R.string.kind_gps_cep;
                break;
            case RES:
                resId = R.string.kind_resources;
                break;
            case RES_NEW:
                resId = R.string.kind_resources;
                break;
            case FIRMWARE:
                resId = R.string.kind_firmware;
                break;
            case WATCHFACE:
                resId = R.string.kind_watchface;
                break;
            case INVALID:
                // fall through
        }
        return GBApplication.getContext().getString(resId);
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
        return firmwareInfo.getWhitelistedVersions();
    }

    @Override
    public boolean isFirmwareGenerallyCompatibleWith(GBDevice device) {
        return firmwareInfo.isGenerallyCompatibleWith(device);
    }

    @Override
    public boolean isSingleFirmware() {
        return true;
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        firmwareInfo.checkValid();
    }

    public HuamiFirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }

}
