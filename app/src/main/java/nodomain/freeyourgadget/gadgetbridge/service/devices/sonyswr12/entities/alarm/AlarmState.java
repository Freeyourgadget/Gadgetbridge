package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm;

public enum AlarmState {
    TRIGGERED( 0),
    SNOOZED(1),
    IDLE(2);

    final int value;

    AlarmState(int value) {
        this.value = value;
    }
}
