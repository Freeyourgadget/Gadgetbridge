/*  Copyright (C) 2020 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioGBX100DeviceSupport;

public class InitOperationGBX100 extends AbstractBTLEOperation<CasioGBX100DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(InitOperationGBX100.class);

    private final TransactionBuilder builder;
    private final CasioGBX100DeviceSupport support;

    public InitOperationGBX100(CasioGBX100DeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.builder = builder;
        this.support = support;
        builder.setGattCallback(this);
    }

    private void writeAllFeaturesRequest(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    private void writeAllFeaturesRequest(byte[] arr) {
        try {
            TransactionBuilder builder = createTransactionBuilder("writeAllFeaturesRequest");
            builder.setGattCallback(this);
            writeAllFeaturesRequest(builder, arr);
            support.performImmediately(builder);
        } catch(IOException e) {
            LOG.error("Error writing all features: " + e.getMessage());
        }
    }

    private void writeAllFeatures(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    private void writeAllFeatures(byte[] arr) {
        try {
            TransactionBuilder builder = createTransactionBuilder("writeAllFeatures");
            builder.setGattCallback(this);
            writeAllFeatures(builder, arr);
            support.performImmediately(builder);
        } catch(IOException e) {
            LOG.error("Error writing all features: " + e.getMessage());
        }
    }

    private void writeAllFeaturesInit() {
        byte[] arr = new byte[2];
        arr[0] = 0x00;
        arr[1] = 0x01;

        writeAllFeatures(arr);
    }

    private void requestWatchName(TransactionBuilder builder) {
        writeAllFeaturesRequest(builder, new byte[]{CasioConstants.characteristicToByte.get("CASIO_WATCH_NAME")});
    }

    private void requestWatchName() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_WATCH_NAME")});
    }

    private void requestBleConfiguration() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_BLE_FEATURES")});
    }

    private void requestBleSettings() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_BLE")});
    }

    private void requestAdvertisingParameters() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_ADVERTISE_PARAMETER_MANAGER")});
    }

    private void requestConnectionParameters() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_CONNECTION_PARAMETER_MANAGER")});
    }

    private void requestModuleId() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_MODULE_ID")});
    }

    private void requestWatchCondition() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_WATCH_CONDITION")});
    }

    private void requestVersionInformation() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_VERSION_INFORMATION")});
    }

    private void requestDstWatchState() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_DST_WATCH_STATE")});
    }

    private void requestDstSetting() {
        writeAllFeaturesRequest(new byte[]{CasioConstants.characteristicToByte.get("CASIO_DST_SETTING")});
    }

    private void writeWatchName() {
        // FIXME: The App ID should be auto-generated and stored in the
        // preferences instead of hard-coding it here
        // CASIO_APP_INFORMATION:
        byte arr[] = new byte[12];
        arr[0] = CasioConstants.characteristicToByte.get("CASIO_APP_INFORMATION");
        for(int i=0; i<10; i++)
            arr[i+1] = (byte) (i & 0xff);
        arr[11] = 2;
        writeAllFeatures(arr);
    }

    private void writeBleSettings(byte[] data) {
        if(data.length < 14)
            return;

        // FIXME: We use hard-coded values here, should they be changeable?

        // CASIO_SETTING_FOR_BLE:
        // Don't forget first byte, we write to ALL_FEATURES!
        // 0000000600000000000100801e00
        // rest: mirror
        // mBleSettings[5] = (byte)(connInterval & 0xff);
        // mBleSettings[6] = (byte)((connInterval >> 8) & 0xff);
        // mBleSettings[7] = (byte)(slaveLatency & 0xff);
        // mBleSettings[8] = (byte)((slaveLatency >> 8) & 0xff);
        // mBleSettings[11] = Auto Reconnect Hour
        // mBleSettings[12] = Auto Reconnect Minute

        // Shift-by-one b/c of ALL_FEATURES!
        data[12] = (byte)0x80;
        data[13] = (byte)0x1e;
        writeAllFeatures(data);
    }

    private void writeAdvertisingParameters() {
        // FIXME: This is hardcoded for now, based on the HCI log of my watch
        // CASIO_ADVERTISE_PARAMETER_MANAGER
        // Don't forget first byte, we write to ALL_FEATURES!
        // 50000a4001050540060a02a000054001051c00
        // byte arr[] = new byte[19];
        // arr[0] = advertiseParamAfterSleepResetOrKeyFunctionAdvertiseInterval & 0xff;
        // arr[1] = advertiseParamAfterSleepResetOrKeyFunctionAdvertiseInterval >> 8 & 0xff;
        // arr[2] = getAdvertiseParamAfterSleepResetOrKeyFunctionAdvertisingTime;
        // arr[3] = advertiseParamHighIntervalAdvertisementAfterLinkLossAdvertiseInterval & 0xff;
        // arr[4] = advertiseParamHighIntervalAdvertisementAfterLinkLossAdvertiseInterval >> 8 & 0xff;
        // arr[5] = getAdvertiseParamHighIntervalAdvertisementAfterLinkLossAdvertisingTime;
        // arr[6] = getAdvertiseParamHighIntervalAdvertisementAfterLinkLossFrequencyToAdvertise;
        // arr[7] = advertiseParamLowIntervalAdvertisementAfterLinkLossAdvertiseInterval & 0xff;
        // arr[8] = advertiseParamLowIntervalAdvertisementAfterLinkLossAdvertiseInterval >> 8 & 0xff;
        // arr[9] = getAdvertiseParamLowIntervalAdvertisementAfterLinkLossAdvertisingTime & 0xff;
        // arr[10] = getAdvertiseParamLowIntervalAdvertisementAfterLinkLossFrequencyToAdvertise & 0xff;
        // arr[11] = advertiseParamRegularBeaconAdvertisementAdvertiseInterval & 0xff;
        // arr[12] = advertiseParamRegularBeaconAdvertisementAdvertiseInterval >> 8 & 0xff;
        // arr[13] = getAdvertiseParamRegularBeaconAdvertisementAdvertisingTime;
        // arr[14] = advertiseParamAdvertiseImmediatelyAfterLinkLossAdvertiseInterval & 0xff;
        // arr[15] = advertiseParamAdvertiseImmediatelyAfterLinkLossAdvertiseInterval >> 8 & 0xff;
        // arr[16] = getAdvertiseParamAdvertiseImmediatelyAfterLinkLossAdvertisingTime
        // arr[17] = advertiseParamPeriodUntilSuspensionOfAutoAdvertise & 0xff;
        // arr[18] = advertiseParamPeriodUntilSuspensionOfAutoAdvertise >> 8 & 0xff;

        byte arr[] = new byte[20];
        arr[0] = CasioConstants.characteristicToByte.get("CASIO_ADVERTISE_PARAMETER_MANAGER");
        arr[1] = (byte)0x50;
        arr[2] = (byte)0x00;
        arr[3] = (byte)0x0a;
        arr[4] = (byte)0x40;
        arr[5] = (byte)0x01;
        arr[6] = (byte)0x05;
        arr[7] = (byte)0x05;
        arr[8] = (byte)0x40;
        arr[9] = (byte)0x06;
        arr[10] = (byte)0x0a;
        arr[11] = (byte)0x02;
        arr[12] = (byte)0xa0;
        arr[13] = (byte)0x00;
        arr[14] = (byte)0x05;
        arr[15] = (byte)0x40;
        arr[16] = (byte)0x01;
        arr[17] = (byte)0x05;
        arr[18] = (byte)0x1c;
        arr[19] = (byte)0x00;
        writeAllFeatures(arr);
    }

    private void writeConnectionParameters() {
        // FIXME: This is hardcoded for now, based on the HCI log of my watch
        // CASIO_CONNECTION_PARAMETER_MANAGER
        // Don't forget first byte, we write to ALL_FEATURES!
        // 00340140010400dc05
        // byte[] arr = new byte[9];
        // arr[0] = getConnectionParamCommand;
        // arr[1] = connectionParamMinimumConnectionInterval & 0xff;
        // arr[2] = connectionParamMinimumConnectionInterval >> 8 & 0xff;
        // arr[3] = connectionParamMaxConnectionInterval & 0xff;
        // arr[4] = connectionParamMaxConnectionInterval >> 8 & 0xff;
        // arr[5] = connectionParamConnLatency & 0xff;
        // arr[6] = connectionParamConnLatency >> 8 & 0xff;
        // arr[7] = connectionParamSupervisionTimeout & 0xff;
        // arr[8] = connectionParamSupervisionTimeout >> 8 & 0xff;

        byte[] arr = new byte[10];
        arr[0] = CasioConstants.characteristicToByte.get("CASIO_CONNECTION_PARAMETER_MANAGER");
        arr[1] = (byte)0x00;
        arr[2] = (byte)0x34;
        arr[3] = (byte)0x01;
        arr[4] = (byte)0x40;
        arr[5] = (byte)0x01;
        arr[6] = (byte)0x04;
        arr[7] = (byte)0x00;
        arr[8] = (byte)0xdc;
        arr[9] = (byte)0x05;
        writeAllFeatures(arr);
    }

    private void writeCurrentTime() {
        byte[] arr = new byte[11];
        Calendar cal = Calendar.getInstance();

        int year = cal.get(Calendar.YEAR);
        arr[0] = CasioConstants.characteristicToByte.get("CASIO_CURRENT_TIME");
        arr[1] = (byte)((year >>> 0) & 0xff);
        arr[2] = (byte)((year >>> 8) & 0xff);
        arr[3] = (byte)(1 + cal.get(Calendar.MONTH));
        arr[4] = (byte)cal.get(Calendar.DAY_OF_MONTH);
        arr[5] = (byte)cal.get(Calendar.HOUR_OF_DAY);
        arr[6] = (byte)cal.get(Calendar.MINUTE);
        arr[7] = (byte)(1 + cal.get(Calendar.SECOND));
        byte dayOfWk = (byte)(cal.get(Calendar.DAY_OF_WEEK) - 1);
        if(dayOfWk == 0)
            dayOfWk = 7;
        arr[8] = dayOfWk;
        arr[9] = (byte)(int) TimeUnit.MILLISECONDS.toSeconds(256 * cal.get(Calendar.MILLISECOND));
        arr[10] = 1; // or 0?

        writeAllFeatures(arr);
    }

    private void enableAllFeatures(boolean enable) {
        try {
            TransactionBuilder builder = createTransactionBuilder("notifyAllFeatures");
            builder.setGattCallback(this);
            enableAllFeatures(builder, enable);
            support.performImmediately(builder);
        } catch(IOException e) {
            LOG.error("Error setting notification value on all features: " + e.getMessage());
        }
    }

    private void enableAllFeatures(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), enable);
    }

    @Override
    protected void doPerform() throws IOException {

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        enableAllFeatures(builder, true);
        requestWatchName(builder);
        //writeAllFeaturesInit();
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        throw new UnsupportedOperationException("This IS the initialization class, you cannot call this method");
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if(data.length == 0)
            return true;

        if(characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if(data[0] == CasioConstants.characteristicToByte.get("CASIO_WATCH_NAME")) {
                LOG.info("Got watch name, requesting BLE features; should write CASIO_APP_INFORMATION");
                writeWatchName();
                requestBleConfiguration();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_BLE_FEATURES")) {
                LOG.info("Got BLE features, requesting BLE settings");
                requestBleSettings();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_BLE")) {
                LOG.info("Got BLE settings, requesting advertising parameters; should write BLE settings");
                writeBleSettings(data);
                requestAdvertisingParameters();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_ADVERTISE_PARAMETER_MANAGER")) {
                LOG.info("Got advertising parameters, requesting connection parameters; should write advertising parameters");
                writeAdvertisingParameters();
                requestConnectionParameters();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_CONNECTION_PARAMETER_MANAGER")) {
                LOG.info("Got connection parameters, requesting module ID; should write connection parameters");
                writeConnectionParameters();
                requestModuleId();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_MODULE_ID")) {
                LOG.info("Got module ID, requesting watch condition");
                requestWatchCondition();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_WATCH_CONDITION")) {
                LOG.info("Got watch condition, requesting version information");
                requestVersionInformation();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_VERSION_INFORMATION")) {
                LOG.info("Got version information, requesting DST watch state");
                requestDstWatchState();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_DST_WATCH_STATE")) {
                LOG.info("Got DST watch state, requesting DST setting; should write DST watch state");
                requestDstSetting();
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_DST_SETTING")) {
                LOG.info("Got DST setting, waiting...; should write DST setting and location and radio information");
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SERVICE_DISCOVERY_MANAGER")) {
                if(data[1] == 0x02) {
                    LOG.info("We need to bond here. This is actually the request for the current time.");
                    writeCurrentTime();
                    writeAllFeaturesInit();
                } else if(data[1] == 0x01) {
                    LOG.info("We need to encrypt here.");
                    writeAllFeaturesInit();
                }
            } else if(data[0] == 0x3d) {
                LOG.info("Init operation done.");
                support.setInitialized();
            }
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {

        return super.onCharacteristicRead(gatt, characteristic, status);
    }
}