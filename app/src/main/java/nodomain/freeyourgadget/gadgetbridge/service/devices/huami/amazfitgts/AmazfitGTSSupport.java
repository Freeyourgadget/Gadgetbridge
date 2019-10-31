package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgtr.AmazfitGTRFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts.AmazfitGTSFWHelper;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;

public class AmazfitGTSSupport extends AmazfitBipSupport {

    @Override
    public byte getCryptFlags() {
        return (byte) 0x80;
    }

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        super.sendNotificationNew(notificationSpec, true);
    }


    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTSFWHelper(uri, context);
    }

    @Override
    protected AmazfitGTSSupport setDisplayItems(TransactionBuilder builder) {
        // not supported yet
        return this;
    }

}
