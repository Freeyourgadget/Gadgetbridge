package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.notification;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class NotificationAttributeTest {

    @Test
    public void testSerializeDeserialize() {
        // arrange
        NotificationAttribute attribute = new NotificationAttribute();
        attribute.setAttributeID((byte)4);
        String value = "TestNotificationAttribute";
        attribute.setValue(value);
        byte[] rawValue = value.getBytes(StandardCharsets.UTF_8);
        short expectedLength = (short) rawValue.length;

        // act
        byte[] result = attribute.serialize();

        // assert
        NotificationAttribute attribute2 = new NotificationAttribute();
        attribute2.deserialize(result);
        assertEquals((byte)4, attribute2.getAttributeID());
        assertEquals(expectedLength, attribute2.getAttributeLength());
        assertEquals(value, attribute2.getValue());
    }

    @Test
    public void testSerializeDeserializeValueTooLong() {
        // arrange
        NotificationAttribute attribute = new NotificationAttribute();
        attribute.setAttributeID((byte)4);
        attribute.setAttributeMaxLength((short)4);
        String value = "TestNotificationAttribute";
        attribute.setValue(value);
        byte[] rawValue = value.getBytes(StandardCharsets.UTF_8);
        short expectedLength = (short) rawValue.length;

        // act
        byte[] result = attribute.serialize();

        // assert
        NotificationAttribute attribute2 = new NotificationAttribute();
        attribute2.deserialize(result);
        assertEquals((byte)4, attribute2.getAttributeID());
        assertEquals(4, attribute2.getAttributeLength());
        assertEquals("Test", attribute2.getValue());
    }

}