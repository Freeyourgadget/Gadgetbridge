package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message.GetSensorRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message.Response;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message.SetSensorRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.SensorState;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class BinarySensorSupport extends BinarySensorBaseSupport {
    final public static String BINARY_SENSOR_SERVICE_UUID = "0000183b-0000-1000-8000-00805f9b34fb";
    final public static String BINARY_SENSOR_CONTROL_CHARACTERISTIC_UUID = "00002b2b-0000-1000-8000-00805f9b34fb";
    final public static String BINARY_SENSOR_RESPONSE_CHARACTERISTIC_UUID = "00002b2c-0000-1000-8000-00805f9b34fb";

    final public static String ACTION_SENSOR_STATE_CHANGED = "nodomain.freeyourgadget.gadgetbridge.binary_sensor.STATE_CHANGED";
    final public static String ACTION_SENSOR_STATE_REQUEST = "nodomain.freeyourgadget.gadgetbridge.binary_sensor.STATE_REQUEST";

    private static final Logger logger = LoggerFactory.getLogger(BinarySensorSupport.class);

    private nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState sensorState = null;
    private int sensorCount = -1;

    public BinarySensorSupport() {
        super(logger);
        addSupportedService(UUID.fromString(BINARY_SENSOR_SERVICE_UUID));

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(
                        stateRequestReceiver,
                        new IntentFilter(ACTION_SENSOR_STATE_REQUEST)
                );
    }

    @Override
    public void dispose() {
        super.dispose();

        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(stateRequestReceiver);
    }

    BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendStateChangeIntent(false);
        }
    };

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().toString().equals(BINARY_SENSOR_RESPONSE_CHARACTERISTIC_UUID)) {
            handleResponseValue(characteristic.getValue());
            return true;
        }

        return false;
    }

    Response decodeResponse(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.get(); // split packet header
        buffer.get(); // RFU
        byte messageIdByte = buffer.get();
        buffer.get(); // RFU
        int parameterCount = buffer.get();

        Parameter[] parameters = new Parameter[parameterCount];

        MessageId messageId = MessageId.fromMessageIdByte(messageIdByte);
        for (int i = 0; i < parameterCount; i++) {
            byte parameterIdByte = buffer.get();
            byte payloadLength = buffer.get();
            buffer.get(); // RFU
            buffer.get(); // RFU

            ParameterId parameterId = ParameterId.fromParameterIdByte(parameterIdByte);

            byte[] payload = new byte[payloadLength];
            buffer.get(payload);

            parameters[i] = Parameter.decode(parameterId, payload);
        }

        return new Response(
                messageId,
                parameters
        );
    }

    void sendStateChangeIntent(boolean sendGlobally){
        Intent intent = new Intent(ACTION_SENSOR_STATE_CHANGED);

        intent.putExtra("EXTRA_SENSOR_CLOSED", sensorState == nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState.SENSOR_STATE_CLOSED);
        intent.putExtra("EXTRA_SENSOR_COUNT", sensorCount);

        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        if(sendGlobally) {
            getContext().sendBroadcast(intent);
        }
    }

    void handleResponseValue(byte[] value) {
        Response response = decodeResponse(value);

        for (Parameter parameter : response.getParameters()) {
            if (parameter instanceof SensorState) {
                if(getDevice().getState() != GBDevice.State.INITIALIZED){
                    new TransactionBuilder("set device state")
                            .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()))
                            .queue(getQueue());
                }

                SensorState stateParameter = (SensorState) parameter;
                logger.debug("sensor state: " + stateParameter.getSensorState() + "  count: " + stateParameter.getCount());

                this.sensorState = stateParameter.getSensorState();
                this.sensorCount = stateParameter.getCount();

                sendStateChangeIntent(true);
            }
        }
    }

    private void sendPacketToDevice(byte[] data, TransactionBuilder builder) {
        byte[] fullData = new byte[data.length + 1];
        fullData[0] = 0x00;
        System.arraycopy(data, 0, fullData, 1, data.length);

        builder.write(getCharacteristic(UUID.fromString(BINARY_SENSOR_CONTROL_CHARACTERISTIC_UUID)), fullData);
    }

    private void sendPacketToDevice(byte[] data) {
        TransactionBuilder builder = new TransactionBuilder("BSS control");
        sendPacketToDevice(data, builder);
        builder.queue(getQueue());
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            GB.toast("error setting indication", Toast.LENGTH_LONG, GB.ERROR);
        }
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        logger.debug("initializing device");

        builder
                .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()))
                .notify(getCharacteristic(UUID.fromString(BINARY_SENSOR_RESPONSE_CHARACTERISTIC_UUID)), true)
        ;

        SetSensorRequest setSensorRequest = new SetSensorRequest(SensorType.SENSOR_TYPE_OPENING_CLOSING, ReportState.REPORT_STATUS_ENABLED);
        GetSensorRequest getSensorRequest = new GetSensorRequest(SensorType.SENSOR_TYPE_OPENING_CLOSING);

        sendPacketToDevice(getSensorRequest.encode(), builder);
        sendPacketToDevice(setSensorRequest.encode(), builder);

        return builder;
    }
}
