package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;

/**
 * Wraps another device support instance and supports busy-checking and throttling of events.
 */
public class ServiceDeviceSupport implements DeviceSupport {

    static enum Flags {
        THROTTLING,
        BUSY_CHECKING,
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeviceSupport.class);

    private static final long THROTTLING_THRESHOLD = 1000; // throttle multiple events in between one second
    private final DeviceSupport delegate;

    private long lastNotificationTime = 0;
    private String lastNotificationKind;
    private final EnumSet<Flags> flags;

    public ServiceDeviceSupport(DeviceSupport delegate, EnumSet<Flags> flags) {
        this.delegate = delegate;
        this.flags = flags;
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        delegate.setContext(gbDevice, btAdapter, context);
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean connect() {
        return delegate.connect();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public GBDevice getDevice() {
        return delegate.getDevice();
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return delegate.getBluetoothAdapter();
    }

    @Override
    public Context getContext() {
        return delegate.getContext();
    }

    @Override
    public boolean useAutoConnect() {
        return delegate.useAutoConnect();
    }

    @Override
    public void pair() {
        delegate.pair();
    }

    private boolean checkBusy(String notificationKind) {
        if (!flags.contains(Flags.BUSY_CHECKING)) {
            return false;
        }
        if (getDevice().isBusy()) {
            LOG.info("Ignoring " + notificationKind + " because we're busy with " + getDevice().getBusyTask());
            return true;
        }
        return false;
    }

    private boolean checkThrottle(String notificationKind) {
        if (!flags.contains(Flags.THROTTLING)) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastNotificationTime) < THROTTLING_THRESHOLD) {
            if (notificationKind != null && notificationKind.equals(lastNotificationKind)) {
                LOG.info("Ignoring " + notificationKind + " because of throttling threshold reached");
                return true;
            }
        }
        lastNotificationTime = currentTime;
        lastNotificationKind = notificationKind;
        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (checkBusy("generic notification") || checkThrottle("generic notification")) {
            return;
        }
        delegate.onNotification(notificationSpec);
    }

    @Override
    public void onSetTime() {
        if (checkBusy("set time") || checkThrottle("set time")) {
            return;
        }
        delegate.onSetTime();
    }

    // No throttling for the other events

    @Override
    public void onSetCallState(String number, String name, ServiceCommand command) {
        if (checkBusy("set call state")) {
            return;
        }
        delegate.onSetCallState(number, name, command);
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        if (checkBusy("set music info")) {
            return;
        }
        delegate.onSetMusicInfo(artist, album, track);
    }

    @Override
    public void onInstallApp(Uri uri) {
        if (checkBusy("install app")) {
            return;
        }
        delegate.onInstallApp(uri);
    }

    @Override
    public void onAppInfoReq() {
        if (checkBusy("app info request")) {
            return;
        }
        delegate.onAppInfoReq();
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        if (checkBusy("app start")) {
            return;
        }
        delegate.onAppStart(uuid, start);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        if (checkBusy("app delete")) {
            return;
        }
        delegate.onAppDelete(uuid);
    }

    @Override
    public void onFetchActivityData() {
        if (checkBusy("fetch activity data")) {
            return;
        }
        delegate.onFetchActivityData();
    }

    @Override
    public void onReboot() {
        if (checkBusy("reboot")) {
            return;
        }
        delegate.onReboot();
    }

    @Override
    public void onFindDevice(boolean start) {
        if (checkBusy("find device")) {
            return;
        }
        delegate.onFindDevice(start);
    }

    @Override
    public void onScreenshotReq() {
        if (checkBusy("request screenshot")) {
            return;
        }
        delegate.onScreenshotReq();
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        if (checkBusy("set alarms")) {
            return;
        }
        delegate.onSetAlarms(alarms);
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        if (checkBusy("enable realtime steps: " + enable)) {
            return;
        }
        delegate.onEnableRealtimeSteps(enable);
    }
}
