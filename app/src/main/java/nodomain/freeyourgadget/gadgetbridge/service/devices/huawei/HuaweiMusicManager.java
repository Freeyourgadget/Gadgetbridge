package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceMusicData;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceMusicUpdate;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiMusicUtils;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusic;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusicPlaylist;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetMusicInfoParams;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetMusicList;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetMusicPlaylist;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetMusicPlaylistMusics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendMusicOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendUploadMusicFileInfoResponse;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

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

    private boolean syncMusicData = false;
    private int frameCount = 0;
    private int endFrame = 65535;
    private int currentFrame = 0;


    public void startSyncMusicData() {
        syncMusicData = true;
        try {
            GetMusicInfoParams getMusicInfoParams = new GetMusicInfoParams(this.support);
            getMusicInfoParams.doPerform();
        } catch (IOException e) {
            LOG.error("Get music info: {}", e.getMessage());
            syncMusicData = false;
        }
    }


    private void syncMusicList() {
        if (!syncMusicData) {
            this.currentFrame = 0;
            return;
        }
        int count = this.frameCount;
        if (support.getHuaweiCoordinator().supportsMoreMusic()) {
            count = Math.min(this.frameCount, 250);
        }
        if (this.currentFrame < count) {
            try {
                GetMusicList getMusicList = new GetMusicList(this.support, this.currentFrame, this.endFrame);
                getMusicList.doPerform();
            } catch (IOException e) {
                LOG.error("Get music list: {}", e.getMessage());
                endMusicListSync();
            }
        } else {
            endMusicListSync();
        }
    }

    private void endMusicListSync() {
        this.currentFrame = 0;
        try {
            GetMusicPlaylist getMusicPlaylist = new GetMusicPlaylist(this.support);
            getMusicPlaylist.doPerform();
        } catch (IOException e) {
            LOG.error("Get music playlist: {}", e.getMessage());
            endMusicPlaylistSync();
        }
    }

    private void endMusicPlaylistSync() {
        this.currentPlaylistIndex = 0;
        this.currentPlaylistFrame = 0;
        tempPlaylistMusic.clear();

        musicPlaylistMusicSync();
    }

    private final List<MusicControl.MusicPlaylists.Response.PlaylistData> devicePlaylists = new ArrayList<>();

    private int currentPlaylistIndex = 0;
    private int currentPlaylistFrame = 0;
    private final List<List<Integer>> tempPlaylistMusic = new ArrayList<>();

    private void musicPlaylistMusicSync() {
        if (this.currentPlaylistIndex < devicePlaylists.size()) {
            MusicControl.MusicPlaylists.Response.PlaylistData playlist = devicePlaylists.get(this.currentPlaylistIndex);
            syncPlaylistMusicsOne(playlist.id, playlist.frameCount);
        } else {
            musicPlaylistMusicDone();
        }
    }

    private void syncPlaylistMusicsOne(int id, int frameCount) {
        if (this.currentPlaylistFrame < frameCount) {
            try {
                GetMusicPlaylistMusics getMusicPlaylistMusics = new GetMusicPlaylistMusics(this.support, id, this.currentPlaylistFrame);
                getMusicPlaylistMusics.doPerform();
            } catch (IOException e) {
                LOG.error("Get music playlist musics: {}", e.getMessage());
                musicPlaylistMusicDone();
            }
        } else {
            syncPlayListMusicIndexDone(id, frameCount);
        }
    }

    public void syncNextPlaylistMusicIndex() {
        this.currentPlaylistFrame++;
        MusicControl.MusicPlaylists.Response.PlaylistData playlist = devicePlaylists.get(this.currentPlaylistIndex);
        syncPlaylistMusicsOne(playlist.id, playlist.frameCount);
    }

    private void syncPlayListMusicIndexDone(int id, int frameCount) {
        MusicControl.MusicPlaylists.Response.PlaylistData playlist = devicePlaylists.get(this.currentPlaylistIndex);

        ArrayList<Integer> musics = new ArrayList<>();
        if (this.tempPlaylistMusic.size() == frameCount) {
            for (int i = 0; i < frameCount; i++) {
                musics.addAll(this.tempPlaylistMusic.get(i));
            }
        }

        GBDeviceMusicPlaylist pl = new GBDeviceMusicPlaylist(playlist.id, playlist.name, musics);
        List<GBDeviceMusicPlaylist> list = new ArrayList<>();
        list.add(pl);
        sendMusicPlaylist(list);
        this.currentPlaylistIndex++;
        this.currentPlaylistFrame = 0;
        this.tempPlaylistMusic.clear();
        musicPlaylistMusicSync();
    }

    private void musicPlaylistMusicDone() {
        this.currentPlaylistIndex = 0;
        this.currentPlaylistFrame = 0;
        this.tempPlaylistMusic.clear();

        this.syncMusicData = false;
        sendMusicSyncDone();
    }

    public void onMusicMusicInfoParams(HuaweiMusicUtils.MusicCapabilities capabilities, int frameCount, List<HuaweiMusicUtils.PageStruct> pageStruct) {
        //TODO: research and use pageStruct. It may/should be used to retrieve music data from devices by pages.
        // without it list can be incomplete, but I can't confirm this.
        LOG.info("FrameCount: {}, pageStruct: {}", frameCount, pageStruct);
        support.getHuaweiCoordinator().setMusicInfoParams(capabilities);
        if(syncMusicData) {
            this.frameCount = frameCount;
            this.currentFrame = 0;
            this.endFrame = 65535;
            String formats = null;
            if(capabilities.supportedFormats != null) {
                formats = String.join(",", capabilities.supportedFormats);
            }
            int maxPlaylistCount = 0;
            if(support.getCoordinator().getHuaweiCoordinator().getExtendedMusicInfoParams() != null) {
                maxPlaylistCount = support.getCoordinator().getHuaweiCoordinator().getExtendedMusicInfoParams().maxPlaylistCount;
            }
            sendMusicSyncStart(support.getContext().getString(R.string.music_huawei_device_info, formats, capabilities.availableSpace), capabilities.maxMusicCount, maxPlaylistCount);
            syncMusicList();
        }
    }

    private void sendMusicSyncStart(final String info, int maxMusicCount, int maxPlaylistCount) {
        final GBDeviceMusicData musicListCmd = new GBDeviceMusicData();
        musicListCmd.type = 1;
        musicListCmd.deviceInfo = info;
        musicListCmd.maxMusicCount = maxMusicCount;
        musicListCmd.maxPlaylistCount = maxPlaylistCount;
        support.evaluateGBDeviceEvent(musicListCmd);
    }


    private void sendMusicList(List<GBDeviceMusic> list) {
        final GBDeviceMusicData musicListCmd = new GBDeviceMusicData();
        musicListCmd.type = 2;
        musicListCmd.list = list;
        support.evaluateGBDeviceEvent(musicListCmd);
    }

    private void sendMusicPlaylist(List<GBDeviceMusicPlaylist> list) {
        final GBDeviceMusicData musicListCmd = new GBDeviceMusicData();
        musicListCmd.type = 2;
        musicListCmd.playlists = list;
        support.evaluateGBDeviceEvent(musicListCmd);
    }

    private void sendMusicSyncDone() {
        final GBDeviceMusicData musicListCmd = new GBDeviceMusicData();
        musicListCmd.type = 10;
        support.evaluateGBDeviceEvent(musicListCmd);
    }

    public void onMusicListResponse(int startFrame, int endFrame, List<GBDeviceMusic> list) {
        sendMusicList(list);
        if (support.getHuaweiCoordinator().supportsMoreMusic() || !(endFrame == this.endFrame || list.size() == 1)) {
            if (list.size() == 2) {
                this.endFrame = list.get(1).getId();
            }
            this.currentFrame++;
            syncMusicList();
            return;
        }
        endMusicListSync();
    }

    public void onMusicPlaylistResponse(List<MusicControl.MusicPlaylists.Response.PlaylistData> playlists) {
        this.devicePlaylists.clear();
        for(MusicControl.MusicPlaylists.Response.PlaylistData pl: playlists) {
            if(pl.id != 0) {
                this.devicePlaylists.add(pl);
            }
        }
        endMusicPlaylistSync();
    }

    public void onMusicPlaylistMusics(int id, int index, List<Integer> musicIds) {
        this.tempPlaylistMusic.add(musicIds);
        syncNextPlaylistMusicIndex();
    }

    public void onMusicOperation(int operation, int playlistIndex, String playlistName, ArrayList<Integer> musicIds) {
        LOG.info("music operation: {}", operation);
        try {
            SendMusicOperation sendMusicOperation = new SendMusicOperation(this.support, operation, playlistIndex, playlistName, musicIds);
            sendMusicOperation.doPerform();
        } catch (IOException e) {
            LOG.error("SendMusicOperation: {}", e.getMessage());
        }
    }

    public void onMusicOperationResponse(int resultCode, int operation, int playlistIndex, String playlistName, ArrayList<Integer> musicIds) {

        boolean success = true;
        if (resultCode != 0x000186A0) {
            GB.toast(support.getContext(), support.getContext().getString(R.string.music_error), Toast.LENGTH_SHORT, GB.ERROR);
            success = false;
        }

        LOG.info("music operation response: {} {}", operation, success);
        final GBDeviceMusicUpdate updateCmd = new GBDeviceMusicUpdate();
        updateCmd.success = success;
        updateCmd.operation = operation;
        updateCmd.playlistIndex = playlistIndex;
        updateCmd.playlistName = playlistName;
        updateCmd.musicIds = musicIds;

        support.evaluateGBDeviceEvent(updateCmd);
    }
}
