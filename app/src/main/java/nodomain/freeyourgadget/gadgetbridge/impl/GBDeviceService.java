package nodomain.freeyourgadget.gadgetbridge.impl;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
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
    public void connect(GBDevice device) {
        Intent intent = createIntent().setAction(ACTION_CONNECT)
                .putExtra(GBDevice.EXTRA_DEVICE, device);
        invokeService(intent);
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
    public void onNotification(NotificationSpec notificationSpec) {
        Intent intent = createIntent().setAction(ACTION_NOTIFICATION)
                .putExtra(EXTRA_NOTIFICATION_FLAGS, notificationSpec.flags)
                .putExtra(EXTRA_NOTIFICATION_PHONENUMBER, notificationSpec.phoneNumber)
                .putExtra(EXTRA_NOTIFICATION_SENDER, notificationSpec.sender)
                .putExtra(EXTRA_NOTIFICATION_SUBJECT, notificationSpec.subject)
                .putExtra(EXTRA_NOTIFICATION_TITLE, notificationSpec.title)
                .putExtra(EXTRA_NOTIFICATION_BODY, notificationSpec.body)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationSpec.id)
                .putExtra(EXTRA_NOTIFICATION_TYPE, notificationSpec.type)
                .putExtra(EXTRA_NOTIFICATION_SOURCENAME, notificationSpec.sourceName);
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
    public void onSetCallState(CallSpec callSpec) {
        // name is actually ignored and provided by the service itself...
        Intent intent = createIntent().setAction(ACTION_CALLSTATE)
                .putExtra(EXTRA_CALL_PHONENUMBER, callSpec.number)
                .putExtra(EXTRA_CALL_COMMAND, callSpec.command);
        invokeService(intent);
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        Intent intent = createIntent().setAction(ACTION_SETMUSICSTATE)
                .putExtra(EXTRA_MUSIC_REPEAT, stateSpec.repeat)
                .putExtra(EXTRA_MUSIC_RATE, stateSpec.playRate)
                .putExtra(EXTRA_MUSIC_STATE, stateSpec.state)
                .putExtra(EXTRA_MUSIC_SHUFFLE, stateSpec.shuffle)
                .putExtra(EXTRA_MUSIC_POSITION, stateSpec.position);
        invokeService(intent);
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        Intent intent = createIntent().setAction(ACTION_SETMUSICINFO)
                .putExtra(EXTRA_MUSIC_ARTIST, musicSpec.artist)
                .putExtra(EXTRA_MUSIC_ALBUM, musicSpec.album)
                .putExtra(EXTRA_MUSIC_TRACK, musicSpec.track)
                .putExtra(EXTRA_MUSIC_DURATION, musicSpec.duration)
                .putExtra(EXTRA_MUSIC_TRACKCOUNT, musicSpec.trackCount)
                .putExtra(EXTRA_MUSIC_TRACKNR, musicSpec.trackNr);
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
    public void onAppStart(UUID uuid, boolean start) {
        Intent intent = createIntent().setAction(ACTION_STARTAPP)
                .putExtra(EXTRA_APP_UUID, uuid)
                .putExtra(EXTRA_APP_START, start);
        invokeService(intent);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        Intent intent = createIntent().setAction(ACTION_DELETEAPP)
                .putExtra(EXTRA_APP_UUID, uuid);
        invokeService(intent);
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config) {
        Intent intent = createIntent().setAction(ACTION_APP_CONFIGURE)
                .putExtra(EXTRA_APP_UUID, uuid)
                .putExtra(EXTRA_APP_CONFIG, config);
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
    public void onHeartRateTest() {
        Intent intent = createIntent().setAction(ACTION_HEARTRATE_TEST);
        invokeService(intent);
    }

    @Override
    public void onFindDevice(boolean start) {
        Intent intent = createIntent().setAction(ACTION_FIND_DEVICE)
                .putExtra(EXTRA_FIND_START, start);
        invokeService(intent);
    }

    @Override
    public void onScreenshotReq() {
        Intent intent = createIntent().setAction(ACTION_REQUEST_SCREENSHOT);
        invokeService(intent);
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        Intent intent = createIntent().setAction(ACTION_ENABLE_REALTIME_STEPS)
                .putExtra(EXTRA_BOOLEAN_ENABLE, enable);
        invokeService(intent);
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        Intent intent = createIntent().setAction(ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT)
                .putExtra(EXTRA_BOOLEAN_ENABLE, enable);
        invokeService(intent);
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        Intent intent = createIntent().setAction(ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT)
                .putExtra(EXTRA_BOOLEAN_ENABLE, enable);
        invokeService(intent);
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        Intent intent = createIntent().setAction(ACTION_ADD_CALENDAREVENT)
                .putExtra(EXTRA_CALENDAREVENT_ID, calendarEventSpec.id)
                .putExtra(EXTRA_CALENDAREVENT_TYPE, calendarEventSpec.type)
                .putExtra(EXTRA_CALENDAREVENT_TIMESTAMP, calendarEventSpec.timestamp)
                .putExtra(EXTRA_CALENDAREVENT_DURATION, calendarEventSpec.durationInSeconds)
                .putExtra(EXTRA_CALENDAREVENT_TITLE, calendarEventSpec.title)
                .putExtra(EXTRA_CALENDAREVENT_DESCRIPTION, calendarEventSpec.description);
        invokeService(intent);
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        Intent intent = createIntent().setAction(ACTION_DELETE_CALENDAREVENT)
                .putExtra(EXTRA_CALENDAREVENT_TYPE, type)
                .putExtra(EXTRA_CALENDAREVENT_ID, id);
        invokeService(intent);
    }
}
