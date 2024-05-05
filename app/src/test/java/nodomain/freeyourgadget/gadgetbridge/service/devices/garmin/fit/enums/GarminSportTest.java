package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import static org.junit.Assert.assertTrue;

import android.util.Pair;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class GarminSportTest extends TestBase {
    @Test
    public void testNoDuplicates() {
        // Ensure there are no duplicated sports with the same type and subtype
        final Set<GarminSport> duplicates = new HashSet<>();
        final Set<Pair<Integer, Integer>> seen = new HashSet<>();

        for (final GarminSport sport : GarminSport.values()) {
            final Pair<Integer, Integer> codePair = Pair.create(sport.getType(), sport.getSubtype());
            if (seen.contains(codePair)) {
                duplicates.add(sport);
            }
            seen.add(codePair);
        }

        assertTrue("Duplicated sport codes: " + duplicates, duplicates.isEmpty());
    }
}
