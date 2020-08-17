package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FirmwareFilePutRequest extends FilePutRawRequest {
    public FirmwareFilePutRequest(byte[] firmwareBytes, FossilHRWatchAdapter adapter) {
        super((short) 0x00FF, firmwareBytes, adapter);
    }

    @Override
    public void onPacketWritten(TransactionBuilder transactionBuilder, int packetNr, int packetCount) {
        int progressPercent = (int) ((((float) packetNr) / packetCount) * 100);
        transactionBuilder.add(new SetProgressAction(GBApplication.getContext().getString(R.string.updatefirmwareoperation_update_in_progress), true, progressPercent, GBApplication.getContext()));
    }

    @Override
    public void onFilePut(boolean success) {
        Context context = GBApplication.getContext();
        if (success) {
            GB.updateInstallNotification(context.getString(R.string.updatefirmwareoperation_update_complete), false, 100, context);
        } else {
            GB.updateInstallNotification(context.getString(R.string.updatefirmwareoperation_write_failed), false, 0, context);
        }
    }
}
