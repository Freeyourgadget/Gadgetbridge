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

            bufferMusicSpec = extractMusicSpec(controller.getMetadata());
            bufferMusicStateSpec = extractMusicStateSpec(controller.getPlaybackState());
        } catch (final SecurityException e) {
            LOG.warn("No permission to get media sessions - did not grant notification access?", e);
        } catch (final Exception e) {
            LOG.error("Failed to get media info", e);
        }
    }

    public static MusicSpec extractMusicSpec(final MediaMetadata d) {
        final MusicSpec musicSpec = new MusicSpec();

        try {
            if (d.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
                musicSpec.artist = d.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if (d.containsKey(MediaMetadata.METADATA_KEY_ALBUM))
                musicSpec.album = d.getString(MediaMetadata.METADATA_KEY_ALBUM);
            if (d.containsKey(MediaMetadata.METADATA_KEY_TITLE))
                musicSpec.track = d.getString(MediaMetadata.METADATA_KEY_TITLE);
            if (d.containsKey(MediaMetadata.METADATA_KEY_DURATION))
                musicSpec.duration = (int) d.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000;
            if (d.containsKey(MediaMetadata.METADATA_KEY_NUM_TRACKS))
                musicSpec.trackCount = (int) d.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS);
            if (d.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
                musicSpec.trackNr = (int) d.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER);
        } catch (final Exception e) {
            LOG.error("Failed to extract music spec", e);
        }

        return musicSpec;
    }

    public static MusicStateSpec extractMusicStateSpec(final PlaybackState s) {
        final MusicStateSpec stateSpec = new MusicStateSpec();

        try {
            stateSpec.position = (int) (s.getPosition() / 1000);
            stateSpec.playRate = Math.round(100 * s.getPlaybackSpeed());
            stateSpec.repeat = MusicStateSpec.STATE_UNKNOWN;
            stateSpec.shuffle = MusicStateSpec.STATE_UNKNOWN;
            switch (s.getState()) {
                case PlaybackState.STATE_PLAYING:
                    stateSpec.state = MusicStateSpec.STATE_PLAYING;
                    break;
                case PlaybackState.STATE_STOPPED:
                    stateSpec.state = MusicStateSpec.STATE_STOPPED;
                    break;
                case PlaybackState.STATE_PAUSED:
                    stateSpec.state = MusicStateSpec.STATE_PAUSED;
                    break;
                default:
                    stateSpec.state = MusicStateSpec.STATE_UNKNOWN;
                    break;
            }
        } catch (final Exception e) {
            LOG.error("Failed to extract music state spec", e);
        }

        return stateSpec;
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
