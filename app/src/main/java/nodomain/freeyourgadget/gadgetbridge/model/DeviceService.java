package nodomain.freeyourgadget.gadgetbridge.model;

import android.support.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

/**
 *
 */
public interface DeviceService extends EventHandler {
    static final String PREFIX = "nodomain.freeyourgadget.gadgetbridge.devices";

    static final String ACTION_START = PREFIX + ".action.start";
    static final String ACTION_CONNECT = PREFIX + ".action.connect";
    static final String ACTION_NOTIFICATION_GENERIC = PREFIX + ".action.notification_generic";
    static final String ACTION_NOTIFICATION_SMS = PREFIX + ".action.notification_sms";
    static final String ACTION_NOTIFICATION_EMAIL = PREFIX + ".action.notification_email";
    static final String ACTION_CALLSTATE = PREFIX + ".action.callstate";
    static final String ACTION_SETTIME = PREFIX + ".action.settime";
    static final String ACTION_SETMUSICINFO = PREFIX + ".action.setmusicinfo";
    static final String ACTION_REQUEST_DEVICEINFO = PREFIX + ".action.request_deviceinfo";
    static final String ACTION_REQUEST_APPINFO = PREFIX + ".action.request_appinfo";
    static final String ACTION_REQUEST_SCREENSHOT = PREFIX + ".action.request_screenshot";
    static final String ACTION_STARTAPP = PREFIX + ".action.startapp";
    static final String ACTION_DELETEAPP = PREFIX + ".action.deleteapp";
    static final String ACTION_INSTALL = PREFIX + ".action.install";
    static final String ACTION_REBOOT = PREFIX + ".action.reboot";
    static final String ACTION_FETCH_ACTIVITY_DATA = PREFIX + ".action.fetch_activity_data";
    static final String ACTION_DISCONNECT = PREFIX + ".action.disconnect";
    static final String ACTION_FIND_DEVICE = PREFIX + ".action.find_device";
    static final String ACTION_SET_ALARMS = PREFIX + ".action.set_alarms";

    static final String EXTRA_DEVICE_ADDRESS = "device_address";
    static final String EXTRA_NOTIFICATION_TITLE = "notification_title";
    static final String EXTRA_NOTIFICATION_BODY = "notification_body";
    static final String EXTRA_NOTIFICATION_SENDER = "notification_sender";
    static final String EXTRA_NOTIFICATION_SUBJECT = "notification_subject";
    static final String EXTRA_NOTIFICATION_HANDLE = "notification_handle";
    static final String EXTRA_NOTIFICATION_KIND = "notificationKind";
    static final String EXTRA_FIND_START = "find_start";
    static final String EXTRA_CALL_COMMAND = "call_command";
    static final String EXTRA_CALL_PHONENUMBER = "call_phonenumber";
    static final String EXTRA_MUSIC_ARTIST = "music_artist";
    static final String EXTRA_MUSIC_ALBUM = "music_album";
    static final String EXTRA_MUSIC_TRACK = "music_track";
    static final String EXTRA_APP_UUID = "app_uuid";
    static final String EXTRA_APP_START = "app_start";
    static final String EXTRA_URI = "uri";
    static final String EXTRA_ALARMS = "alarms";
    static final String EXTRA_PERFORM_PAIR = "perform_pair";



    void start();

    void connect();
    void connect(@Nullable String deviceAddress);
    void connect(@Nullable String deviceAddress, boolean performPair);
    void disconnect();

    void quit();
    /**
     * Requests information from the {@link DeviceCommunicationService} about the connection state,
     * firmware info, etc.
     * <p/>
     * Note that this will not need a connection to the device -- only the cached information
     * from the service will be reported.
     */
    void requestDeviceInfo();
}
