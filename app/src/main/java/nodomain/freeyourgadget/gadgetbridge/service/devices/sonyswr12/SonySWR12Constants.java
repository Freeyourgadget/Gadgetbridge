package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import java.util.UUID;

public class SonySWR12Constants {
    //accessory host service
    public static final String BASE_UUID_AHS = "0000%s-37CB-11E3-8682-0002A5D5C51B";
    public static final UUID UUID_SERVICE_AHS = UUID.fromString(String.format(BASE_UUID_AHS, "0200"));
    public static final UUID UUID_CHARACTERISTIC_ALARM = UUID.fromString(String.format(BASE_UUID_AHS, "0204"));
    public static final UUID UUID_CHARACTERISTIC_EVENT = UUID.fromString(String.format(BASE_UUID_AHS, "0205"));
    public static final UUID UUID_CHARACTERISTIC_TIME = UUID.fromString(String.format(BASE_UUID_AHS, "020B"));
    public static final UUID UUID_CHARACTERISTIC_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID_AHS, "0208"));

    public static final String VIBRATION_PREFERENCE = "vibration_preference";
    public static final String STAMINA_PREFERENCE = "stamina_preference";
    public static final String SMART_ALARM_INTERVAL_PREFERENCE = "smart_alarm_interval_preference";

    public static final int TYPE_ACTIVITY = 0;
    public static final int TYPE_LIGHT = 1;
    public static final int TYPE_DEEP = 2;
    public static final int TYPE_NOT_WORN = 3;
}
