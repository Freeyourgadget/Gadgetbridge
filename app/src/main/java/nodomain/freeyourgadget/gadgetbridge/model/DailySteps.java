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

import android.content.Context;
import android.widget.Toast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Calendar;
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


    public int getDailyStepsForAllDevices(Calendar day) {
        Context context = GBApplication.getContext();
        //get today's steps for all devices in GB
        int all_steps=0;

        if (context instanceof GBApplication) {
            GBApplication gbApp = (GBApplication) context;
            List<? extends GBDevice> devices = gbApp.getDeviceManager().getDevices();
            for (GBDevice device : devices){
                all_steps+=getDailyStepsForDevice(device, day);
            };
        }
        LOG.debug("gbwidget All steps:" + all_steps);
        return all_steps;
    }



    public int getDailyStepsForDevice(GBDevice device, Calendar day
    ) {
        //get today's steps for given device
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

        try (DBHandler handler = GBApplication.acquireDB()) {
            ActivityAnalysis analysis = new ActivityAnalysis();
            ActivityAmounts amounts = null;
            amounts = analysis.calculateActivityAmounts(getSamplesOfDay(handler, device, startTs, endTs));
            return getTotalsForActivityAmounts(amounts);

        } catch (Exception e) {

            GB.toast("Error loading activity summaries.", Toast.LENGTH_SHORT, GB.ERROR, e);
            return 0;
        }
    }


    private int getTotalsForActivityAmounts(ActivityAmounts activityAmounts) {
        long totalSteps = 0;
        float totalValue = 0;

        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            totalSteps += amount.getTotalSteps();
        }

        float[] totalValues = new float[]{totalSteps};

        for (int i = 0; i < totalValues.length; i++) {
            float value = totalValues[i];
            totalValue += value;
        }
        return (int)totalValue;
    };


    private List<? extends ActivitySample> getSamplesOfDay(DBHandler db, GBDevice device, int startTs, int endTs) {
        return getSamples(db, device, startTs, endTs);
    }


    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return getAllSamples(db, device, tsFrom, tsTo);
    }


    protected SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.getSampleProvider(device, db.getDaoSession());
    }


    protected List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }
}
