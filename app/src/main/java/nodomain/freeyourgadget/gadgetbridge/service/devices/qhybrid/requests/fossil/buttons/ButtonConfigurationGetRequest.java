package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.buttons;

import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig.ConfigPayload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRequest;

public class ButtonConfigurationGetRequest extends FileGetRequest {
    public ButtonConfigurationGetRequest(FossilWatchAdapter adapter) {
        super((short) 0x0600, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        log("fileData");

        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        short fileHandle = buffer.getShort(0);
        // TODO check file handle
        // if(fileData != )

        byte count = buffer.get(15);

        ConfigPayload[] configs = new ConfigPayload[count];

        for(int i = 0; i < count; i++){
            byte buttonIndex = (byte) (buffer.get(16 + i * 7) >> 4);
            short appId = buffer.getShort(19 + i * 7);

            try {
                configs[buttonIndex - 1] = ConfigPayload.fromId(appId);
            }catch (RuntimeException e){
                configs[buttonIndex - 1] =  null;
            }
        }

        this.onConfigurationsGet(configs);
    }

    public void onConfigurationsGet(ConfigPayload[] configs){}
}
