package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MusicPlaybackReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String artist = intent.getStringExtra("artist");
        String album = intent.getStringExtra("album");
        String track = intent.getStringExtra("track");

        Log.i(TAG, "Current track: " + artist + ", " + album + ", " + track);

        Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
        startIntent.setAction(BluetoothCommunicationService.ACTION_SETMUSICINFO);
        startIntent.putExtra("music_artist", artist);
        startIntent.putExtra("music_album", album);
        startIntent.putExtra("music_track", track);
        context.startService(startIntent);
    }
}
