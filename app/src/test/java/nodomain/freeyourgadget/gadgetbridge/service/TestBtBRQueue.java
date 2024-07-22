/*  Copyright (C) 2022-2023 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.BtBRQueue;

@RunWith(MockitoJUnitRunner.class)
public class TestBtBRQueue {

    @Test
    public void connect() {
        GBDevice device = Mockito.mock(GBDevice.class);
        when(device.isConnected()).thenReturn(false);

        BluetoothDevice btDevice = Mockito.mock(BluetoothDevice.class);

        BluetoothAdapter btAdapter = Mockito.mock(BluetoothAdapter.class);
        when(btAdapter.getRemoteDevice((String) any())).thenReturn(btDevice);

        BtBRQueue queue = new BtBRQueue(btAdapter, device, null, null, null, 512);
        Assert.assertTrue(queue.connect());
    }

    @Test
    public void reconnect() {
        GBDevice device = Mockito.mock(GBDevice.class);
        when(device.isConnected()).thenReturn(false);

        BluetoothDevice btDevice = Mockito.mock(BluetoothDevice.class);

        BluetoothAdapter btAdapter = Mockito.mock(BluetoothAdapter.class);
        when(btAdapter.getRemoteDevice((String) any())).thenReturn(btDevice);

        BtBRQueue queue = new BtBRQueue(btAdapter, device, null, null, null, 512);
        Assert.assertTrue(queue.connect());

        queue.disconnect();

        Assert.assertTrue(queue.connect());
    }
}
