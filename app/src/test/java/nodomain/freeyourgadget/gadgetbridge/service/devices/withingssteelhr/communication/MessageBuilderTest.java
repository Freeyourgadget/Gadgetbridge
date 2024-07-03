package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.WithingsSteelHRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.BatteryValues;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.MessageBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.MessageFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.WithingsMessage;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MessageBuilderTest {

    @Mock
    private MessageFactory messageFactoryMock;

    @Mock
    private WithingsSteelHRDeviceSupport supportMock;

    private MessageBuilder messageBuilder2Test;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        messageBuilder2Test = new MessageBuilder(supportMock, messageFactoryMock);
    }

    @Test
    public void testUnresolveableMessage() {
        // arrange
        byte[] data = GB.hexStringToByteArray("143fbcce");
        when(messageFactoryMock.createMessageFromRawData(data)).thenReturn(null);

        // act
        boolean result = messageBuilder2Test.buildMessage(data);

        // assert;
        assertFalse(result);
        verifyNoInteractions(supportMock);
    }

    @Test
    public void testUnknownMessageType() {
        // arrange
        byte[] data = GB.hexStringToByteArray("0103e7000456abcdef");
        when(messageFactoryMock.createMessageFromRawData(data)).thenReturn(new WithingsMessage((short)999));

        // act
        boolean result = messageBuilder2Test.buildMessage(data);

        // assert;
        assertTrue(result);
        verify(messageFactoryMock, times(1)).createMessageFromRawData(data);
        verifyNoInteractions(supportMock);
    }

    @Test
    public void testIncompleteMessage() {
        // arrange
        byte[] data = GB.hexStringToByteArray("0103e7000856abcdef");
        when(messageFactoryMock.createMessageFromRawData(data)).thenReturn(new WithingsMessage((short)1286));

        // act
        boolean result = messageBuilder2Test.buildMessage(data);

        // assert;
        assertFalse(result);
        verify(messageFactoryMock, never()).createMessageFromRawData(data);
        verifyNoInteractions(supportMock);
    }

    @Test
    public void testUnknownMessageInTwoChunks() {
        // arrange
        byte[] data1 = GB.hexStringToByteArray("0103e7000856abcdef");
        byte[] data2 = GB.hexStringToByteArray("56abcdef");
        byte[] dataComplete= GB.hexStringToByteArray("0103e7000856abcdef56abcdef");
        when(messageFactoryMock.createMessageFromRawData(dataComplete)).thenReturn(new WithingsMessage((short)1286));

        // act
        boolean result1 = messageBuilder2Test.buildMessage(data1);
        boolean result2 = messageBuilder2Test.buildMessage(data2);

        // assert;
        assertFalse(result1);
        assertTrue(result2);
        verify(messageFactoryMock, never()).createMessageFromRawData(data1);
        verify(messageFactoryMock, never()).createMessageFromRawData(data2);
        verify(messageFactoryMock, times(1)).createMessageFromRawData(dataComplete);
        verifyNoInteractions(supportMock);
    }

    @Test
    public void testKnownMessageInTwoChunks() {
        // arrange
        byte[] data1 = GB.hexStringToByteArray("010504000856abcdef");
        byte[] data2 = GB.hexStringToByteArray("56abcdef");
        byte[] dataComplete= GB.hexStringToByteArray("010504000856abcdef56abcdef");
        when(messageFactoryMock.createMessageFromRawData(dataComplete)).thenReturn(new WithingsMessage((short)1284));

        // act
        boolean result1 = messageBuilder2Test.buildMessage(data1);
        boolean result2 = messageBuilder2Test.buildMessage(data2);

        // assert;
        assertFalse(result1);
        assertTrue(result2);
        verify(messageFactoryMock, never()).createMessageFromRawData(data1);
        verify(messageFactoryMock, never()).createMessageFromRawData(data2);
        verify(messageFactoryMock, times(1)).createMessageFromRawData(dataComplete);
        verifyNoInteractions(supportMock);
    }

    @Test
    public void testKnownMessageWithValidDataInTwoChunks() {
        // arrange
        byte[] data1 = GB.hexStringToByteArray("010504000e0504000a4702");
        byte[] data2 = GB.hexStringToByteArray("00000f0100000000");
        byte[] dataComplete= GB.hexStringToByteArray("010504000e0504000a470200000f0100000000");
        Message message = new WithingsMessage((short)1284);
        BatteryValues batteryValues = new BatteryValues();
        message.addDataStructure(batteryValues);
        when(messageFactoryMock.createMessageFromRawData(dataComplete)).thenReturn(message);

        // act
        boolean result1 = messageBuilder2Test.buildMessage(data1);
        boolean result2 = messageBuilder2Test.buildMessage(data2);

        // assert;
        assertFalse(result1);
        assertTrue(result2);
        verify(messageFactoryMock, never()).createMessageFromRawData(data1);
        verify(messageFactoryMock, never()).createMessageFromRawData(data2);
        verify(messageFactoryMock, times(1)).createMessageFromRawData(dataComplete);
        verifyNoMoreInteractions(supportMock);
    }

}