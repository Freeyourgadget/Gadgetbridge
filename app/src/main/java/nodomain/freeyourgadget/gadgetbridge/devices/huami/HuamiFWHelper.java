/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.IOException;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;

public abstract class HuamiFWHelper extends AbstractMiBandFWHelper {
    protected AbstractHuamiFirmwareInfo firmwareInfo;

    public HuamiFWHelper(final Uri uri, final Context context) throws IOException {
        super(uri, context);
    }

    @Override
    public String format(int version) {
        return firmwareInfo.toVersion(version);
    }

    @NonNull
    @Override
    public String getFirmwareKind() {
        return GBApplication.getContext().getString(getFirmwareInfo().getFirmwareType().getNameResId());
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

    @Override
    public HuamiFirmwareType getFirmwareType() {
        return firmwareInfo.getFirmwareType();
    }

    @Override
    public void unsetFwBytes() {
        super.unsetFwBytes();
        firmwareInfo.unsetFwBytes();
    }

    public AbstractHuamiFirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }

    @Override
    public Bitmap getPreview() {
        if (firmwareInfo != null) {
            return firmwareInfo.getPreview();
        }

        return null;
    }
}
