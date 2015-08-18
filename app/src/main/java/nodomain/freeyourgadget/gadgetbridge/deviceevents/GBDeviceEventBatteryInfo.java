package nodomain.freeyourgadget.gadgetbridge.deviceevents;


import java.util.GregorianCalendar;

public class GBDeviceEventBatteryInfo extends GBDeviceEvent {
    public GregorianCalendar lastChargeTime;
    public BatteryState state = BatteryState.UNKNOWN;
    //TODO: I think the string should be deprecated in favor of the Enum above
    public String status;
    public short level = 50;
    public int numCharges = -1;

    public GBDeviceEventBatteryInfo() {
        eventClass = EventClass.BATTERY_INFO;
    }

    public enum BatteryState {
        UNKNOWN,
        CHARGE_FULL,
        CHARGE_MEDIUM,
        CHARGE_LOW,
        CHARGING,
    }
}
