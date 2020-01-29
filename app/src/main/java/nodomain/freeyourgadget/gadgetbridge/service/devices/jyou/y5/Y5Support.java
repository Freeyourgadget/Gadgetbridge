/*  Copyright (C) 2017-2020 Andreas Shimokawa, Da Pa, Pavel Elagin, Sami Alaoui

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.y5;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.JYouConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.JYouSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.JYouActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.JYouSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.RealtimeSamplesSupport;

public class Y5Support extends JYouSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Y5Support.class);

    private RealtimeSamplesSupport realtimeSamplesSupport;


    public Y5Support() {
        super(LOG);
        addSupportedService(JYouConstants.UUID_SERVICE_JYOU);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        switch (data[0]) {
            case JYouConstants.RECEIVE_HISTORY_SLEEP_COUNT:
                LOG.info("onCharacteristicChanged: " + data[0]);
                return true;
            case JYouConstants.RECEIVE_BLOOD_PRESSURE:
                int heartRate = data[2];
                int bloodPressureHigh = data[3];
                int bloodPressureLow = data[4];
                int bloodOxygen = data[5];
                int Fatigue = data[6];
                LOG.info("RECEIVE_BLOOD_PRESSURE: Heart rate: " + heartRate + " Pressure high: " + bloodPressureHigh+ " pressure low: " + bloodPressureLow);
                return true;
            case JYouConstants.RECEIVE_DEVICE_INFO:
                int model = data[7];
                int fwVerNum = data[4] & 0xFF;
                versionCmd.fwVersion = (fwVerNum / 100) + "." + ((fwVerNum % 100) / 10) + "." + ((fwVerNum % 100) % 10);
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case JYouConstants.RECEIVE_BATTERY_LEVEL:
                batteryCmd.level = data[8];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + batteryCmd.level);
                return true;
            case JYouConstants.RECEIVE_STEPS_DATA:
                int steps = ByteBuffer.wrap(data, 5, 4).getInt();
                LOG.info("Number of walked steps: " + steps);
                handleRealtimeSteps(steps);
                return true;
            case JYouConstants.RECEIVE_HEARTRATE:
                handleHeartrate(data[8]);
                return true;
            case JYouConstants.RECEIVE_WATCH_MAC:
                return true;
            case JYouConstants.RECEIVE_GET_PHOTO:
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                return true;
        }
    }

    private void handleRealtimeSteps(int value) {
        //todo Call on connect the device
        if (LOG.isDebugEnabled()) {
            LOG.debug("realtime steps: " + value);
        }
        getRealtimeSamplesSupport().setSteps(value);
    }

    private void handleHeartrate(int value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("heart rate: " + value);
        }
        RealtimeSamplesSupport realtimeSamplesSupport = getRealtimeSamplesSupport();
        realtimeSamplesSupport.setHeartrateBpm(value);
        if (!realtimeSamplesSupport.isRunning()) {
            // single shot measurement, manually invoke storage and result publishing
            realtimeSamplesSupport.triggerCurrentSample();
        }
    }

    public JYouActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        JYouActivitySample sample = new JYouActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);
        return sample;
    }

    private void enableRealtimeSamplesTimer(boolean enable) {
        if (enable) {
            getRealtimeSamplesSupport().start();
        } else {
            if (realtimeSamplesSupport != null) {
                realtimeSamplesSupport.stop();
            }
        }
    }

    private RealtimeSamplesSupport getRealtimeSamplesSupport() {
        if (realtimeSamplesSupport == null) {
            realtimeSamplesSupport = new RealtimeSamplesSupport(1000, 1000) {
                @Override
                public void doCurrentSample() {

                    try (DBHandler handler = GBApplication.acquireDB()) {
                        DaoSession session = handler.getDaoSession();
                        int ts = (int) (System.currentTimeMillis() / 1000);
                        JYouSampleProvider provider = new JYouSampleProvider(gbDevice, session);
                        JYouActivitySample sample = createActivitySample(DBHelper.getDevice(getDevice(), session), DBHelper.getUser(session), ts, provider);
                        sample.setHeartRate(getHeartrateBpm());
                        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                        sample.setRawKind(JYouSampleProvider.TYPE_ACTIVITY); // to make it visible in the charts TODO: add a MANUAL kind for that?

                        provider.addGBActivitySample(sample);

                        // set the steps only afterwards, since realtime steps are also recorded
                        // in the regular samples and we must not count them twice
                        // Note: we know that the DAO sample is never committed again, so we simply
                        // change the value here in memory.
                        sample.setSteps(getSteps());
                        if(steps > 1){
                            LOG.debug("Have steps: " + getSteps());
                        }

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("realtime sample: " + sample);
                        }

                        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

                    } catch (Exception e) {
                        LOG.warn("Unable to acquire db for saving realtime samples", e);
                    }
                }
            };
        }
        return realtimeSamplesSupport;
    }

    @Override
    protected void syncSettings(TransactionBuilder builder) {
        syncDateAndTime(builder);
    }

    @Override
    public void dispose() {
        LOG.info("Dispose");
        super.dispose();
    }

    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_SET_HEARTRATE_AUTO, 0, 0

            ));
            performConnected(builder.getTransaction());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("RealTimeHeartMeasurement");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_ACTION_HEARTRATE_SWITCH, 0, enable ? 1 : 0
            ));
            performConnected(builder.getTransaction());
            enableRealtimeSamplesTimer(enable);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

}
