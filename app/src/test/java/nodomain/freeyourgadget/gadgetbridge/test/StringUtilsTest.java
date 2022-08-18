package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringUtilsTest extends TestBase {
    private static final String SEP = ":";
    private static final String E1 = "e1";
    private static final String E2 = "e2";
    private static final String E3 = "e3";

    @Test
    public void testJoinNull() {
        StringBuilder result = StringUtils.join(SEP, (String[]) null);
        assertEquals("", result.toString());
    }

    @Test
    public void testJoinNullElement() {
        StringBuilder result = StringUtils.join(SEP, (String) null);
        assertEquals("", result.toString());
    }

    @Test
    public void testJoinSingleElement() {
        StringBuilder result = StringUtils.join(SEP, E1);
        assertEquals(E1, result.toString());
    }

    @Test
    public void testJoinSingleAndNullElement() {
        StringBuilder result = StringUtils.join(SEP, E1, null);
        assertEquals(E1, result.toString());
    }

    @Test
    public void testJoinTwoElements() {
        StringBuilder result = StringUtils.join(SEP, E1, E2);
        assertEquals(E1 + SEP + E2, result.toString());
    }

    @Test
    public void testJoinTwoElementsAndNull() {
        StringBuilder result = StringUtils.join(SEP, E1, null, E2);
        assertEquals(E1 + SEP + E2, result.toString());
    }

    @Test
    public void testJoinThreeElements() {
        StringBuilder result = StringUtils.join(SEP, E1, E2, E3);
        assertEquals(E1 + SEP + E2 + SEP + E3, result.toString());
    }

    @Test
    public void testUntilNullTerminator() {
        assertNull(StringUtils.untilNullTerminator("Hello, World!".getBytes(StandardCharsets.UTF_8), 7));
        assertNull(StringUtils.untilNullTerminator("Hello, World!".getBytes(StandardCharsets.UTF_8), 99));
        assertEquals("Hello, World!", StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 0));
        assertEquals("World!", StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 7));
        assertEquals("", StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 13));
        assertNull(StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 14));
        assertNull(StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 99));
    }
}
