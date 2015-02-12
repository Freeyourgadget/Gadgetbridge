package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;

public class GBMusicControlReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getSimpleName();

    public static final String ACTION_MUSICCONTROL = "nodomain.freeyourgadget.gadgetbridge.musiccontrol";

    @Override
    public void onReceive(Context context, Intent intent) {
        GBCommand command = GBCommand.values()[intent.getIntExtra("command", 0)];
        int keyCode;
        switch (command) {
            case MUSIC_NEXT:
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
            case MUSIC_PREVIOUS:
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            case MUSIC_PLAY:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
                break;
            case MUSIC_PAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;
                break;
            case MUSIC_PLAYPAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            default:
                return;
        }

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
