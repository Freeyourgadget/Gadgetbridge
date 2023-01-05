package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor;

import android.net.Uri;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;

public class BinarySensorBaseSupport extends AbstractBTLEDeviceSupport {
    public BinarySensorBaseSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
