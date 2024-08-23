/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import androidx.annotation.DrawableRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ColmiI28UltraCoordinator extends AbstractMoyoungDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(ColmiI28UltraCoordinator.class);

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("i28 Ultra");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_colmi_i28_ultra;
    }


    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miwatch;
    }

    @Override
    @DrawableRes
    public int getDisabledIconResource() {
        return R.drawable.ic_device_miwatch_disabled;
    }

    @Override
    public String getManufacturer() {
        return "Colmi";
    }
}