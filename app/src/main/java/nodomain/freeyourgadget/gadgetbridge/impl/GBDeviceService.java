package nodomain.freeyourgadget.gadgetbridge.impl;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class GBDeviceService implements DeviceService {
    protected final Context mContext;
    protected final Class<? extends Service> mServiceClass;

    public GBDeviceService(Context context) {
        mContext = context;
        mServiceClass = DeviceCommunicationService.class;
    }

    protected Intent createIntent() {
        Intent startIntent = new Intent(mContext, mServiceClass);
        return startIntent;
    }

    protected void invokeService(Intent intent) {
        mContext.startService(intent);
    }

    protected void stopService(Intent intent) {
        mContext.stopService(intent);
    }

    @Override
    public void start() {
        Intent intent = createIntent().setAction(ACTION_START);
        invokeService(intent);
    }

    @Override
    public void connect() {
        connect(null, false);
    }

    @Override
    public void connect(@Nullable String deviceAddress) {
        connect(deviceAddress, false);
    }

    @Override
    public void connect(@Nullable String deviceAddress, boolean performPair) {
        Intent intent = createIntent().setAction(ACTION_CONNECT)
                .putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
                .putExtra(EXTRA_PERFORM_PAIR, performPair);
        invokeService(intent);
    }

    @Override
    public void disconnect() {
        Intent intent = createIntent().setAction(ACTION_DISCONNECT);
        invokeService(intent);
    }

    @Override
    public void quit() {
        Intent intent = createIntent();
        stopService(intent);
    }

    @Override
    public void requestDeviceInfo() {
        Intent intent = createIntent().setAction(ACTION_REQUEST_DEVICEINFO);
        invokeService(intent);
    }

    @Override
    public void onSMS(String from, String body) {
        Intent intent = createIntent().setAction(ACTION_NOTIFICATION_SMS)
                .putExtra(EXTRA_NOTIFICATION_SENDER, from)
                .putExtra(EXTRA_NOTIFICATION_BODY, body);
        invokeService(intent);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        Intent intent = createIntent().setAction(ACTION_NOTIFICATION_EMAIL)
                .putExtra(EXTRA_NOTIFICATION_SENDER, from)
                .putExtra(EXTRA_NOTIFICATION_SUBJECT, subject)
                .putExtra(EXTRA_NOTIFICATION_BODY, body);
        invokeService(intent);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        Intent intent = createIntent().setAction(ACTION_NOTIFICATION_GENERIC)
                .putExtra(EXTRA_NOTIFICATION_TITLE, title)
                .putExtra(EXTRA_NOTIFICATION_BODY, details);
        invokeService(intent);
    }

    @Override
    public void onSetTime() {
        Intent intent = createIntent().setAction(ACTION_SETTIME);
        invokeService(intent);
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        Intent intent = createIntent().setAction(ACTION_SET_ALARMS)
                .putParcelableArrayListExtra(EXTRA_ALARMS, alarms);
        invokeService(intent);
    }

    @Override
    public void onSetCallState(String number, String name, ServiceCommand command) {
        // name is actually ignored and provided by the service itself...
        Intent intent = createIntent().setAction(ACTION_CALLSTATE)
                .putExtra(EXTRA_CALL_PHONENUMBER, number)
                .putExtra(EXTRA_CALL_COMMAND, command);
        invokeService(intent);
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        Intent intent = createIntent().setAction(ACTION_SETMUSICINFO)
                .putExtra(EXTRA_MUSIC_ARTIST, artist)
                .putExtra(EXTRA_MUSIC_TRACK, track);
        invokeService(intent);
    }

    @Override
    public void onInstallApp(Uri uri) {
        Intent intent = createIntent().setAction(ACTION_INSTALL)
                .putExtra(EXTRA_URI, uri);
        invokeService(intent);
    }

    @Override
    public void onAppInfoReq() {
        Intent intent = createIntent().setAction(ACTION_REQUEST_APPINFO);
        invokeService(intent);
    }

    @Override
    public void onAppStart(UUID uuid) {
        Intent intent = createIntent().setAction(ACTION_STARTAPP)
                .putExtra(EXTRA_APP_UUID, uuid);
        invokeService(intent);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        Intent intent = createIntent().setAction(ACTION_DELETEAPP)
                .putExtra(EXTRA_APP_UUID, uuid);
        invokeService(intent);
    }

    @Override
    public void onFetchActivityData() {
        Intent intent = createIntent().setAction(ACTION_FETCH_ACTIVITY_DATA);
        invokeService(intent);
    }

    @Override
    public void onReboot() {
        Intent intent = createIntent().setAction(ACTION_REBOOT);
        invokeService(intent);
    }

    @Override
    public void onFindDevice(boolean start) {
        Intent intent = createIntent().setAction(ACTION_FIND_DEVICE)
                .putExtra(EXTRA_APP_UUID, start);
        invokeService(intent);
    }

    @Override
    public void onScreenshotReq() {
        Intent intent = createIntent().setAction(ACTION_REQUEST_SCREENSHOT);
        invokeService(intent);
    }
}
