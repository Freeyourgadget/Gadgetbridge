/*  Copyright (C) 2021-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication;

import android.bluetooth.BluetoothGattCharacteristic;

public class ConfirmOnDeviceRequest extends AuthenticationRequest {
    protected boolean isFinished = false;

    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x02, 0x06, 0x30, 0x75, 0x00, 0x00, 0x00};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        if(!characteristic.getUuid().equals(getRequestUUID())){
            throw new RuntimeException("wrong characteristic responded to authentication");
        }
        byte[] value = characteristic.getValue();
        if(value.length != 4){
            throw new RuntimeException("wrong authentication response length");
        }
        if(value[0] != 0x03 || value[1] != 0x06 || value[2] != 0x00){
            throw new RuntimeException("wrong authentication response bytes");
        }
        this.onResult(value[3] == 0x01);
        this.isFinished = true;
    }

    public void onResult(boolean confirmationSuccess){};

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}
