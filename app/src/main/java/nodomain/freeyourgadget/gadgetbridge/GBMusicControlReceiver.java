package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;

import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;

public class GBMusicControlReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getSimpleName();

    public static final String ACTION_MUSICCONTROL = "nodomain.freeyourgadget.gadgetbridge.musiccontrol";

    @Override
    public void onReceive(Context context, Intent intent) {
        GBDeviceCommandMusicControl.Command musicCmd = GBDeviceCommandMusicControl.Command.values()[intent.getIntExtra("command", 0)];
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
                AudioManager audioManager =
                        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, volumeAdjust, 0);
                break;
            default:
                return;
        }

        if (keyCode != -1) {
            long eventtime = SystemClock.uptimeMillis();

            Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, keyCode, 0);
            downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
            context.sendOrderedBroadcast(downIntent, null);

            Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, keyCode, 0);
            upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
            context.sendOrderedBroadcast(upIntent, null);
        }
    }
}
