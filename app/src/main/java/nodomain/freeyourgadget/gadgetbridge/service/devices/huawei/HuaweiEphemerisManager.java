package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisDataRequestResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisFileConsultResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisFileListResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisFileStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisFileUploadChunk;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisFileUploadDoneResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisOperatorResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisParameterConsultRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendEphemerisSingleFileInfoResponse;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class HuaweiEphemerisManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiEphemerisManager.class);

    public static class UploadParameters {
        //upload file related data
        private final String protocolVersion;
        private final byte bitmapEnable;
        private final short transferSize;
        private final int maxDataSize;
        private final short timeOut;
        private final byte fileType;

        public UploadParameters(String protocolVersion, byte bitmapEnable, short transferSize, int maxDataSize, short timeOut, byte fileType) {
            this.protocolVersion = protocolVersion;
            this.bitmapEnable = bitmapEnable;
            this.transferSize = transferSize;
            this.maxDataSize = maxDataSize;
            this.timeOut = timeOut;
            this.fileType = fileType;
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public byte getBitmapEnable() {
            return bitmapEnable;
        }

        public short getTransferSize() {
            return transferSize;
        }

        public int getMaxDataSize() {
            return maxDataSize;
        }

        public short getTimeOut() {
            return timeOut;
        }

        public byte getFileType() {
            return fileType;
        }
    }

    public static class RequestInfo {
        private int tagVersion = -1;
        private String tagUUID = null;
        private List<String> tagFiles = null;

        private byte[] currentFileData = null;
        private String currentFileName = null;

        UploadParameters uploadParameters = null;

        private List<String> processedFiles = new ArrayList<>();

        public RequestInfo(int tagVersion, String tagUUID, List<String> tagFiles) {
            this.tagVersion = tagVersion;
            this.tagUUID = tagUUID;
            this.tagFiles = tagFiles;
        }

        public int getTagVersion() {
            return tagVersion;
        }

        public String getTagUUID() {
            return tagUUID;
        }

        public List<String> getTagFiles() {
            return tagFiles;
        }

        public byte[] getCurrentFileData() {
            return currentFileData;
        }

        public String getCurrentFileName() {
            return currentFileName;
        }

        public UploadParameters getUploadParameters() {
            return uploadParameters;
        }

        public void setCurrentFileData(byte[] currentFileData) {
            this.currentFileData = currentFileData;
        }

        public void setCurrentFileName(String currentFileName) {
            this.currentFileName = currentFileName;
        }

        public void setUploadParameters(UploadParameters uploadParameters) {
            this.uploadParameters = uploadParameters;
        }

        public void addProcessedFile(String name) {
            processedFiles.add(name);
        }

        public boolean isAllProcessed() {
            LOG.info("Ephemeris tagFiles: {}", tagFiles.toString());
            LOG.info("Ephemeris processed: {}", processedFiles.toString());
            return processedFiles.size() == tagFiles.size() && new HashSet<>(processedFiles).containsAll(tagFiles);
        }
    }

    private final HuaweiSupportProvider support;


    private JsonObject availableDataConfig = null;


    private RequestInfo currentRequest = null;


    public HuaweiEphemerisManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    private byte[] getZIPFileContent(File file, String name) {
        byte[] ret = null;
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(name)) {
                    InputStream inputStream = zipFile.getInputStream(entry);
                    ret = new byte[inputStream.available()];
                    inputStream.read(ret);
                    inputStream.close();
                }
            }
            zipFile.close();
        } catch (ZipException e) {
            LOG.error("zip exception", e);
        } catch (IOException e) {
            LOG.error("zip IO exception", e);
        }
        return ret;
    }

    private boolean checkZIPFileExists(File file, String name) {
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().equals(name)) {
                    return true;
                }
            }
            zipFile.close();
        } catch (ZipException e) {
            LOG.error("zip exception", e);
        } catch (IOException e) {
            LOG.error("zip IO exception", e);
        }
        return false;
    }

    public void handleOperatorRequest(byte operationInfo, int operationTime) {
        //TODO:
        // 100007 - no network connection
        if (operationInfo == 1) {
            // NOTE: clear data on new request
            currentRequest = null;

            int responseCode = 100000;
            try {
                File filesDir = support.getContext().getExternalFilesDir(null);
                File file = new File(filesDir, "ephemeris.zip");
                if (!file.exists()) {
                    throw new Exception("Ephemeris file does not exists");
                }
                byte[] time = getZIPFileContent(file, "time");
                byte[] config = getZIPFileContent(file, "ephemeris_config.json");

                if (time == null || config == null) {
                    throw new Exception("Ephemeris no time or config in file");
                }

                long fileTime = Long.parseLong(new String(time));
                if (fileTime < (System.currentTimeMillis() - (7200 * 1000))) { // 2 hours. Maybe should be decreased.
                    throw new Exception("Ephemeris file old");
                }

                availableDataConfig = JsonParser.parseString(new String(config)).getAsJsonObject();

                LOG.info("Ephemeris Time: {} ConfigData: {}", fileTime, availableDataConfig.toString());

            } catch (Exception e) {
                LOG.error("Ephemeris exception file or config processing", e);
                availableDataConfig = null;
                //responseCode = 100007; //no network connection
                return; // NOTE: just ignore request if something wrong with data.
            }

            try {
                SendEphemerisOperatorResponse sendEphemerisOperatorResponse = new SendEphemerisOperatorResponse(this.support, responseCode);
                sendEphemerisOperatorResponse.doPerform();

                if (responseCode == 100000) {
                    SendEphemerisParameterConsultRequest sendEphemerisParameterConsultRequest = new SendEphemerisParameterConsultRequest(this.support);
                    sendEphemerisParameterConsultRequest.doPerform();
                }

            } catch (IOException e) {
                LOG.error("Error send Ephemeris data");
            }
        }

        // TODO: operation code == 2 send file download progress
        //   Currently we do not need this code because everything is downloaded already. Maybe needed in the future
        //   0x2: 02
        //   0x3: operationTime
        //   0x4:  from 01 to 06 status
        //   in the container 0x81
    }

    public void handleParameterConsultResponse(int consultDeviceTime, byte downloadVersion, String downloadTag) {
        LOG.info("consultDeviceTime: {}, downloadVersion: {}, downloadTag: {}", consultDeviceTime, downloadVersion, downloadTag);
        byte status = 0x3; // ready to download

        try {
            File filesDir = support.getContext().getExternalFilesDir(null);
            File file = new File(filesDir, "ephemeris.zip");
            if (!file.exists()) {
                throw new Exception("Ephemeris file does not exists");
            }

            if (availableDataConfig == null) {
                throw new Exception("Ephemeris no config data");
            }

            JsonObject conf = availableDataConfig.getAsJsonObject(downloadTag);

            int version = conf.get("ver").getAsInt();
            if(version != downloadVersion) {
                throw new Exception("Ephemeris version mismatch");
            }
            String uuid = conf.get("uuid").getAsString();

            if(uuid.isEmpty())
                throw new Exception("Ephemeris uuid is empty");

            JsonArray filesJs = conf.get("files").getAsJsonArray();

            List<String> files = new ArrayList<>();
            for (int i = 0; i < filesJs.size(); i++) {
                files.add(filesJs.get(i).getAsString());
            }

            if(files.isEmpty())
                throw new Exception("Ephemeris file list is empty");

            for(String fl: files) {
                if(!checkZIPFileExists(file, uuid + File.separator + fl)) {
                    throw new Exception("Ephemeris file does not exist in ZIP");
                }
            }

            currentRequest = new RequestInfo(downloadVersion, uuid, files);

        } catch (Exception e) {
            LOG.error("Ephemeris exception file or config processing", e);
            availableDataConfig = null;
            status = 0x5; //error or timeout
        }

        try {
            SendEphemerisFileStatusRequest sendEphemerisFileStatusRequest = new SendEphemerisFileStatusRequest(this.support, status);
            sendEphemerisFileStatusRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Error to send SendEphemerisFileStatusRequest");
        }
    }

    //File transfer related
    public void handleFileSendRequest(byte fileType, String productId) {
        if(currentRequest == null) {
            return;
        }

        String fileList = "";
        int responseCode = 0;

        if(fileType == 0) {
            //TODO: find all files that name contain productId and send
            LOG.error("Currently not supported. File type: 0");
        } else if(fileType == 1){
            if(currentRequest.getTagVersion() == 0) {
                //TODO: implement this type
                //fileList = "gpslocation.dat";
                LOG.error("Currently not supported. File type 1. Tag version 0");
            } else if (currentRequest.getTagVersion() == 1 || currentRequest.getTagVersion() == 2 || currentRequest.getTagVersion() == 3){
                int i = 0;
                while (i < currentRequest.getTagFiles().size()) {
                    fileList += currentRequest.getTagFiles().get(i);
                    if (i == currentRequest.getTagFiles().size() - 1) {
                        break;
                    }
                    fileList += ";";
                    i++;
                }
            } else {
                LOG.error("Unknown version");
            }
        } else {
            LOG.error("Unknown file id");
        }

        if(TextUtils.isEmpty(fileList)) {
            responseCode = 100001;
            cleanupUpload(true);
        }

        try {
            SendEphemerisFileListResponse sendEphemerisFileListResponse = new SendEphemerisFileListResponse(this.support, responseCode, fileList);
            sendEphemerisFileListResponse.doPerform();
        } catch (IOException e) {
            LOG.error("Error to send SendEphemerisFileListResponse");
        }
    }

    void handleFileConsultIncomingRequest(int responseCode, String protocolVersion, byte bitmapEnable, short transferSize, int maxDataSize, short timeOut, byte fileType) {
        if(currentRequest == null) {
            return;
        }

        if (responseCode != 0) {
            LOG.error("Error on handleFileConsultIncomingRequest: {}", responseCode);
            cleanupUpload(true);
            return;
        }

        if (transferSize == 0) {
            LOG.error("transfer size is 0");
            cleanupUpload(true);
            return;
        }

        currentRequest.setUploadParameters(new UploadParameters(protocolVersion, bitmapEnable, transferSize, maxDataSize, timeOut, fileType));

        if((!TextUtils.isEmpty(protocolVersion) && fileType == 0) || fileType == 1) {
            try {
                SendEphemerisFileConsultResponse sendEphemerisFileConsultResponse = new SendEphemerisFileConsultResponse(this.support, 100000);
                sendEphemerisFileConsultResponse.doPerform();
            } catch (IOException e) {
                LOG.error("Error to send SendEphemerisFileConsultResponse");
            }
        } else {
            // I don't know how to  properly process error in this case. Just cleanup.
            cleanupUpload(true);
        }
    }

    void handleSingleFileIncomingRequest(String filename) {
        if(currentRequest == null || currentRequest.getUploadParameters() == null) {
            cleanupUpload(true);
            return;
        }

        if(TextUtils.isEmpty(filename) || !currentRequest.getTagFiles().contains(filename)) {
            cleanupUpload(true);
            try {
                SendEphemerisSingleFileInfoResponse sendEphemerisSingleFileInfoResponse = new SendEphemerisSingleFileInfoResponse(this.support, 100001, 0, (short) 0);
                sendEphemerisSingleFileInfoResponse.doPerform();
            } catch (IOException e) {
                LOG.error("Error to send sendEphemerisSingleFileInfoResponse");
            }
            return;
        }

        currentRequest.addProcessedFile(filename);

        int responseCode = 0;
        try {

            File filesDir = support.getContext().getExternalFilesDir(null);
            File file = new File(filesDir, "ephemeris.zip");
            if (!file.exists()) {
                throw new Exception("Ephemeris handleSingleFileIncomingRequest file does not exists");
            }

            byte[] currentFileData = getZIPFileContent(file, currentRequest.getTagUUID() + File.separator + filename);
            if (currentFileData == null || currentFileData.length == 0) {
                throw new Exception("Ephemeris handleSingleFileIncomingRequest file is empty");
            }
            currentRequest.setCurrentFileData(currentFileData);
            currentRequest.setCurrentFileName(filename);
        } catch (Exception e) {
            LOG.error("Ephemeris exception handleSingleFileIncomingRequest processing", e);
            cleanupUpload(true);
            responseCode = 100001;
        }

        short crc = 0;
        if(currentRequest.getUploadParameters().getFileType() == 0) {
            //TODO: implement this type
            responseCode = 100001;
        } else if(currentRequest.getUploadParameters().getFileType()  == 1) {
            crc = (short)CheckSums.getCRC16(currentRequest.getCurrentFileData(), 0x0000);
        }

        int size = 0;
        if(currentRequest.getCurrentFileData() == null || crc == 0) {
            responseCode = 100001;
        } else {
            size = currentRequest.getCurrentFileData().length;
        }

        try {
            SendEphemerisSingleFileInfoResponse sendEphemerisSingleFileInfoResponse = new SendEphemerisSingleFileInfoResponse(this.support, responseCode, size, crc);
            sendEphemerisSingleFileInfoResponse.doPerform();
        } catch (IOException e) {
            LOG.error("Error to send SendEphemerisSingleFileInfoResponse");
        }
    }

    public static String getHexDigest(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (str.contains(":")) {
            str = str.replace(":", "");
        }
        try {
            byte[] data = CryptoUtils.digest(str.getBytes(StandardCharsets.UTF_8));
            return StringUtils.bytesToHex(data);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("EncryptUtil getEncryption :{} ", e.getMessage());
            return "";
        }
    }

    void handleDataRequestIncomingRequest(int responseCode, String fileName, int offset, int len, byte bitmap) {
        if(currentRequest == null || currentRequest.getUploadParameters() == null) {
            cleanupUpload(true);
            return;
        }

        if(offset == -1 || fileName == null || fileName.isEmpty()) {
            cleanupUpload(true);
            return;
        }

        if(!currentRequest.getCurrentFileName().equals(fileName)) {
            cleanupUpload(true);
            return;
        }

        int localResponseCode = 100000;

        String localFilename = "";

        if(currentRequest.getUploadParameters().getFileType() == 0) {
            //TODO: implement this type
            localResponseCode = 100001;
        } else if(currentRequest.getUploadParameters().getFileType() == 1) {
            localFilename = getHexDigest(support.deviceMac) + fileName;
        } else {
            LOG.error("Unsupported type: {}", currentRequest.getUploadParameters().getFileType());
            localResponseCode = 100001;
        }

        try {
            SendEphemerisDataRequestResponse sendEphemerisDataRequestResponse = new SendEphemerisDataRequestResponse(this.support, localResponseCode, localFilename, offset);
            sendEphemerisDataRequestResponse.doPerform();
        } catch (IOException e) {
            LOG.error("Error to send SendEphemerisDataRequestResponse");
        }

        if(localResponseCode == 100000) {
            int dataSize = Math.min(currentRequest.getCurrentFileData().length - offset,currentRequest.getUploadParameters().getMaxDataSize());
            int packetsCount = (int) Math.ceil((double) dataSize / currentRequest.getUploadParameters().getTransferSize());
            byte[] chunk = new byte[dataSize];
            System.arraycopy(currentRequest.getCurrentFileData(), offset, chunk, 0, dataSize);

            try {
                SendEphemerisFileUploadChunk sendEphemerisFileUploadChunk = new SendEphemerisFileUploadChunk(this.support, chunk, currentRequest.getUploadParameters().getTransferSize(), packetsCount);
                sendEphemerisFileUploadChunk.doPerform();
            } catch (IOException e) {
                LOG.error("Error to send SendEphemerisFileUploadChunk");
            }
        }
    }

    void handleFileUploadResponse(int responseCode) {
        LOG.info("handleFileUploadResponse {}", responseCode);
        if(responseCode != 100000) {
            cleanupUpload(true);
        }
    }

    void handleFileDoneRequest(byte uploadResult) {
        LOG.info("handleFileDoneRequest");
        cleanupUpload(false);
        try {
            SendEphemerisFileUploadDoneResponse sendEphemerisFileUploadDoneResponse = new SendEphemerisFileUploadDoneResponse(this.support, 100000);
            sendEphemerisFileUploadDoneResponse.doPerform();
        } catch (IOException e) {
            LOG.error("Error to send SendEphemerisFileUploadDoneResponse");
        }
    }

    void cleanupUpload(boolean force) {
        if(currentRequest != null) {
            currentRequest.setCurrentFileName(null);
            currentRequest.setCurrentFileData(null);
            currentRequest.setUploadParameters(null);
            boolean isAllProcessed = currentRequest.isAllProcessed();
            LOG.info("Ephemeris is Done: {}", isAllProcessed);
            if(isAllProcessed || force) {
                currentRequest = null;
                LOG.info("Ephemeris All files uploaded. {} Cleanup...", force?"Force":"");
            }
        }
    }

}
