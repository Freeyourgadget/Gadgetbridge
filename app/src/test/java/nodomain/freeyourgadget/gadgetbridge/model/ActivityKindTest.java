package nodomain.freeyourgadget.gadgetbridge.model;

import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ActivityKindTest {
    @Test
    public void enumCheckNoOverlap() {
        // Ensure that no 2 activity kind overlap in codes
        final Map<Integer, Boolean> knownCodes = new HashMap<>();
        for (final ActivityKind kind : ActivityKind.values()) {
            final Boolean existingCode = knownCodes.put(kind.getCode(), true);
            assertNull("ActivityKind with overlapping codes: " + kind, existingCode);
        }
    }
}
