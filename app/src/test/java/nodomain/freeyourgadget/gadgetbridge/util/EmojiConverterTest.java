package nodomain.freeyourgadget.gadgetbridge.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class EmojiConverterTest extends TestBase {
    @Test
    public void testConvert() {
        final Map<String, String> snippets = new HashMap<String, String>() {{
            put(
                    "no emoji",
                    "no emoji"
            );

            put(
                    "Hello! \uD83D\uDE0A",
                    "Hello! :-)"
            );

            put(
                    "\uD83D\uDE1B hi",
                    ":-P hi"
            );

            put(
                    "\uD83D\uDC7B\uD83D\uDC80",
                    ":ghost::skull:"
            );

            put(
                    "also \uD83D\uDE36 with words \uD83D\uDCAA\uD83C\uDFFC in-between \uD83D\uDE09",
                    "also :no_mouth: with words :muscle: in-between ;-)"
            );
        }};

        for (final Map.Entry<String, String> e : snippets.entrySet()) {
            assertEquals(
                    e.getValue(),
                    EmojiConverter.convertUnicodeEmojiToAscii(
                            e.getKey(),
                            RuntimeEnvironment.getApplication()
                    )
            );
        }
    }
}
