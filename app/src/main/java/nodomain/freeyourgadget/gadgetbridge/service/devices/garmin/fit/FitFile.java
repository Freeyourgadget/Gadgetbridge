package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class FitFile {
    protected static final Logger LOG = LoggerFactory.getLogger(FitFile.class);
    private final Header header;
    private final Map<RecordDefinition, List<RecordData>> dataRecords;
    private final boolean canGenerateOutput;

    public FitFile(Header header, Map<RecordDefinition, List<RecordData>> dataRecords) {
        this.header = header;
        this.dataRecords = dataRecords;
        this.canGenerateOutput = false;
    }

    public FitFile(LinkedHashMap<RecordDefinition, List<RecordData>> dataRecords) {
        this.dataRecords = dataRecords;
        this.header = new Header(true, 16, 21117);
        this.canGenerateOutput = true;
    }

    private static byte[] readFileToByteArray(File file) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FitFile parseIncoming(File file) {
        return parseIncoming(readFileToByteArray(file));
    }

    //TODO: process file in chunks??
    public static FitFile parseIncoming(byte[] fileContents) {

        final GarminByteBufferReader garminByteBufferReader = new GarminByteBufferReader(fileContents);
        garminByteBufferReader.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        final Header header = Header.parseIncomingHeader(garminByteBufferReader);

        Map<RecordHeader, RecordDefinition> recordDefinitionMap = new HashMap<>(); //needed because the headers can be redefined in the file. The last header wins
        Map<RecordDefinition, List<RecordData>> dataRecords = new LinkedHashMap<>();
        Long referenceTimestamp = null;

        while (garminByteBufferReader.getPosition() < header.getHeaderSize() + header.getDataSize()) {
            byte rawRecordHeader = (byte) garminByteBufferReader.readByte();
            RecordHeader recordHeader = new RecordHeader(rawRecordHeader);
            if (recordHeader.isCompressedTimestamp()) {
                referenceTimestamp += recordHeader.getTimeOffset();
                recordHeader.setReferenceTimestamp(referenceTimestamp);
            }
            if (recordHeader.isDefinition()) {
                final RecordDefinition recordDefinition = RecordDefinition.parseIncoming(garminByteBufferReader, recordHeader);
                if (recordDefinition != null) {
                    if (recordHeader.isDeveloperData())
                        for (RecordDefinition rd : dataRecords.keySet()) {
                            if (GlobalFITMessage.FIELD_DESCRIPTION.equals(rd.getGlobalFITMessage()))
                                recordDefinition.populateDevFields(dataRecords.get(rd));
                        }
                    recordDefinitionMap.put(recordHeader, recordDefinition);
                    dataRecords.put(recordDefinition, new ArrayList<>());
                }
            } else {
                final RecordDefinition referenceRecordDefinition = recordDefinitionMap.get(recordHeader);
                final List<RecordData> myList = dataRecords.get(referenceRecordDefinition);
                if (referenceRecordDefinition != null) {
                    final RecordData runningData = new RecordData(referenceRecordDefinition, recordHeader);
                    myList.add(runningData);
                    Long newTimestamp = runningData.parseDataMessage(garminByteBufferReader);
                    if (newTimestamp != null)
                        referenceTimestamp = newTimestamp;
                }
            }
        }
        garminByteBufferReader.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        int fileCrc = garminByteBufferReader.readShort();
        if (fileCrc != ChecksumCalculator.computeCrc(fileContents, header.getHeaderSize(), fileContents.length - header.getHeaderSize() - 2)) {
            throw new IllegalArgumentException("Wrong CRC for FIT file");
        }
        return new FitFile(header, dataRecords);
    }

    public List<RecordData> getRecordsByGlobalMessage(GlobalFITMessage globalFITMessage) {
        final List<RecordData> filtered = new ArrayList<>();
        for (RecordDefinition rd : dataRecords.keySet()) {
            if (globalFITMessage.equals(rd.getGlobalFITMessage()))
                filtered.addAll(dataRecords.get(rd));
        }
        return filtered;
    }

    public void generateOutgoingDataPayload(MessageWriter writer) {
        if (!canGenerateOutput)
            throw new IllegalArgumentException("Generation of previously parsed FIT file not supported.");

        MessageWriter temporary = new MessageWriter();
        temporary.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        for (Map.Entry<RecordDefinition, List<RecordData>> entry : dataRecords.entrySet()) {
            RecordDefinition key = entry.getKey();
            List<RecordData> valueList = entry.getValue();

            key.generateOutgoingPayload(temporary);
            for (RecordData rd :
                    valueList) {
                rd.generateOutgoingDataPayload(temporary);
            }
        }
        this.header.setDataSize(temporary.getSize());

        this.header.generateOutgoingDataPayload(writer);
        writer.writeBytes(temporary.getBytes());
        writer.writeShort(ChecksumCalculator.computeCrc(writer.getBytes(), this.header.getHeaderSize(), writer.getBytes().length - this.header.getHeaderSize()));

    }

    @Override
    public String toString() {
        return dataRecords.toString();
    }

    static class Header {
        public static final int MAGIC = 0x5449462E;

        private final int headerSize;
        private final int protocolVersion;
        private final int profileVersion;
        private final boolean hasCRC;
        private int dataSize;

        public Header(boolean hasCRC, int protocolVersion, int profileVersion) {
            this(hasCRC, protocolVersion, profileVersion, 0);
        }

        public Header(boolean hasCRC, int protocolVersion, int profileVersion, int dataSize) {
            this.hasCRC = hasCRC;
            headerSize = hasCRC ? 14 : 12;
            this.protocolVersion = protocolVersion;
            this.profileVersion = profileVersion;
            this.dataSize = dataSize;
        }

        static Header parseIncomingHeader(GarminByteBufferReader garminByteBufferReader) {
            int headerSize = garminByteBufferReader.readByte();
            if (headerSize < 12) {
                throw new IllegalArgumentException("Too short header in FIT file.");
            }
            boolean hasCRC = headerSize == 14;
            int protocolVersion = garminByteBufferReader.readByte();
            int profileVersion = garminByteBufferReader.readShort();
            int dataSize = garminByteBufferReader.readInt();
            int magic = garminByteBufferReader.readInt();
            if (magic != MAGIC) {
                throw new IllegalArgumentException("Wrong magic header in FIT file");
            }
            if (hasCRC) {
                int incomingCrc = garminByteBufferReader.readShort();

                if (incomingCrc != ChecksumCalculator.computeCrc(garminByteBufferReader.asReadOnlyBuffer(), 0, headerSize - 2)) {
                    throw new IllegalArgumentException("Wrong CRC for header in FIT file");
                }
                //            LOG.info("Fit File Header didn't have CRC, no check performed.");
            }
            return new Header(hasCRC, protocolVersion, profileVersion, dataSize);
        }

        public int getHeaderSize() {
            return headerSize;
        }

        public int getDataSize() {
            return dataSize;
        }

        public void setDataSize(int dataSize) {
            this.dataSize = dataSize;
        }

        public void generateOutgoingDataPayload(MessageWriter writer) {
            writer.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            writer.writeByte(headerSize);
            writer.writeByte(protocolVersion);
            writer.writeShort(profileVersion);
            writer.writeInt(dataSize);
            writer.writeInt(MAGIC);//magic
            if (hasCRC)
                writer.writeShort(ChecksumCalculator.computeCrc(writer.getBytes(), 0, writer.getBytes().length));
        }

    }
}
