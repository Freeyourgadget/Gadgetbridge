package nodomain.freeyourgadget.gadgetbridge.tasker.plugin;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.devices.xwatch.XWatchTaskerSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.tasker.spec.TaskerSpec;

/**
 * Tasker devices with corresponding {@link nodomain.freeyourgadget.gadgetbridge.impl.GBDevice}.
 * <p>
 * Add new devices here! Provide a {@link TaskerSpec} and your ready to go.
 */
public enum TaskerDevice implements Serializable {

    XWATCH(DeviceType.XWATCH, new XWatchTaskerSpec(DeviceType.XWATCH));

    private DeviceType type;
    private TaskerSpec spec;

    TaskerDevice(DeviceType type, TaskerSpec spec) {
        this.type = type;
        this.spec = spec;
    }

    public DeviceType getType() {
        return type;
    }

    public TaskerSpec getSpec() {
        return spec;
    }
}