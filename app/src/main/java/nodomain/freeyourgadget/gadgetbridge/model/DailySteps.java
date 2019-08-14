/*  Copyright (C) 2017-2019 Carsten Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.model;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DailySteps {
    Logger LOG = LoggerFactory.getLogger(DailySteps.class);

    public String loadItems(GBDevice device) {


        try (DBHandler handler = GBApplication.acquireDB()) {

            LOG.info("PETR db load items handler " + handler);
            LOG.info("PETR db load items device " + device);
            List r = getSamplesOfDay(handler, device);
            LOG.info("PETR this is r",String.valueOf(r));
            LOG.info("PETR length of r ",String.valueOf(r.size()));
            return "test this";
        } catch (Exception e) {

            GB.toast("Error loading activity summaries.", Toast.LENGTH_SHORT, GB.ERROR, e);
            return "fail " + e;
        }

    }



    private List<? extends ActivitySample> getSamplesOfDay(DBHandler db, GBDevice device) {

        Calendar day = GregorianCalendar.getInstance();
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        startTs = (int) (day.getTimeInMillis() / 1000);

        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        endTs = (int) (day.getTimeInMillis() / 1000);
        LOG.info("PETR get samples " +" : "+ db +" : "+ device +" : "+ startTs +" : "+ endTs);
        LOG.info("PETR samples size: ", getSamples(db, device, startTs, endTs).size());
        return getSamples(db, device, startTs, endTs);
    }


    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        LOG.info("PETR get all samples" + db + device + tsFrom + tsTo);
        return getAllSamples(db, device, tsFrom, tsTo);
    }


    protected SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.getSampleProvider(device, db.getDaoSession());
    }


    protected List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        LOG.info("PETR get all activity samples" + db + device + tsFrom + tsTo + provider);
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }


}

