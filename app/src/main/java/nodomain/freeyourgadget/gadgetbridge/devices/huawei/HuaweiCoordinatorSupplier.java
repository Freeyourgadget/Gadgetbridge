/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public interface HuaweiCoordinatorSupplier {

    enum HuaweiDeviceType {
        AW(0),     //BLE behind
        BR(1),
        BLE(2),
        SMART(5)   //BLE behind
        ;

        final int huaweiType;

        HuaweiDeviceType(int huaweiType) {
            this.huaweiType = huaweiType;
        }

        public int getType(){
            return huaweiType;
        }
    }

    HuaweiCoordinator getHuaweiCoordinator();
    HuaweiDeviceType getHuaweiType();
    DeviceType getDeviceType();
    void setDevice(GBDevice Device);
    GBDevice getDevice();
}
