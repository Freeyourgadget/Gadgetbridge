/*  Copyright (C) 2024 Arjan Schrijver, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;

public abstract class AbstractHeadphoneDeviceSupport extends AbstractSerialDeviceSupport implements HeadphoneHelper.Callback {

    private HeadphoneHelper headphoneHelper;

    @Override
    public void onSetCallState(CallSpec callSpec) {
        headphoneHelper.onSetCallState(callSpec);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        headphoneHelper.onNotification(notificationSpec);
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);
        headphoneHelper = new HeadphoneHelper(getContext(), getDevice(), this);
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public void onSendConfiguration(String config) {
        if (!headphoneHelper.onSendConfiguration(config))
            super.onSendConfiguration(config);
    }

    @Override
    public void dispose() {
        if (headphoneHelper != null)
            headphoneHelper.dispose();
        super.dispose();
    }
}
