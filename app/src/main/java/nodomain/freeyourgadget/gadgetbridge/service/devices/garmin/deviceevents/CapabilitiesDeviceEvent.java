package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;

public class CapabilitiesDeviceEvent extends GBDeviceEvent {
    public Set<GarminCapability> capabilities;

    public CapabilitiesDeviceEvent(final Set<GarminCapability> capabilities) {
        this.capabilities = capabilities;
    }
}
