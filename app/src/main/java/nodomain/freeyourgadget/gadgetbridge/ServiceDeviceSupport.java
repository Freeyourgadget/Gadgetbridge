package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Wraps another device support instance and supports busy-checking and throttling of events.
 */
public class ServiceDeviceSupport implements DeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeviceSupport.class);
    private static final long THROTTLING_THRESHOLD = 1000; // throttle multiple events in between one second

    private final DeviceSupport delegate;
    private long lastNoficationTime = 0;
    private String lastNotificationKind;

    public ServiceDeviceSupport(DeviceSupport delegate) {
        this.delegate = delegate;
    }

    @Override
    public void initialize(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        delegate.initialize(gbDevice, btAdapter, context);
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
        if (getDevice().isBusy()) {
            LOG.info("Ignoring " + notificationKind + " because we're busy with " + getDevice().getBusyTask());
        }
        return false;
    }

    private boolean checkThrottle(String notificationKind) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastNoficationTime) < THROTTLING_THRESHOLD) {
            if (notificationKind != null && notificationKind.equals(lastNotificationKind)) {
                LOG.info("Ignoring " + notificationKind + " because of throttling threshold reached");
                return true;
            }
        }
        lastNoficationTime = currentTime;
        lastNotificationKind = notificationKind;
        return false;
    }

    @Override
    public void onSMS(String from, String body) {
        if (checkBusy("sms") || checkThrottle("sms")) {
            return;
        }
        delegate.onSMS(from, body);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        if (checkBusy("email") || checkThrottle("email")) {
            return;
        }
        delegate.onEmail(from, subject, body);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        if (checkBusy("generic notification") || checkThrottle("generic notification")) {
            return;
        }
        delegate.onGenericNotification(title, details);
    }

    @Override
    public void onSetTime(long ts) {
        if (checkBusy("set time") || checkThrottle("set time")) {
            return;
        }
        delegate.onSetTime(ts);
    }

    // No throttling for the other events

    @Override
    public void onSetCallState(String number, String name, GBCommand command) {
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
    public void onFirmwareVersionReq() {
        if (checkBusy("firmware version request")) {
            return;
        }
        delegate.onFirmwareVersionReq();
    }

    @Override
    public void onBatteryInfoReq() {
        if (checkBusy("battery info request")) {
            return;
        }
        delegate.onBatteryInfoReq();
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
    public void onAppStart(UUID uuid) {
        if (checkBusy("app start")) {
            return;
        }
        delegate.onAppStart(uuid);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        if (checkBusy("app delete")) {
            return;
        }
        delegate.onAppDelete(uuid);
    }

    @Override
    public void onPhoneVersion(byte os) {
        if (checkBusy("phone version")) {
            return;
        }
        delegate.onPhoneVersion(os);
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
}
