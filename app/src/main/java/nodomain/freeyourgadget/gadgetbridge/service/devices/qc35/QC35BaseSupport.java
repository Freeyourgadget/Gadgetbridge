/*  Copyright (C) 2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qc35;

import android.net.Uri;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class QC35BaseSupport extends AbstractSerialDeviceSupport {

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }



    @Override
    public boolean connect() {
        getDeviceProtocol();
        getDeviceIOThread().start();
        getDevice().setBatteryThresholdPercent((short)25);
        return true;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new QC35Protocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new QC35IOThread(getDevice(), getContext(), (QC35Protocol) createDeviceProtocol(), this, getBluetoothAdapter());
    }
}
