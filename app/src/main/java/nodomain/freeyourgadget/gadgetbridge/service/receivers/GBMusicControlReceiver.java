package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
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
            Prefs prefs = GBApplication.getPrefs();
            String audioPlayer = prefs.getString("audio_player", "default");

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
            upIntent.setPackage(audioPlayer);
            context.sendOrderedBroadcast(upIntent, null);
        }
    }
}
