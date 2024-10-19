package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendUploadMusicFileInfoResponse;

public class HuaweiMusicManager {
    static Logger LOG = LoggerFactory.getLogger(HuaweiMusicManager.class);

    public static class AudioInfo {
        private final String fileName;
        private final long fileSize;
        private final String title;
        private final String artist;
        private final String extension;

        private String mimeType;

        private long duration;
        private int sampleRate;
        private int bitrate;
        private byte channels;

        //public byte musicEncode = -1;  // TODO: not sure
        //public short unknownBitrate = -1;   // TODO: not sure

        public AudioInfo(String fileName, long fileSize, String title, String artist, String extension) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.title = title;
            this.artist = artist;
            this.extension = extension;
        }

        public String getFileName() {
            return fileName;
        }

        public long getFileSize() { return fileSize;}

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public void setCharacteristics(long duration, int sampleRate, int bitrate, byte channels) {
            this.duration = duration;
            this.sampleRate = sampleRate;
            this.bitrate = bitrate;
            this.channels = channels;
        }

        public long getDuration() {
            return duration;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public int getBitrate() {
            return bitrate;
        }

        public byte getChannels() {
            return channels;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("AudioInfo{");
            sb.append("fileName='").append(fileName).append('\'');
            sb.append("fileSize='").append(fileSize).append('\'');
            sb.append(", title='").append(title).append('\'');
            sb.append(", artist='").append(artist).append('\'');
            sb.append(", mimeType='").append(mimeType).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    private final HuaweiSupportProvider support;

    private AudioInfo currentMusicInfo;


    public HuaweiMusicManager(HuaweiSupportProvider support) {
        this.support = support;
    }


    public void addUploadMusic(AudioInfo audioInfo) {
        currentMusicInfo = audioInfo;
    }

    public void uploadMusicInfo(short songIndex, String fileName) {
        AudioInfo current = currentMusicInfo;
        if(current == null || (!current.getFileName().equals(fileName))) {
            LOG.error("Upload file info does not exist.");
            return;
        }
        try {
            SendUploadMusicFileInfoResponse sendUploadMusicFileInfoResponse = new SendUploadMusicFileInfoResponse(support,
                    songIndex, current.getTitle(), current.getArtist());
            sendUploadMusicFileInfoResponse.doPerform();
        } catch (IOException e) {
            LOG.error("Could not send sendUploadMusicFileInfoResponse", e);
        }
    }
}
