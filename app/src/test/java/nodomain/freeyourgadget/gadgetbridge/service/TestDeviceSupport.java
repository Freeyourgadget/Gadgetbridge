package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationKind;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

public class TestDeviceSupport extends AbstractDeviceSupport {

    public TestDeviceSupport() {
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        gbDevice = new GBDevice(getClass().getName(), "Test Device", DeviceType.TEST);
        super.setContext(gbDevice, btAdapter, context);
    }

    @Override
    public boolean connect() {
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());
        return true;
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void pair() {

    }

    @Override
    public void onSMS(String from, String body) {

    }

    @Override
    public void onEmail(String from, String subject, String body) {

    }

    @Override
    public void onGenericNotification(String title, String details, int handle, NotificationKind notification_kind) {
        
    }

    @Override
    public void onSetTime() {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(@Nullable String number, @Nullable String name, ServiceCommand command) {

    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onFetchActivityData() {

    }

    @Override
    public void onReboot() {

    }

    @Override
    public void onFindDevice(boolean start) {

    }

    @Override
    public void onScreenshotReq() {

    }
}
