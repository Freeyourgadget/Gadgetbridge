package nodomain.freeyourgadget.gadgetbridge.service.devices.oppo;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class OppoHeadphonesProtocolTest extends TestBase {
    @Test
    public void testHandleFirmware() {
        final OppoHeadphonesProtocol protocol = new OppoHeadphonesProtocol(null);
        GBDeviceEvent[] oppoEvents = protocol.decodeResponse(GB.hexStringToByteArray("aa4f00000581f34800000a312c312c312c312c322c3136302c312c332c3838382c312c342c302c322c312c312c322c322c3136302c322c332c3838382c322c342c302c332c312c312c332c322c38323700"));
        Assert.assertEquals(1, oppoEvents.length);
        GBDeviceEventVersionInfo oppoEvent = (GBDeviceEventVersionInfo) oppoEvents[0];
        Assert.assertEquals("160.160.827", oppoEvent.fwVersion);

        GBDeviceEvent[] realme = protocol.decodeResponse(GB.hexStringToByteArray("aa2a040005810e23000003312c322c312e312e302e37352c322c322c312e312e302e37352c332c322c303031"));
        Assert.assertEquals(1, realme.length);
        GBDeviceEventVersionInfo realmeEvent = (GBDeviceEventVersionInfo) realme[0];
        Assert.assertEquals("1.1.0.75", realmeEvent.fwVersion);
    }
}
