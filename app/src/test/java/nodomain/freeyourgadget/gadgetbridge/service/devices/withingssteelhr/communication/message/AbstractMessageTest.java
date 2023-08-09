package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsTestStructure;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AbstractMessageTest {

    @Test
    public void testGetRawDataNoData() {
        // arrange
        Message testMessage = createTestcommand();

        // act
        byte[] rawData = testMessage.getRawData();

        // assert
        assertEquals("0100630000", StringUtils.bytesToHex(rawData));
    }

    @Test
    public void testGetRawDataWithData() {
        // arrange
        Message testMessage = createTestcommand();
        testMessage.addDataStructure(new WithingsTestStructure());

        // act
        byte[] rawData = testMessage.getRawData();

        // assert
        assertEquals("0100630006006354657374", StringUtils.bytesToHex(rawData));
    }

    private Message createTestcommand() {
        return new AbstractMessage(){
            @Override
            public short getType() {
                    return 99;
            }

            @Override
            public boolean needsResponse() {
                return false;
            }

            @Override
            public boolean needsEOT() {
                return false;
            }

            @Override
            public boolean isIncomingMessage() {
                return false;
            }
        };
    }
}