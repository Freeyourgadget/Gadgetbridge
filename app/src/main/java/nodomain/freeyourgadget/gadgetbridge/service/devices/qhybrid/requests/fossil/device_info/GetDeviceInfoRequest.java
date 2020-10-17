package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.device_info;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRequest;

public class GetDeviceInfoRequest extends FileGetRequest {
    enum INFO_CLASS{
        SUPPORTED_FILE_VERSIONS((short) 0x0a, SupportedFileVersionsInfo.class),
        ;
        private short identifier;
        private  Class<? extends DeviceInfo> itemClass;

        private INFO_CLASS(short identifier, Class<? extends DeviceInfo> itemClass){
            this.identifier = identifier;
            this.itemClass = itemClass;
        }

        static INFO_CLASS getByIdentifier(short identifier){
            for(INFO_CLASS infoClass : values()){
                if(infoClass.getIdentifier() == identifier) return infoClass;
            }
            return null;
        }

        public short getIdentifier() {
            return identifier;
        }

        public Class<? extends DeviceInfo> getItemClass() {
            return itemClass;
        }
    }

    public GetDeviceInfoRequest(FossilWatchAdapter adapter) {
        super(FileHandle.DEVICE_INFO, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ArrayList<DeviceInfo> deviceInfos = new ArrayList<>();

        while(buffer.remaining() > 0){
            short type = buffer.getShort();
            int length = buffer.get();
            byte[] payload = new byte[length];
            buffer.get(payload);

            INFO_CLASS infoClass = INFO_CLASS.getByIdentifier(type);
            if(infoClass == null) continue;

            Class<? extends DeviceInfo> infoC = infoClass.getItemClass();
            try {
                DeviceInfo info = infoC.newInstance();
                info.parsePayload(payload);
                deviceInfos.add(info);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        handleDeviceInfos(deviceInfos.toArray(new DeviceInfo[0]));
    }

    public void handleDeviceInfos(DeviceInfo[] deviceInfos){
        log("got infos");
    };
}
