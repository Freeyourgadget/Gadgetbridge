package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;

public class CommunicatorV1 implements ICommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicatorV1.class);

    // Seen in Vivovit / Forerunner 620
    public static final UUID UUID_SERVICE_GARMIN_GFDI_V0 = UUID.fromString("9B012401-BC30-CE9A-E111-0F67E491ABDE");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_GFDI_V0_SEND = UUID.fromString("DF334C80-E6A7-D082-274D-78FC66F85E16");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_GFDI_V0_RECEIVE = UUID.fromString("4ACBCD28-7425-868E-F447-915C8F00D0CB");

    // Seen in Vivomove HR
    public static final UUID UUID_SERVICE_GARMIN_GFDI_V1 = UUID.fromString("6A4E2401-667B-11E3-949A-0800200C9A66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_GFDI_V1_SEND = UUID.fromString("6A4E4C80-667B-11E3-949A-0800200C9A66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_GFDI_V1_RECEIVE = UUID.fromString("6A4ECD28-667B-11E3-949A-0800200C9A66");

    private BluetoothGattCharacteristic characteristicSend;
    private BluetoothGattCharacteristic characteristicReceive;

    private final GarminSupport mSupport;

    public final CobsCoDec cobsCoDec;
    public int maxWriteSize = 20;

    public CommunicatorV1(final GarminSupport garminSupport) {
        this.mSupport = garminSupport;
        this.cobsCoDec = new CobsCoDec();
    }

    @Override
    public void onMtuChanged(final int mtu) {
        maxWriteSize = mtu - 3;
    }

    @Override
    public boolean initializeDevice(final TransactionBuilder builder) {
        characteristicSend = mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_GFDI_V1_SEND);
        characteristicReceive = mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_GFDI_V1_RECEIVE);

        if (characteristicSend == null || characteristicReceive == null) {
            characteristicSend = mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_GFDI_V0_SEND);
            characteristicReceive = mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_GFDI_V0_RECEIVE);
        }

        if (characteristicSend == null || characteristicReceive == null) {
            LOG.warn("Failed to find V0/V1 GFDI characteristics");
            return false;
        }

        builder.notify(characteristicReceive, true);

        LOG.debug("Initializing as Garmin V1");

        return true;
    }

    @Override
    public void sendMessage(final String taskName, final byte[] message) {
        if (null == message)
            return;

        final byte[] payload = cobsCoDec.encode(message);

        final TransactionBuilder builder = new TransactionBuilder(taskName);
        int remainingBytes = payload.length;
        if (remainingBytes > maxWriteSize - 1) {
            int position = 0;
            while (remainingBytes > 0) {
                final byte[] fragment = Arrays.copyOfRange(payload, position, position + Math.min(remainingBytes, maxWriteSize - 1));
                builder.write(characteristicSend, fragment);
                position += fragment.length;
                remainingBytes -= fragment.length;
            }
        } else {
            builder.write(characteristicSend, payload);
        }
        builder.queue(this.mSupport.getQueue());
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(characteristicReceive.getUuid())) {
            this.cobsCoDec.receivedBytes(characteristic.getValue());
            this.mSupport.onMessage(this.cobsCoDec.retrieveMessage());

            return true;
        }

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
