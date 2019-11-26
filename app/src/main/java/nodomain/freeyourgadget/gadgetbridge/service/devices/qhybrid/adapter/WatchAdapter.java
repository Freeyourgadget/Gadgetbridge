package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public abstract class WatchAdapter {
    private QHybridSupport deviceSupport;

    public WatchAdapter(QHybridSupport deviceSupport){
        this.deviceSupport = deviceSupport;
    }

    public QHybridSupport getDeviceSupport(){
        return this.deviceSupport;
    }

    public Context getContext(){
        return getDeviceSupport().getContext();
    }

    public abstract void initialize();

    public abstract void playPairingAnimation();
    public abstract void playNotification(NotificationConfiguration config);
    public abstract void setTime();
    public abstract void overwriteButtons(String buttonConfigJson);
    public abstract void setActivityHand(double progress);
    public abstract void setHands(short hour, short minute);
    public abstract void vibrate(PlayNotificationRequest.VibrationType vibration);
    public abstract void vibrateFindMyDevicePattern();
    public abstract void requestHandsControl();
    public abstract void releaseHandsControl();
    public abstract void setStepGoal(int stepGoal);
    public abstract void setVibrationStrength(short strength);
    public abstract void syncNotificationSettings();
    public abstract void onTestNewFunction();
    public abstract void setTimezoneOffsetMinutes(short offset);

    public abstract boolean supportsFindDevice();
    public abstract boolean supportsExtendedVibration();
    public abstract boolean supportsActivityHand();

    public String getModelName() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return "Q Commuter";
            case "HL.0.0":
                return "Q Activist";
            case "DN.1.0":
                return "Hybrid HR Collider";
        }
        return "unknwon Q";
    }

    public abstract void onFetchActivityData();

    public abstract void onSetAlarms(ArrayList<? extends Alarm> alarms);

    public abstract boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status){};


    public String arrayToString(byte[] bytes) {
        if (bytes.length == 0) return "";
        StringBuilder s = new StringBuilder();
        final String chars = "0123456789ABCDEF";
        for (byte b : bytes) {
            s.append(chars.charAt((b >> 4) & 0xF)).append(chars.charAt(b & 0xF)).append(" ");
        }
        return s.substring(0, s.length() - 1) + "\n";
    }
}
