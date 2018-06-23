package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;


public class GBAutoFetchReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBAutoFetchReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        //LOG.info("User is present!");
        GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
    }
}

