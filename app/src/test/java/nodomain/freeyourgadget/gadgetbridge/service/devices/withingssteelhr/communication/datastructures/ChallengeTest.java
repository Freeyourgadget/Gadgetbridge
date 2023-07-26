package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ChallengeTest {

    @Test
    public void testFillFromRawData() {
        // arrange
        byte[] rawData = GB.hexStringToByteArray("1130303a32343a65343a36653a34633a38611082f3d9e121f16a5a3cf0ba94261e8ff6");
        byte[] expectedChallengeBytes = GB.hexStringToByteArray("82f3d9e121f16a5a3cf0ba94261e8ff6");
        Challenge challenge2Test = new Challenge();

        // act
        challenge2Test.fillFromRawData(rawData);

        // assert
        assertEquals("00:24:e4:6e:4c:8a", challenge2Test.getMacAddress());
        assertArrayEquals(expectedChallengeBytes, challenge2Test.getChallenge());
    }

    @Test
    public void testToRawData() {
        // arrange
        Challenge challenge2Test = new Challenge();
        challenge2Test.setMacAddress("00:24:e4:6e:4c:8a");
        challenge2Test.setChallenge(GB.hexStringToByteArray("82f3d9e121f16a5a3cf0ba94261e8ff6"));

        // act
        byte[] result = challenge2Test.getRawData();

        // assert
        assertArrayEquals(GB.hexStringToByteArray("012200231130303a32343a65343a36653a34633a38611082f3d9e121f16a5a3cf0ba94261e8ff6"), result);
    }

    @Test
    public void testToRawDataNoMacAddress() {
        // arrange
        Challenge challenge2Test = new Challenge();
        challenge2Test.setChallenge(GB.hexStringToByteArray("82f3d9e121f16a5a3cf0ba94261e8ff6"));

        // act
        byte[] result = challenge2Test.getRawData();

        // assert
        assertArrayEquals(GB.hexStringToByteArray("01220012001082f3d9e121f16a5a3cf0ba94261e8ff6"), result);
    }

    @Test
    public void testToRawDataNoChallengeBytes() {
        // arrange
        Challenge challenge2Test = new Challenge();
        challenge2Test.setMacAddress("00:24:e4:6e:4c:8a");

        // act
        byte[] result = challenge2Test.getRawData();

        // assert
        assertArrayEquals(GB.hexStringToByteArray("012200131130303a32343a65343a36653a34633a386100"), result);
    }

}