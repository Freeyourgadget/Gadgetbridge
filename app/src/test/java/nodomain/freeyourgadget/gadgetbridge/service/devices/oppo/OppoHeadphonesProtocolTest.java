package nodomain.freeyourgadget.gadgetbridge.service.devices.oppo;

import org.junit.Assert;
import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
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

        GBDeviceEvent[] air2Events = protocol.decodeResponse(GB.hexStringToByteArray("AA4500000581003E000009312C312C302C312C322C3134322C312C342C302C322C312C382C322C322C3134322C322C342C302C332C312C33332C332C322C3132392C332C342C30"));
        Assert.assertEquals(1, air2Events.length);
        GBDeviceEventVersionInfo air2event = (GBDeviceEventVersionInfo) air2Events[0];
        Assert.assertEquals("142.142.129", air2event.fwVersion);

        GBDeviceEvent[] realme = protocol.decodeResponse(GB.hexStringToByteArray("aa2a040005810e23000003312c322c312e312e302e37352c322c322c312e312e302e37352c332c322c303031"));
        Assert.assertEquals(1, realme.length);
        GBDeviceEventVersionInfo realmeEvent = (GBDeviceEventVersionInfo) realme[0];
        Assert.assertEquals("1.1.0.75", realmeEvent.fwVersion);
    }

    @Test
    public void testMultipleResponses() {
        final OppoHeadphonesProtocol protocol = new OppoHeadphonesProtocol(null);
        GBDeviceEvent[] events = protocol.decodeResponse(GB.hexStringToByteArray("AA4100000881013A00000E010100000101010101010205010103000101040C0101050001010600020100000201010102010206020103000201040B0201050002010600AA0F000006810208000003016402640346"));
        Assert.assertEquals(4, events.length);
        Assert.assertTrue(events[0] instanceof GBDeviceEventUpdatePreferences);
        Assert.assertTrue(events[1] instanceof GBDeviceEventBatteryInfo);
        Assert.assertTrue(events[2] instanceof GBDeviceEventBatteryInfo);
        Assert.assertTrue(events[3] instanceof GBDeviceEventBatteryInfo);
    }
}
