/*  Copyright (C) 2020-2023 Petr Kadlec

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
