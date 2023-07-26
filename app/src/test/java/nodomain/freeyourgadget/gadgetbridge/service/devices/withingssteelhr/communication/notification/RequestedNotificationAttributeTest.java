package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.notification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class RequestedNotificationAttributeTest {

    @Test
    public void testSerializeDeserialize() {
        // arrange
        RequestedNotificationAttribute attribute = new RequestedNotificationAttribute();
        attribute.setAttributeID((byte)4);
        attribute.setAttributeMaxLength((short)19);

        // act
        byte[] result = attribute.serialize();

        // assert
        String test = GB.hexdump(result);
        RequestedNotificationAttribute attribute2 = new RequestedNotificationAttribute();
        attribute2.deserialize(result);
        assertEquals((byte)4, attribute2.getAttributeID());
        assertEquals(19, attribute2.getAttributeMaxLength());
    }

}