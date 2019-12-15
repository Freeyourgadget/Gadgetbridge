package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.utils.StringUtils;

public class AssetFilePutRequest extends FilePutRequest {
    public AssetFilePutRequest(byte[] fileName, byte[] file, FossilWatchAdapter adapter) {
        super((short) 0x0701, prepareFileData(fileName, file), adapter);
    }
    public AssetFilePutRequest(byte[][] fileNames, byte[][] files, FossilWatchAdapter adapter) throws IOException {
        super((short) 0x0701, prepareFileData(fileNames, files), adapter);
    }

    private static byte[] prepareFileData(byte[][] fileNames, byte[][] files) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for(int i = 0; i < fileNames.length; i++){
            stream.write(
                    prepareFileData(fileNames[i], files[i])
            );
        }

        return stream.toByteArray();
    }

    private static byte[] prepareFileData(byte[] fileNameNullTerminated, byte[] file){
        ByteBuffer buffer = ByteBuffer.allocate(fileNameNullTerminated.length + 2 + file.length);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short)(fileNameNullTerminated.length + file.length));
        buffer.put(fileNameNullTerminated);
        buffer.put(file);

        return buffer.array();
    }
}
