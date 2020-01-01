/*  Copyright (C) 2017-2019 Daniele Gobbetti, João Paulo Barraca, Quallenauge

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
package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author Quallenauge &lt;Hamsi2k@freenet.de&gt;
*/


import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

/**
 * Pseudo Coordinator for the EXRIZU K8, a sub type of the HPLUS devices
 */
public class EXRIZUK8Coordinator extends HPlusCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        if(name != null && name.startsWith("iRun ")){
            return DeviceType.EXRIZUK8;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.EXRIZUK8;
    }

    @Override
    public String getManufacturer() {
        return "EXRIZU";
    }

}
