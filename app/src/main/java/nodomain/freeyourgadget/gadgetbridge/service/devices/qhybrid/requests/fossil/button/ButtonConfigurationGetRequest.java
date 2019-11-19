package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.button;

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

        buffer.position(16);
        for(int i = 0; i < count; i++){
            int buttonIndex = buffer.get() >> 4;
            int entryCount = buffer.get();
            buffer.get();
            short appId = buffer.getShort();

            buffer.position(buffer.position() + entryCount * 5 - 3);

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
