/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.deviceevents.SonyHeadphonesEnqueueRequestEvent;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SonyHeadphonesSupport extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SonyHeadphonesSupport.class);

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new SonyHeadphonesProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new SonyHeadphonesIoThread(getDevice(), getContext(), (SonyHeadphonesProtocol) getDeviceProtocol(), SonyHeadphonesSupport.this, getBluetoothAdapter());
    }

    @Override
    public synchronized SonyHeadphonesIoThread getDeviceIOThread() {
        return (SonyHeadphonesIoThread) super.getDeviceIOThread();
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        final SonyHeadphonesProtocol sonyProtocol = (SonyHeadphonesProtocol) getDeviceProtocol();

        if (deviceEvent instanceof SonyHeadphonesEnqueueRequestEvent) {
            final SonyHeadphonesEnqueueRequestEvent enqueueRequestEvent = (SonyHeadphonesEnqueueRequestEvent) deviceEvent;
            sonyProtocol.enqueueRequests(enqueueRequestEvent.getRequests());

            if (sonyProtocol.getPendingAcks() == 0) {
                // There are no pending acks, send one request from the queue
                // TODO: A more elegant way of scheduling these?
                SonyHeadphonesIoThread deviceIOThread = getDeviceIOThread();
                deviceIOThread.write(sonyProtocol.getFromQueue());
            }
        }

        super.evaluateGBDeviceEvent(deviceEvent);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onInstallApp(Uri uri) {
        // Nothing to do
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config, Integer id) {
        // Nothing to do
    }

    @Override
    public void onHeartRateTest() {
        // Nothing to do
    }

    @Override
    public void onSetConstantVibration(int intensity) {
        // Nothing to do
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        // Nothing to do
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        // Nothing to do
    }

    @Override
    public void onReadConfiguration(String config) {
        // Nothing to do
    }
}
