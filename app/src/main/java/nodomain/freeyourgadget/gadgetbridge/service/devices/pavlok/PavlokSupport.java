package nodomain.freeyourgadget.gadgetbridge.service.devices.pavlok;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ReadAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.nut.NutSupport;

public class PavlokSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(PavlokSupport.class);

    private static final String ACTION_ZAP = "nodomain.freeyourgadget.gadgetbridge.devices.pavlok.ACTION_ZAP";

    private static final UUID UUID_PAVLOK_SERVICE = UUID.fromString("156e1000-a300-4fea-897b-86f698d74461");
    private static final UUID UUID_PAVLOK_CHARACTERISTIC_ZAP = UUID.fromString("00001003-0000-1000-8000-00805f9b34fb");

    BroadcastReceiver zapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intensity = intent.getIntExtra("EXTRA_INTENSITY", 0);
            if((intensity <= 0) || (intensity > 100)){
                LOG.error("EXTRA_INTENSITY missing, <= 0 or > 100");
                return;
            }

            sendZap(intensity);
        }
    };

    private void sendZap(int intensity){
        byte[] packet = new byte[]{(byte) 0x89, (byte) intensity};

        new TransactionBuilder("send pavlok zap")
                .write(getCharacteristic(UUID_PAVLOK_CHARACTERISTIC_ZAP), packet)
                .queue(getQueue());
    }

    public PavlokSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(UUID_PAVLOK_SERVICE);
    }

    @Override
    public void dispose() {
        getContext().unregisterReceiver(zapReceiver);
        super.dispose();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        getContext().registerReceiver(
                zapReceiver,
                new IntentFilter(ACTION_ZAP), Context.RECEIVER_EXPORTED
        );

        return builder
                .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()))
                .add(new ReadAction(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)))
                .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if(characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)){
            int level = (int) characteristic.getValue()[0];
            getDevice().setBatteryLevel(level);
            getDevice().sendDeviceUpdateIntent(getContext());
        }

        return super.onCharacteristicRead(gatt, characteristic, status);
    }
}
