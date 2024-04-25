package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDataTransferService;

public class DataTransferHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DataTransferHandler.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private static final Map<Integer, Data> dataById = new HashMap<>();
    private static final Map<Integer, ChunkInfo> unprocessedChunksByRequestId = new HashMap<>();

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
        unprocessedChunksByRequestId.put(requestId, new ChunkInfo(dataId, offset, offset + chunk.length));
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

    public static void onDataChunkSuccessfullyReceived(final int requestId) {
        final ChunkInfo chunkInfo = unprocessedChunksByRequestId.get(requestId);
        if (chunkInfo == null) {
            return;
        }
        unprocessedChunksByRequestId.remove(requestId);
        final Data data = dataById.get(chunkInfo.dataId);
        if (data == null) {
            return;
        }
        data.onDataChunkSuccessfullyReceived(chunkInfo);
        if (data.isDataSuccessfullySent()) {
            LOG.info("Data successfully sent to the device (id: {}, size: {})", chunkInfo.dataId, data.data.length);
            dataById.remove(chunkInfo.dataId);
        } else {
            LOG.debug(
                    "Data chunk successfully sent to the device (dataId: {}, requestId: {}): {}-{}/{}",
                    chunkInfo.dataId, requestId, chunkInfo.start, chunkInfo.end, data.data.length
            );
        }
    }

    private static class ChunkInfo {
        private final int dataId;
        private final int start;
        private final int end;

        private ChunkInfo(int dataId, int start, int end) {
            this.dataId = dataId;
            this.start = start;
            this.end = end;
        }
    }

    private static class Data {
        // TODO Wouldn't it be better to store data as streams?
        // Because now we have to store the whole data in RAM.
        private final byte[] data;
        private final TreeMap<Integer, ChunkInfo> chunksReceivedByDevice;

        private Data(byte[] data) {
            this.data = data;
            chunksReceivedByDevice = new TreeMap<>();
        }

        private byte[] getDataChunk(final int offset, final int maxChunkSize) {
            if (offset < 0 || offset >= data.length) {
                return null;
            }
            return Arrays.copyOfRange(data, offset, Math.min(offset + maxChunkSize, data.length));
        }

        private void onDataChunkSuccessfullyReceived(ChunkInfo newlyReceivedChunk) {
            final ChunkInfo alreadyReceivedChunk = chunksReceivedByDevice.get(newlyReceivedChunk.start);
            if (alreadyReceivedChunk == null || alreadyReceivedChunk.end < newlyReceivedChunk.end) {
                chunksReceivedByDevice.put(newlyReceivedChunk.start, newlyReceivedChunk);
            }
        }

        private boolean isDataSuccessfullySent() {
            Integer previousChunkEnd = null;
            for (Map.Entry<Integer, ChunkInfo> chunkEntry : chunksReceivedByDevice.entrySet()) {
                if (previousChunkEnd == null && chunkEntry.getKey() != 0) {
                    // The head of the data wasn't received by the device.
                    return false;
                }
                if (previousChunkEnd != null && chunkEntry.getKey() > previousChunkEnd) {
                    // There is some gap between received chunks.
                    return false;
                }
                previousChunkEnd = chunkEntry.getValue().end;
            }
            // Check if the end of the last chunk matches the data size.
            return previousChunkEnd != null && data.length == previousChunkEnd;
        }
    }
}
