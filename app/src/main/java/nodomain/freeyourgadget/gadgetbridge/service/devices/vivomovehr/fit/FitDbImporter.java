package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveHrSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FitDbImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FitDbImporter.class);

    private final GBDevice gbDevice;
    private final FitImporter fitImporter;

    public FitDbImporter(GBDevice gbDevice) {
        this.gbDevice = gbDevice;
        fitImporter = new FitImporter();
    }

    public void processFitFile(List<FitMessage> messages) {
        try {
            fitImporter.importFitData(messages);
        } catch (Exception e) {
            LOG.error("Error importing FIT data", e);
        }
    }

    public void processData() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            final VivomoveHrSampleProvider provider = new VivomoveHrSampleProvider(gbDevice, session);

            fitImporter.processImportedData(sample -> {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);

                provider.addGBActivitySample(sample);
            });
        } catch (Exception e) {
            LOG.error("Error importing FIT data", e);
        }
    }
}
