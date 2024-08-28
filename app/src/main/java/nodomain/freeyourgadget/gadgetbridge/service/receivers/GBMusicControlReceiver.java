/*  Copyright (C) 2015-2024 Andreas Shimokawa, Benjamin Swartley, Carsten
    Pfeiffer, Daniele Gobbetti, Gabe Schrecker, José Rebelo, Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.view.KeyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class GBMusicControlReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBMusicControlReceiver.class);

    public static final String ACTION_MUSICCONTROL = "nodomain.freeyourgadget.gadgetbridge.musiccontrol";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final int event = intent.getIntExtra("event", 0);
        final GBDeviceEventMusicControl.Event musicCmd = GBDeviceEventMusicControl.Event.values()[event];
        final int keyCode;

        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (musicCmd) {
            case NEXT:
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
            case PREVIOUS:
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            case PLAY:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
                break;
            case PAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;
                break;
            case PLAYPAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case REWIND:
                keyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
                break;
            case FORWARD:
                keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                break;
            case VOLUMEUP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                sendPhoneVolume(audioManager);
                return;
            case VOLUMEDOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                sendPhoneVolume(audioManager);
                return;
            default:
                LOG.error("Unknown event {}", event);
                return;
        }

        final GBPrefs prefs = GBApplication.getPrefs();

        if (prefs.getBoolean("pref_deprecated_media_control", false)) {
            // Deprecated path - mb_intents works for some players and not others, and vice-versa

            final long eventTime = SystemClock.uptimeMillis();

            if (prefs.getBoolean("mb_intents", false)) {
                String audioPlayer = getAudioPlayer(context);

                LOG.debug("Sending key press {} to {}", musicCmd, audioPlayer);

                final Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                final KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
                downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                if (!"default".equals(audioPlayer)) {
                    downIntent.setPackage(audioPlayer);
                }
                context.sendOrderedBroadcast(downIntent, null);

                final Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                final KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                if (!"default".equals(audioPlayer)) {
                    upIntent.setPackage(audioPlayer);
                }
                context.sendOrderedBroadcast(upIntent, null);
            } else {
                LOG.debug("Sending key press {} generally", musicCmd);
                final KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
                audioManager.dispatchMediaKeyEvent(downEvent);

                final KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        } else {
            try {
                final MediaSessionManager mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
                final List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                        new ComponentName(context, NotificationListener.class)
                );

                if (controllers.isEmpty()) {
                    LOG.warn("No media controller found to handle {}", musicCmd);
                    return;
                }

                final MediaController controller = controllers.get(0);

                switch (musicCmd) {
                    case NEXT:
                        controller.getTransportControls().skipToNext();
                        return;
                    case PREVIOUS:
                        controller.getTransportControls().skipToPrevious();
                        return;
                    case PLAY:
                        controller.getTransportControls().play();
                        return;
                    case PAUSE:
                        controller.getTransportControls().pause();
                        return;
                    case PLAYPAUSE:
                        final PlaybackState playbackState = controller.getPlaybackState();
                        if (playbackState != null) {
                            switch (playbackState.getState()) {
                                case PlaybackState.STATE_NONE:
                                case PlaybackState.STATE_STOPPED:
                                case PlaybackState.STATE_PAUSED:
                                case PlaybackState.STATE_FAST_FORWARDING:
                                case PlaybackState.STATE_REWINDING:
                                    controller.getTransportControls().play();
                                    return;
                                case PlaybackState.STATE_PLAYING:
                                    controller.getTransportControls().pause();
                                    return;
                                default:
                                    return;
                            }
                        } else {
                            LOG.warn("Failed to determine playback state, attempting to play");
                            controller.getTransportControls().play();
                        }
                        return;
                    case REWIND:
                        controller.getTransportControls().rewind();
                        return;
                    case FORWARD:
                        controller.getTransportControls().fastForward();
                }
            } catch (final SecurityException e) {
                LOG.warn("Failed to get media controller - did not grant notification access?", e);
            } catch (final Exception e) {
                LOG.error("Failed to get media controller", e);
            }
        }
    }

    private static void sendPhoneVolume(final AudioManager audioManager) {
        final int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int volumePercentage = (byte) Math.round(100 * (volumeLevel / (float) volumeMax));

        GBApplication.deviceService().onSetPhoneVolume(volumePercentage);
    }

    @Deprecated
    private static String getAudioPlayer(final Context context) {
        final Prefs prefs = GBApplication.getPrefs();
        final String audioPlayer = prefs.getString("audio_player", "default");
        final MediaSessionManager mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        try {
            final List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                    new ComponentName(context, NotificationListener.class)
            );

            if (controllers.isEmpty()) {
                LOG.warn("No media controller available");
                return audioPlayer;
            }

            return controllers.get(0).getPackageName();
        } catch (final SecurityException e) {
            LOG.warn("No permission to get media sessions - did not grant notification access?", e);
        } catch (final Exception e) {
            LOG.error("Failed to get media controller", e);
        }

        return audioPlayer;
    }
}
