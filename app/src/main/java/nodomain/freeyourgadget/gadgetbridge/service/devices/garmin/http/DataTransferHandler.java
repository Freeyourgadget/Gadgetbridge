package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDataTransferService;

public class DataTransferHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DataTransferHandler.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private static final Map<Integer, Data> dataById = new HashMap<>();
    private static final Map<Integer, RequestInfo> requestInfoById = new HashMap<>();

    public GdiDataTransferService.DataTransferService handle(
            final GdiDataTransferService.DataTransferService dataTransferService,
            final int requestId
    ) {
        if (dataTransferService.hasDataDownloadRequest()) {
            final GdiDataTransferService.DataTransferService.DataDownloadResponse dataDownloadResponse
                    = handleDataDownloadRequest(dataTransferService.getDataDownloadRequest(), requestId);
            if (dataDownloadResponse != null) {
                return GdiDataTransferService.DataTransferService.newBuilder()
                        .setDataDownloadResponse(dataDownloadResponse)
                        .build();
            }
            return null;
        }

        LOG.warn("Unsupported data transfer service request: {}", dataTransferService);

        return null;
    }

    public GdiDataTransferService.DataTransferService.DataDownloadResponse handleDataDownloadRequest(
            final GdiDataTransferService.DataTransferService.DataDownloadRequest dataDownloadRequest,
            final int requestId
    ) {
        final int dataId = dataDownloadRequest.getId();
        final int offset = dataDownloadRequest.getOffset();
        LOG.debug("Received data download request (id: {}, offset: {})", dataId, offset);
        final Data data = dataById.get(dataId);
        if (data == null) {
            LOG.error("Device requested data with invalid id: {}", dataId);
            return GdiDataTransferService.DataTransferService.DataDownloadResponse.newBuilder()
                    .setStatus(GdiDataTransferService.DataTransferService.Status.INVALID_ID)
                    .setId(dataId)
                    .setOffset(offset)
                    .build();
        }
        final int maxChunkSize = dataDownloadRequest.hasMaxChunkSize() ? dataDownloadRequest.getMaxChunkSize() : Integer.MAX_VALUE;
        final byte[] chunk = data.getDataChunk(offset, maxChunkSize);
        if (chunk == null) {
            LOG.error("Device requested data with invalid offset: {}", offset);
            return GdiDataTransferService.DataTransferService.DataDownloadResponse.newBuilder()
                    .setStatus(GdiDataTransferService.DataTransferService.Status.INVALID_OFFSET)
                    .setId(dataId)
                    .setOffset(offset)
                    .build();
        }
        requestInfoById.put(requestId, new RequestInfo(dataId, chunk.length));
        return GdiDataTransferService.DataTransferService.DataDownloadResponse.newBuilder()
                .setStatus(GdiDataTransferService.DataTransferService.Status.SUCCESS)
                .setId(dataId)
                .setOffset(offset)
                .setPayload(ByteString.copyFrom(chunk))
                .build();
    }

    public static int registerData(final byte[] data) {
        int id = idCounter.getAndIncrement();
        LOG.info("New data will be sent to the device (id: {}, size: {})", id, data.length);
        dataById.put(id, new Data(data));
        return id;
    }

    public static void onDataSuccessfullyReceived(final int requestId) {
        final RequestInfo requestInfo = requestInfoById.get(requestId);
        requestInfoById.remove(requestId);
        if (requestInfo == null) {
            return;
        }
        final Data data = dataById.get(requestInfo.dataId);
        if (data == null) {
            return;
        }
        int dataLeft = data.onDataSuccessfullyReceived(requestInfo.requestDataLength);
        if (dataLeft == 0) {
            LOG.info("Data successfully sent to the device (id: {}, size: {})", requestInfo.dataId, data.data.length);
            dataById.remove(requestInfo.dataId);
        } else {
            LOG.debug("Data chunk successfully sent to the device (dataId: {}, requestId: {}, data left: {})", requestInfo.dataId, requestId, dataLeft);
        }
    }

    private static class RequestInfo {
        private final int dataId;
        private final int requestDataLength;

        private RequestInfo(int dataId, int requestDataLength) {
            this.dataId = dataId;
            this.requestDataLength = requestDataLength;
        }
    }

    private static class Data {
        // TODO Wouldn't it be better to store data as streams?
        // Because now we have to store the whole data in RAM.
        private final byte[] data;
        private final AtomicInteger dataLeft;

        private Data(byte[] data) {
            this.data = data;
            this.dataLeft = new AtomicInteger(data.length);
        }

        private byte[] getDataChunk(final int offset, final int maxChunkSize) {
            if (offset < 0 || offset >= data.length) {
                return null;
            }
            return Arrays.copyOfRange(data, offset, Math.min(offset + maxChunkSize, data.length));
        }

        private int onDataSuccessfullyReceived(int chunkSize) {
            // TODO Does this work properly?
            // Problems can arise when the app receives two ACKs for the same data.
            // It can be solved by storing information about what data was ACKed instead of just dataLeft variable.
            return dataLeft.addAndGet(-chunkSize);
        }
    }
}
