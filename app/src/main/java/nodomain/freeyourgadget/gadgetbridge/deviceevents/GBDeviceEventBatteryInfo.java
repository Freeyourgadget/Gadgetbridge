package nodomain.freeyourgadget.gadgetbridge.deviceevents;


import java.util.GregorianCalendar;

public class GBDeviceEventBatteryInfo extends GBDeviceEvent {
    public GregorianCalendar lastChargeTime= null;
    public BatteryState state = BatteryState.UNKNOWN;
    public short level = 50;
    public int numCharges = -1;

    public GBDeviceEventBatteryInfo() {
        eventClass = EventClass.BATTERY_INFO;
    }

    public enum BatteryState {
        UNKNOWN,
        BATTERY_NORMAL,
        BATTERY_LOW,
        BATTERY_CHARGING,
        BATTERY_CHARGING_FULL,
        BATTERY_NOT_CHARGING_FULL
    }

    public boolean extendedInfoAvailable() {
        if (numCharges != -1 && lastChargeTime != null) {
            return true;
        }
        return false;
    }
}
