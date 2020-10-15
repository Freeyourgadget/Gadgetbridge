package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class AssetFilePutRequest extends FilePutRequest {
    public AssetFilePutRequest(AssetFile[] files, FileHandle handle, FossilWatchAdapter adapter) throws IOException {
        super(handle, prepareFileData(files), adapter);
    }
    public AssetFilePutRequest(AssetFile file, FileHandle handle, FossilWatchAdapter adapter) {
        super(handle, prepareFileData(file), adapter);
    }

    private static byte[] prepareFileData(AssetFile[] files) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (AssetFile file : files) {
            stream.write(
                    prepareFileData(file)
            );
        }

        return stream.toByteArray();
    }

    private static byte[] prepareFileData(AssetFile file){
        int size = file.getFileName().length() + file.getFileData().length + 1; // null byte
        ByteBuffer buffer = ByteBuffer.allocate(size + 2);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short)(size));
        buffer.put(file.getFileName().getBytes());
        buffer.put((byte) 0x00);
        buffer.put(file.getFileData());

        return buffer.array();
    }
}
