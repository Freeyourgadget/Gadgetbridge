/*  Copyright (C) 2018 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.roidmi;

import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.roidmi.RoidmiConst;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class RoidmiSupport extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RoidmiSupport.class);

    private final Handler handler = new Handler();
    private int infoRequestTries = 0;
    private final Runnable infosRunnable = new Runnable() {
        public void run() {
            infoRequestTries += 1;

            try {
                boolean infoMissing = false;

                if (getDevice().getExtraInfo("led_color") == null) {
                    infoMissing = true;
                    onSendConfiguration(RoidmiConst.ACTION_GET_LED_COLOR);
                }

                if (getDevice().getExtraInfo("fm_frequency") == null) {
                    infoMissing = true;

                    onSendConfiguration(RoidmiConst.ACTION_GET_FM_FREQUENCY);
                }

                if (getDevice().getType() == DeviceType.ROIDMI3) {
                    if (getDevice().getBatteryVoltage() == -1) {
                        infoMissing = true;

                        onSendConfiguration(RoidmiConst.ACTION_GET_VOLTAGE);
                    }
                }

                if (infoMissing) {
                    if (infoRequestTries < 6) {
                        requestDeviceInfos(500 + infoRequestTries * 120);
                    } else {
                        LOG.error("Failed to get Roidmi infos after 6 tries");
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to get Roidmi infos", e);
            }
        }
    };

    private void requestDeviceInfos(int delayMillis) {
        handler.postDelayed(infosRunnable, delayMillis);
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();

        requestDeviceInfos(1500);

        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        if (getDevice().getType() == DeviceType.ROIDMI) {
            return new Roidmi1Protocol(getDevice());
        } else if (getDevice().getType() == DeviceType.ROIDMI3) {
            return new Roidmi3Protocol(getDevice());
        }

        LOG.error("Unsupported device type with key = " + getDevice().getType().getKey());
        return null;
    }

    @Override
    public void onSendConfiguration(final String config) {
        LOG.debug("onSendConfiguration " + config);

        RoidmiIoThread roidmiIoThread = getDeviceIOThread();
        RoidmiProtocol roidmiProtocol = (RoidmiProtocol) getDeviceProtocol();

        switch (config) {
            case RoidmiConst.ACTION_GET_LED_COLOR:
                roidmiIoThread.write(roidmiProtocol.encodeGetLedColor());
                break;
            case RoidmiConst.ACTION_GET_FM_FREQUENCY:
                roidmiIoThread.write(roidmiProtocol.encodeGetFmFrequency());
                break;
            case RoidmiConst.ACTION_GET_VOLTAGE:
                roidmiIoThread.write(roidmiProtocol.encodeGetVoltage());
                break;
            default:
                LOG.error("Invalid Roidmi configuration " + config);
                break;
        }
    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new RoidmiIoThread(getDevice(), getContext(), (RoidmiProtocol) getDeviceProtocol(), RoidmiSupport.this, getBluetoothAdapter());
    }

    @Override
    public synchronized RoidmiIoThread getDeviceIOThread() {
        return (RoidmiIoThread) super.getDeviceIOThread();
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
}
