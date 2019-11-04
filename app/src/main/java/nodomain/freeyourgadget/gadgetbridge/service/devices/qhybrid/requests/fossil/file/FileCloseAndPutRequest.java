package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.NotificationFilterPutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FileCloseAndPutRequest extends FileCloseRequest {
    FossilWatchAdapter adapter;
    byte[] data;

    public FileCloseAndPutRequest(short fileHandle, byte[] data, FossilWatchAdapter adapter) {
        super(fileHandle);
        this.adapter = adapter;
        this.data = data;
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        adapter.queueWrite(new FilePutRequest(getHandle(), this.data, adapter) {
            @Override
            public void onFilePut(boolean success) {
                super.onFilePut(success);
                FileCloseAndPutRequest.this.onFilePut(success);
            }
        });
    }

    public void onFilePut(boolean success){

    }
}
