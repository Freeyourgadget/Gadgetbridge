package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;

public class MusicPlaybackReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlaybackReceiver.class);
    private static MusicSpec lastMusicSpec = new MusicSpec();

    @Override
    public void onReceive(Context context, Intent intent) {
        MusicSpec musicSpec = new MusicSpec();
        musicSpec.artist = intent.getStringExtra("artist");
        musicSpec.album = intent.getStringExtra("album");
        musicSpec.track = intent.getStringExtra("track");
        if (!lastMusicSpec.equals(musicSpec)) {
            lastMusicSpec = musicSpec;
            LOG.info("Update Music Info: " + musicSpec.artist + " / " + musicSpec.album + " / " + musicSpec.track);
            GBApplication.deviceService().onSetMusicInfo(musicSpec);
        } else {
            LOG.info("got metadata changed intent, but nothing changed, ignoring.");
        }
    }
}
