package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import static org.junit.Assert.*;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class DataStructureFactoryTest {

    private DataStructureFactory factory2Test;

    @Before
    public void setUp() {
        factory2Test = new DataStructureFactory();
    }

    @Test
    public void testEmptyData() {
        // arrange

        // act
        List<WithingsStructure> result = factory2Test.createStructuresFromRawData(new byte[0]);

        // assert
        assertTrue(result.isEmpty());
    }


    @Test
    public void testNullData() {
        // arrange

        // act
        List<WithingsStructure> result = factory2Test.createStructuresFromRawData(null);

        // assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOneStructure() {
        // arrange
        String dataString = "0504000a470200000f0100000000";
        byte[] data = Hex.decode(dataString);

        // act
        List<WithingsStructure> result = factory2Test.createStructuresFromRawData(data);

        // assert
        assertEquals(1, result.size());
        BatteryValues batteryValues = (BatteryValues)result.get(0);
        assertEquals(2, batteryValues.getStatus());
        assertEquals(71, batteryValues.getPercent());
        assertEquals(3841, batteryValues.getVolt());
    }

    @Test
    public void testTwoStructures() {
        // arrange
        String dataString = "0504000a470200000f01000000000504000a350100000e0200000000";
        byte[] data = Hex.decode(dataString);

        // act
        List<WithingsStructure> result = factory2Test.createStructuresFromRawData(data);

        // assert
        assertEquals(2, result.size());
        BatteryValues batteryValues = (BatteryValues)result.get(0);
        assertEquals(2, batteryValues.getStatus());
        assertEquals(71, batteryValues.getPercent());
        assertEquals(3841, batteryValues.getVolt());
        batteryValues = (BatteryValues)result.get(1);
        assertEquals(1, batteryValues.getStatus());
        assertEquals(53, batteryValues.getPercent());
        assertEquals(3586, batteryValues.getVolt());
    }

    @Test
    public void testTwoStructuresWithAdditionalBytes() {
        // arrange
        String dataString = "0504000a470200000f01000000000504000a350100000e0200000000abcdef1234";
        byte[] data = Hex.decode(dataString);

        // act
        List<WithingsStructure> result = factory2Test.createStructuresFromRawData(data);

        // assert
        assertEquals(2, result.size());
        BatteryValues batteryValues = (BatteryValues)result.get(0);
        assertEquals(2, batteryValues.getStatus());
        assertEquals(71, batteryValues.getPercent());
        assertEquals(3841, batteryValues.getVolt());
        batteryValues = (BatteryValues)result.get(1);
        assertEquals(1, batteryValues.getStatus());
        assertEquals(53, batteryValues.getPercent());
        assertEquals(3586, batteryValues.getVolt());
    }



    @Test
    public void testMovementData() {
        // arrange
        String dataString = "0601000463fbb15806020002003c060a00040008002e0603000e0010000004cf0000000000000000060400020000060b000433971a5a0601000463fbb19406020002003c060a00040006001d0603000e000b000003400000000000000000060400020000060b000435971c5a";
        byte[] data = Hex.decode(dataString);

        // act
        List<WithingsStructure> result = factory2Test.createStructuresFromRawData(data);

        // assert
        assertEquals(12, result.size());
        assertTrue(result.get(0) instanceof ActivitySampleTime);
        assertTrue(result.get(1) instanceof ActivitySampleDuration);
        assertTrue(result.get(2) instanceof ActivitySampleCalories2);
        assertTrue(result.get(3) instanceof ActivitySampleMovement);
        assertTrue(result.get(4) instanceof ActivitySampleWalk);
        assertTrue(result.get(5) instanceof ActivitySampleUnknown);
        assertTrue(result.get(6) instanceof ActivitySampleTime);
        assertTrue(result.get(7) instanceof ActivitySampleDuration);
        assertTrue(result.get(8) instanceof ActivitySampleCalories2);
        assertTrue(result.get(9) instanceof ActivitySampleMovement);
        assertTrue(result.get(10) instanceof ActivitySampleWalk);
        assertTrue(result.get(11) instanceof ActivitySampleUnknown);
    }

}