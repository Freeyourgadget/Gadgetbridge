package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FitImporterTest {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void localTest() throws IOException {
        final FitImporter fitImporter = new FitImporter(  null, null);
        fitImporter.importFile(new File("/storage/SKIN_TEMP.fit"));
    }
}
