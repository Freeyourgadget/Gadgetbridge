package nodomain.freeyourgadget.gadgetbridge.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleColor;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;

public class PebbleUtilsTest extends TestBase {
    @Test
    public void testHexToPebbleColorConversion() {
        Map<String, Byte> testCases = new HashMap<>();

        testCases.put("#000000", PebbleColor.Black);
        testCases.put("#ffffff", PebbleColor.White);
        testCases.put("#00ff00", PebbleColor.Green);

        testCases.put("#452435", PebbleColor.Black);
        testCases.put("#334afd", PebbleColor.DukeBlue);
        testCases.put("#ccb75c", PebbleColor.Brass);
        testCases.put("#1b1c94", PebbleColor.OxfordBlue);
        testCases.put("#90f892", PebbleColor.MayGreen);
        testCases.put("#ff7301", PebbleColor.Orange);

        testCases.put("#00aa00", PebbleColor.IslamicGreen);

        for (String colorKey : testCases.keySet()) {
            byte evaluatedColor = PebbleUtils.getPebbleColor(colorKey);
            assertEquals("Color " + colorKey + " failed to translate properly!",
                    testCases.get(colorKey).byteValue(), evaluatedColor);
        }
    }

    @Test
    public void testIntToPebbleColorConversion() {
        Map<Integer, Byte> testCases = new HashMap<>();

        testCases.put(0x000000, PebbleColor.Black);
        testCases.put(0xffffff, PebbleColor.White);
        testCases.put(0x00ff00, PebbleColor.Green);

        testCases.put(0x00aa00, PebbleColor.IslamicGreen);

        for (int colorKey : testCases.keySet()) {
            byte evaluatedColor = PebbleUtils.getPebbleColor(colorKey);
            assertEquals("Color " + Integer.toHexString(colorKey) + " failed to translate properly!",
                    testCases.get(colorKey).byteValue(), evaluatedColor);
        }
    }
}
