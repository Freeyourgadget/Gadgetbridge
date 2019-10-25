package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.misfit.MisfitWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public abstract class WatchAdapter {
    private QHybridSupport deviceSupport;

    public WatchAdapter(QHybridSupport deviceSupport){
        this.deviceSupport = deviceSupport;
    }

    protected QHybridSupport getDeviceSupport(){
        return this.deviceSupport;
    }

    protected Context getContext(){
        return getDeviceSupport().getContext();
    }

    public abstract void initialize();

    public abstract void playPairingAnimation();
    public abstract void playNotification(PackageConfig config);
    public abstract void setTime();
    public abstract void overwriteButtons();
    public abstract void setActivityHand(double progress);
    public abstract void setHands(short hour, short minute);
    public abstract void vibrate(PlayNotificationRequest.VibrationType vibration);
    public abstract void vibrateFindMyDevicePattern();
    public abstract void requestHandsControl();
    public abstract void releaseHandsControl();
    public abstract void setStepGoal(int stepGoal);
    public abstract void setVibrationStrength(short strength);

    public abstract boolean supportsExtendedVibration();
    public abstract boolean supportsActivityHand();
    public abstract String getModelName();

    public abstract void onFetchActivityData();

    public abstract boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

}
