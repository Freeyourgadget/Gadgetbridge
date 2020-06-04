/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
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
    public abstract void onInstallApp(Uri uri);

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

    public abstract void onSendConfiguration(String config);

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

    public void setCommuteMenuMessage(String message, boolean finished) {
    }

    public void setMusicInfo(MusicSpec musicSpec) {
    }

    public void setMusicState(MusicStateSpec stateSpec) {
    }

    public void setWidgetContent(String widgetID, String content, boolean render) {
    }

    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    }

    public void updateWidgets() {
    }

    public void onSetCallState(CallSpec callSpec) {
    }

    public void onFindDevice(boolean start) {
    }

    public void onSendWeather(WeatherSpec weatherSpec) {
    }

    public void setBackgroundImage(byte[] pixels) {
    }

    public void onDeleteNotification(int id) {
    }
}
