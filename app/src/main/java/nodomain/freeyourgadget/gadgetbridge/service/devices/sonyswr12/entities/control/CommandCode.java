package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control;

public enum CommandCode {
    FLUSH_ACTIVITY(7),
    HEARTRATE_REALTIME(11),
    STAMINA_MODE(17),
    MANUAL_ALARM(19),
    LOW_VIBRATION(25);

    public final int value;

    CommandCode(int value) {
        this.value = value;
    }
}
