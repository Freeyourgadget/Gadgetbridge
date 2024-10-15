package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class XiaomiActivityParserTest {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void localTest() throws IOException {
        final byte[] data = Files.readAllBytes(Paths.get("/storage/activity.bin"));
        final byte[] fileIdBytes = Arrays.copyOfRange(data, 0, 7);
        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);
        final XiaomiActivityParser parser = XiaomiActivityParser.create(fileId);

        parser.parse(null, fileId, data);
    }
}
