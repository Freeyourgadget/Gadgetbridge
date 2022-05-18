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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vesc;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.vesc.VescCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class VescDeviceSupport extends VescBaseDeviceSupport {
    BluetoothGattCharacteristic serialWriteCharacteristic, serialReadCharacteristic;

    public static final String COMMAND_SET_RPM = "nodomain.freeyourgadget.gadgetbridge.vesc.command.SET_RPM";
    public static final String COMMAND_SET_CURRENT = "nodomain.freeyourgadget.gadgetbridge.vesc.command.SET_CURRENT";
    public static final String COMMAND_SET_BREAK_CURRENT = "nodomain.freeyourgadget.gadgetbridge.vesc.command.SET_BREAK_CURRENT";
    public static final String COMMAND_GET_VALUES = "nodomain.freeyourgadget.gadgetbridge.vesc.command.GET_VALUES";
    public static final String EXTRA_RPM = "EXTRA_RPM";
    public static final String EXTRA_CURRENT = "EXTRA_CURRENT";
    public static final String EXTRA_VOLTAGE = "EXTRA_VOLTAGE";

    public static final String ACTION_GOT_VALUES = "nodomain.freeyourgadget.gadgetbridge.vesc.action.GOT_VALUES";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private DeviceType deviceType;

    private ByteBuffer responseBuffer = ByteBuffer.allocate(100);

    public VescDeviceSupport(DeviceType type) {
        super();
        responseBuffer.order(ByteOrder.BIG_ENDIAN);

        deviceType = type;

        if (type == DeviceType.VESC_NRF) {
            addSupportedService(UUID.fromString(VescCoordinator.UUID_SERVICE_SERIAL_NRF));
        } else if (type == DeviceType.VESC_HM10) {
            addSupportedService(UUID.fromString(VescCoordinator.UUID_SERVICE_SERIAL_HM10));
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        super.onFetchRecordedData(dataTypes);
        getValues();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        logger.debug("initializing device");

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        initBroadcast();

        if (deviceType == DeviceType.VESC_NRF) {
            this.serialWriteCharacteristic = getCharacteristic(UUID.fromString(VescCoordinator.UUID_CHARACTERISTIC_SERIAL_TX_NRF));
            this.serialReadCharacteristic = getCharacteristic(UUID.fromString(VescCoordinator.UUID_CHARACTERISTIC_SERIAL_RX_NRF));
        } else if (deviceType == DeviceType.VESC_HM10) {
            this.serialWriteCharacteristic = getCharacteristic(UUID.fromString(VescCoordinator.UUID_CHARACTERISTIC_SERIAL_TX_HM10));
            this.serialReadCharacteristic = getCharacteristic(UUID.fromString(VescCoordinator.UUID_CHARACTERISTIC_SERIAL_RX_HM10));
        }

        builder.notify(this.serialReadCharacteristic, true);

        return builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        getValues();

        return true;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        handleRxCharacteristic(characteristic);

        return true;
    }

    private void handleRxCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic != serialReadCharacteristic) return;

        responseBuffer.put(characteristic.getValue());
        short length = 0;
        int oldPosition = responseBuffer.position();
        responseBuffer.position(0);
        byte lengthType = responseBuffer.get();
        if (lengthType == 2) {
            length = responseBuffer.get();
        } else if (lengthType == 3) {
            length = responseBuffer.getShort();
        } else {
            return;
        }
        if (length == oldPosition - 5) {
            // whole message transmitted
            responseBuffer.position(oldPosition);
            handleResponseBuffer(responseBuffer);

            oldPosition = 0;
        }
        responseBuffer.position(oldPosition);
    }

    private void handleResponseBuffer(ByteBuffer responseBuffer) {
        int bufferLength = responseBuffer.position();
        int payloadStartPosition = responseBuffer.get(0);

        byte[] payload = new byte[bufferLength - 3 - payloadStartPosition];
        System.arraycopy(responseBuffer.array(), payloadStartPosition, payload, 0, payload.length);

        int actualCrc = CheckSums.getCRC16(payload, 0);
        int expectedCrc = responseBuffer.getShort(bufferLength - 3);

        byte responseType = payload[0];
        if (responseType == 0x04) {
            handleResponseValues(responseBuffer);
        }
    }

    private void handleResponseValues(ByteBuffer valueBuffer) {
        valueBuffer.position(3);
        float temp_mos = buffer_get_float16(valueBuffer, 1e1);
        float temp_motor = buffer_get_float16(valueBuffer, 1e1);
        float current_motor = buffer_get_float32(valueBuffer, 1e2);
        float current_in = buffer_get_float32(valueBuffer, 1e2);
        float id = buffer_get_float32(valueBuffer, 1e2);
        float iq = buffer_get_float32(valueBuffer, 1e2);
        float duty_now = buffer_get_float16(valueBuffer, 1e3);
        float rpm = buffer_get_float32(valueBuffer, 1e0);
        float v_in = buffer_get_float16(valueBuffer, 1e1);
        float amp_hours = buffer_get_float32(valueBuffer, 1e4);
        float amp_hours_charged = buffer_get_float32(valueBuffer, 1e4);
        float watt_hours = buffer_get_float32(valueBuffer, 1e4);
        float watt_hours_charged = buffer_get_float32(valueBuffer, 1e4);
        float tachometer = buffer_get_int32(valueBuffer);
        float tachometer_abs = buffer_get_int32(valueBuffer);

        handleBatteryVoltage(v_in);

        Intent intent = new Intent(ACTION_GOT_VALUES);
        intent.putExtra(EXTRA_VOLTAGE, v_in);
    }



    void handleBatteryVoltage(float voltage){
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        float minimalVoltage = Float.parseFloat(prefs.getString(DeviceSettingsPreferenceConst.PREF_VESC_MINIMUM_VOLTAGE, "-1"));
        float maximalVoltage = Float.parseFloat(prefs.getString(DeviceSettingsPreferenceConst.PREF_VESC_MAXIMUM_VOLTAGE, "-1"));

        if(minimalVoltage == -1){
            return;
        }
        if(maximalVoltage == -1){
            return;
        }

        float voltageAboveMinimum = voltage - minimalVoltage;
        float voltageRange = maximalVoltage - minimalVoltage;
        float fullness = voltageAboveMinimum / voltageRange;

        int fullnessPercent = (int)(fullness * 100);
        fullnessPercent = Math.max(fullnessPercent, 0);
        fullnessPercent = Math.min(fullnessPercent, 100);

        getDevice().setBatteryLevel(fullnessPercent);
        getDevice().setBatteryVoltage(voltage);
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    float buffer_get_float16(ByteBuffer buffer, double scale){
        return (float) (buffer.getShort() / scale);
    }

    float buffer_get_float32(ByteBuffer buffer, double scale){
        return (float) (buffer.getInt() / scale);
    }

    int buffer_get_int32(ByteBuffer buffer){
        return buffer.getInt();
    }

    @Override
    public void onTestNewFunction() {
        getValues();
        // getDecodedADC();
    }

    private void getDecodedADC() {
        buildAndQueryPacket(CommandType.COMM_GET_DECODED_ADC);
    }

    private void initBroadcast() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(COMMAND_SET_RPM);
        filter.addAction(COMMAND_SET_CURRENT);
        filter.addAction(COMMAND_SET_BREAK_CURRENT);
        filter.addAction(COMMAND_GET_VALUES);

        broadcastManager.registerReceiver(commandReceiver, filter);
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
    }

    BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(COMMAND_SET_RPM)) {
                VescDeviceSupport.this.setRPM(
                        intent.getIntExtra(EXTRA_RPM, 0)
                );
            } else if (intent.getAction().equals(COMMAND_SET_BREAK_CURRENT)) {
                VescDeviceSupport.this.setBreakCurrent(
                        intent.getIntExtra(EXTRA_CURRENT, 0)
                );
            } else if (intent.getAction().equals(COMMAND_SET_CURRENT)) {
                VescDeviceSupport.this.setCurrent(
                        intent.getIntExtra(EXTRA_CURRENT, 0)
                );
            } else if (intent.getAction().equals(COMMAND_GET_VALUES)) {
                VescDeviceSupport.this.getValues();
            }
        }
    };

    public void setCurrent(int currentMillisAmperes) {
        buildAndQueryPacket(CommandType.COMM_SET_CURRENT, currentMillisAmperes);
    }

    public void setBreakCurrent(int breakCurrentMillisAmperes) {
        buildAndQueryPacket(CommandType.COMM_SET_CURRENT_BRAKE, breakCurrentMillisAmperes);
    }

    public void getValues() {
        buildAndQueryPacket(CommandType.COMM_GET_VALUES);
    }

    public void setRPM(int rpm) {
        buildAndQueryPacket(CommandType.COMM_SET_RPM, rpm);
    }

    public void buildAndQueryPacket(CommandType commandType, Object... args) {
        byte[] data = buildPacket(commandType, args);
        queryPacket(data);
    }

    public void queryPacket(byte[] data) {
        new TransactionBuilder("write serial packet")
                .write(this.serialWriteCharacteristic, data)
                .queue(getQueue());
    }

    public byte[] buildPacket(CommandType commandType, Object... args) {
        int dataLength = 0;
        for (Object arg : args) {
            if (arg instanceof Integer) dataLength += 4;
            else if (arg instanceof Short) dataLength += 2;
        }
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);

        for (Object arg : args) {
            if (arg instanceof Integer) buffer.putInt((Integer) arg);
            if (arg instanceof Short) buffer.putShort((Short) arg);
        }

        return buildPacket(commandType, buffer.array());
    }

    public byte[] buildPacket(CommandType commandType, byte[] data) {
        return buildPacket(commandType.getCommandByte(), data);
    }

    private byte[] buildPacket(byte commandByte, byte[] data) {
        byte[] contents = new byte[data.length + 1];
        contents[0] = commandByte;
        System.arraycopy(data, 0, contents, 1, data.length);
        return buildPacket(contents);
    }

    private byte[] buildPacket(byte[] contents) {
        int dataLength = contents.length;
        ByteBuffer buffer = ByteBuffer.allocate(dataLength + (dataLength < 256 ? 5 : 6));
        if (dataLength < 256) {
            buffer.put((byte) 0x02);
            buffer.put((byte) dataLength);
        } else {
            buffer.put((byte) 0x03);
            buffer.putShort((short) dataLength);
        }
        buffer.put(contents);
        buffer.putShort((short) CheckSums.getCRC16(contents, 0));
        buffer.put((byte) 0x03);

        return buffer.array();
    }
}
