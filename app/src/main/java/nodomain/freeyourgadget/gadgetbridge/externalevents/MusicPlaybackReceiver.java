package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class MusicPlaybackReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlaybackReceiver.class);

    private static String mLastSource;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int lastDot = action.lastIndexOf(".");
        String source = action.substring(0, lastDot);

        if (!source.equals(mLastSource)) {
            mLastSource = source;
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = sharedPrefs.edit();
            edit.putString("last_audiosource", mLastSource);
            LOG.info("set last audiosource to " + mLastSource);
            edit.apply();
        }

        String artist = intent.getStringExtra("artist");
        String album = intent.getStringExtra("album");
        String track = intent.getStringExtra("track");

        LOG.info("Current track: " + artist + ", " + album + ", " + track);

        GBApplication.deviceService().onSetMusicInfo(artist, album, track);
    }
}
