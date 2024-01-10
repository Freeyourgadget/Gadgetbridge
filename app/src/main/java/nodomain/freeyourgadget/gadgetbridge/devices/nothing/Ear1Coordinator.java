/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, Daniele Gobbetti,
    José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class Ear1Coordinator extends AbstractEarCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Nothing ear (1)", Pattern.LITERAL);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_nothingear1;
    }

    @Override
    public boolean incrementCounter() {
        return false;
    }

    @Override
    public boolean supportsLightAncAndTransparency() {
        return true;
    }
}
