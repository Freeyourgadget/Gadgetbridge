package nodomain.freeyourgadget.gadgetbridge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;

/**
 * Created by Vebryn on 29/05/17.
 */

public class CSVExport {
    private static final Logger LOG = LoggerFactory.getLogger(SampleProvider.class);

    public static void exportToCSV(AbstractActivitySample[] activitySamples, File outFile) {
        String separator = ",";

        LOG.info("Exporting samples into csv file: " + outFile.getName());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            bw.write("TIMESTAMP" + separator + "DEVICE_ID" + separator + "USER_ID" + separator + "RAW_INTENSITY" + separator + "STEPS" + separator + "RAW_KIND" + separator + "HEART_RATE");
            bw.newLine();

            for (AbstractActivitySample sample : activitySamples){
                String line = sample.getTimestamp() + separator + sample.getDeviceId() + separator + sample.getUserId() + separator + sample.getRawIntensity() + separator + sample.getSteps() + separator + sample.getRawKind() + separator + sample.getHeartRate();

                //LOG.debug("Adding line into buffer: " + line);
                bw.write(line);
                bw.newLine();
            }

            bw.flush();
        } catch (IOException e) {
            LOG.error("Error related to " + outFile.getName() + " file: " + e.getMessage(), e);
        }
    }
}
