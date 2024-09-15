package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;

public class CommunicatorV1 implements ICommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicatorV1.class);

    public static final UUID UUID_SERVICE_GARMIN_GFDI = UUID.fromString("6A4E2401-667B-11E3-949A-0800200C9A66");

    private final GarminSupport mSupport;

    public CommunicatorV1(final GarminSupport garminSupport) {
        this.mSupport = garminSupport;
    }

    @Override
    public void onMtuChanged(final int mtu) {

    }

    @Override
    public void initializeDevice(final TransactionBuilder builder) {
        LOG.error("Initialization is not implemented for V1");
    }

    @Override
    public void sendMessage(final String taskName, final byte[] message) {

    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public void onHeartRateTest() {
        LOG.error("onHeartRateTest is not implemented for V1");
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        LOG.error("onEnableRealtimeHeartRateMeasurement is not implemented for V1");
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        LOG.error("onEnableRealtimeSteps is not implemented for V1");
    }
}
