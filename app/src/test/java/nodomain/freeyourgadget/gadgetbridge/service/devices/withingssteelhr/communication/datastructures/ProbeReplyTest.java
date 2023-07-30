package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ProbeReplyTest {

    @Test
    public void testFillFromRawData() {
        // arrange
        // this data is a real world example:
        byte[] rawData = GB.hexStringToByteArray("0000000008537465656c2048521130303a32343a65343a36653a34633a3861103433303765303861643433383531616500ffffff0830303132303132320000001b00001b8100ffffff");
        ProbeReply probeReply2Test = new ProbeReply();

        // act
        probeReply2Test.fillFromRawData(rawData);

        // assert
        assertEquals(0, probeReply2Test.getYetUnknown1());
        assertEquals("Steel HR", probeReply2Test.getName());
        assertEquals("00:24:e4:6e:4c:8a", probeReply2Test.getMac());
        assertEquals("4307e08ad43851ae", probeReply2Test.getSecret());
        assertEquals(16777215, probeReply2Test.getYetUnknown2());
        assertEquals("00120122", probeReply2Test.getmId());
        assertEquals(27, probeReply2Test.getYetUnknown3());
        assertEquals(7041, probeReply2Test.getFirmwareVersion());
        assertEquals(16777215, probeReply2Test.getYetUnknown4());
    }
}