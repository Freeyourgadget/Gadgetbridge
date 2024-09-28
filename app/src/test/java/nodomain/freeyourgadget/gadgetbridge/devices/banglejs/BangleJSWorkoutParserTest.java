package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class BangleJSWorkoutParserTest extends TestBase {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void testLocal() {
        final File file = new File("/storage/downloads/recorder.log20240317a.csv");
        final List<BangleJSActivityPoint> pointsFromCsv = BangleJSActivityPoint.fromCsv(file);
        assert pointsFromCsv != null;
        final ActivitySummaryData summaryData = BangleJSWorkoutParser.dataFromPoints(pointsFromCsv);
    }
}
