/*  Copyright (C) 2023 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;

public class MediaManager {
    private static final Logger LOG = LoggerFactory.getLogger(MediaManager.class);

    private final Context context;

    private MusicSpec bufferMusicSpec = null;
    private MusicStateSpec bufferMusicStateSpec = null;

    public MediaManager(final Context context) {
        this.context = context;
    }

    public MusicSpec getBufferMusicSpec() {
        return bufferMusicSpec;
    }

    public MusicStateSpec getBufferMusicStateSpec() {
        return bufferMusicStateSpec;
    }

    /**
     * Returns true if the spec changed, so the device should be updated.
     */
    public boolean onSetMusicState(final MusicStateSpec stateSpec) {
        if (stateSpec != null && !stateSpec.equals(bufferMusicStateSpec)) {
            bufferMusicStateSpec = stateSpec;
            return true;
        }

        return false;
    }

    /**
     * Returns true if the spec changed, so the device should be updated.
     */
    public boolean onSetMusicInfo(MusicSpec musicSpec) {
        if (musicSpec != null && !musicSpec.equals(bufferMusicSpec)) {
            bufferMusicSpec = musicSpec;
            if (bufferMusicStateSpec != null) {
                bufferMusicStateSpec.state = 0;
                bufferMusicStateSpec.position = 0;
            }
            return true;
        }
        return false;
    }

    public void refresh() {
        LOG.info("Refreshing media state");

        final MediaSessionManager mediaSessionManager =
                (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);

        try {
            final List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                    new ComponentName(context, NotificationListener.class)
            );
            if (controllers.isEmpty()) {
                LOG.debug("No media controller available");
                return;
            }
            final MediaController controller = controllers.get(0);

            final MediaMetadata metadata = controller.getMetadata();
            final PlaybackState playbackState = controller.getPlaybackState();

            final MusicSpec musicSpec = new MusicSpec();
            musicSpec.artist = StringUtils.ensureNotNull(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
            musicSpec.album = StringUtils.ensureNotNull(metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            musicSpec.track = StringUtils.ensureNotNull(metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
            musicSpec.trackNr = (int) metadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER);
            musicSpec.trackCount = (int) metadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS);
            musicSpec.duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000;

            final MusicStateSpec stateSpec = new MusicStateSpec();
            switch (playbackState.getState()) {
                case PlaybackState.STATE_PLAYING:
                case PlaybackState.STATE_FAST_FORWARDING:
                case PlaybackState.STATE_REWINDING:
                case PlaybackState.STATE_BUFFERING:
                case PlaybackState.STATE_CONNECTING:
                case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                case PlaybackState.STATE_SKIPPING_TO_NEXT:
                case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                    stateSpec.state = MusicStateSpec.STATE_PLAYING;
                    break;
                case PlaybackState.STATE_PAUSED:
                    stateSpec.state = MusicStateSpec.STATE_PAUSED;
                    break;
                case PlaybackState.STATE_STOPPED:
                case PlaybackState.STATE_ERROR:
                    stateSpec.state = MusicStateSpec.STATE_STOPPED;
                    break;
                case PlaybackState.STATE_NONE:
                default:
                    stateSpec.state = MusicStateSpec.STATE_UNKNOWN;
            }
            stateSpec.position = (int) playbackState.getPosition() / 1000;
            stateSpec.playRate = (int) (playbackState.getPlaybackSpeed() * 100);
            stateSpec.repeat = MusicStateSpec.STATE_UNKNOWN;
            stateSpec.shuffle = MusicStateSpec.STATE_UNKNOWN;

            bufferMusicStateSpec = stateSpec;
            bufferMusicSpec = musicSpec;
        } catch (final SecurityException e) {
            LOG.warn("No permission to get media sessions - did not grant notification access?", e);
        }
    }

    public int getPhoneVolume() {
        return getPhoneVolume(context);
    }

    public static int getPhoneVolume(final Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        final int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return Math.round(100 * (volumeLevel / (float) volumeMax));
    }
}
