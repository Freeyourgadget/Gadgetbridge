/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Gabe Schrecker

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.SystemClock;
import android.view.KeyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class GBMusicControlReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBMusicControlReceiver.class);

    public static final String ACTION_MUSICCONTROL = "nodomain.freeyourgadget.gadgetbridge.musiccontrol";

    @Override
    public void onReceive(Context context, Intent intent) {
        GBDeviceEventMusicControl.Event musicCmd = GBDeviceEventMusicControl.Event.values()[intent.getIntExtra("event", 0)];
        int keyCode = -1;
        int volumeAdjust = AudioManager.ADJUST_LOWER;

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
            case VOLUMEUP:
                // change default and fall through, :P
                volumeAdjust = AudioManager.ADJUST_RAISE;
            case VOLUMEDOWN:
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, volumeAdjust, 0);
                break;
            default:
                return;
        }

        if (keyCode != -1) {
            String audioPlayer = getAudioPlayer(context);

            LOG.debug("keypress: " + musicCmd.toString() + " sent to: " + audioPlayer);

            long eventtime = SystemClock.uptimeMillis();

            Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, keyCode, 0);
            downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
            if (!"default".equals(audioPlayer)) {
                downIntent.setPackage(audioPlayer);
            }
            context.sendOrderedBroadcast(downIntent, null);

            Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, keyCode, 0);
            upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
            if (!"default".equals(audioPlayer)) {
                upIntent.setPackage(audioPlayer);
            }
            context.sendOrderedBroadcast(upIntent, null);
        }
    }

    private String getAudioPlayer(Context context) {
        Prefs prefs = GBApplication.getPrefs();
        String audioPlayer = prefs.getString("audio_player", "default");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaSessionManager mediaSessionManager =
                    (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);

            List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                    new ComponentName(context, NotificationListener.class));
            try {
                MediaController controller = controllers.get(0);
                audioPlayer = controller.getPackageName();
            } catch (IndexOutOfBoundsException e) {
                LOG.error("IndexOutOfBoundsException: " + e.getMessage());
            }
        }
        return audioPlayer;
    }
}
