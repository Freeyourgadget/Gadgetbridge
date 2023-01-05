package nodomain.freeyourgadget.gadgetbridge.service.devices.flipper.zero.support;

import android.net.Uri;

import org.slf4j.LoggerFactory;

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

public class FlipperZeroBaseSupport extends AbstractBTLEDeviceSupport {
    public FlipperZeroBaseSupport() {
        super(LoggerFactory.getLogger(FlipperZeroBaseSupport.class));
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
