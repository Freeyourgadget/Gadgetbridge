package nodomain.freeyourgadget.gadgetbridge.deviceevents;


import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

public class GBDeviceEventBatteryInfo extends GBDeviceEvent {
    public GregorianCalendar lastChargeTime= null;
    public BatteryState state = BatteryState.UNKNOWN;
    public short level = 50;
    public int numCharges = -1;

    public GBDeviceEventBatteryInfo() {
        eventClass = EventClass.BATTERY_INFO;
    }

    public boolean extendedInfoAvailable() {
        if (numCharges != -1 && lastChargeTime != null) {
            return true;
        }
        return false;
    }
}
