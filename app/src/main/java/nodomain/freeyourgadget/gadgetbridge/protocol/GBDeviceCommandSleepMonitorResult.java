package nodomain.freeyourgadget.gadgetbridge.protocol;

public class GBDeviceCommandSleepMonitorResult extends GBDeviceCommand {
    // FIXME: this is just the low-level data from Morpheuz, we need something generic
    public short[] points;
    public int smartalarm_from = -1; // time in minutes relative from 0:00 for smart alarm (earliest)
    public int smartalarm_to = -1;// time in minutes relative from 0:00 for smart alarm (latest)
    public int recording_base_timestamp = -1; // timestamp for the first "point", all folowing are +10 minutes offset each
    public int alarm_gone_off = -1; // time in minutes relative from 0:00 when alarm gone off

    public GBDeviceCommandSleepMonitorResult() {
        commandClass = CommandClass.SLEEP_MONITOR_RES;
    }
}
