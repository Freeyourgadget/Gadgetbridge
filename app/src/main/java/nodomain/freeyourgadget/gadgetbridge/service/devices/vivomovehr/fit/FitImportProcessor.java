package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import nodomain.freeyourgadget.gadgetbridge.entities.VivomoveHrActivitySample;

interface FitImportProcessor {
    void onSample(VivomoveHrActivitySample sample);
}
