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

public class MisirunC17Coordinator extends AbstractMoyoungDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MisirunC17Coordinator.class);

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("C17");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_misirun_c17;
    }


    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_banglejs;
    }

    @Override
    @DrawableRes
    public int getDisabledIconResource() {
        return R.drawable.ic_device_banglejs_disabled;
    }

    @Override
    public String getManufacturer() {
        return "Misirun";
    }

    @Override
    public int getMtu() {
        return 508;
    }
}