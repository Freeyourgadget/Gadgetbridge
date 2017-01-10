package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;

public class MusicPlaybackReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlaybackReceiver.class);
    private static MusicSpec lastMusicSpec = new MusicSpec();
    private static MusicStateSpec lastStatecSpec = new MusicStateSpec();

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        Bundle bundle = intent.getExtras();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            LOG.info(String.format("%s %s (%s)", key,
                    value != null ? value.toString() : "null", value != null ? value.getClass().getName() : "no class"));
        }
        */
        MusicSpec musicSpec = new MusicSpec();
        musicSpec.artist = intent.getStringExtra("artist");
        musicSpec.album = intent.getStringExtra("album");
        if (intent.hasExtra("track")) {
            musicSpec.track = intent.getStringExtra("track");
        }
        else if (intent.hasExtra("title")) {
            musicSpec.track = intent.getStringExtra("title");
        }

        musicSpec.duration = intent.getIntExtra("duration", 0) / 1000;

        if (!lastMusicSpec.equals(musicSpec)) {
            lastMusicSpec = musicSpec;
            LOG.info("Update Music Info: " + musicSpec.artist + " / " + musicSpec.album + " / " + musicSpec.track);
            GBApplication.deviceService().onSetMusicInfo(musicSpec);
        } else {
            LOG.info("got metadata changed intent, but nothing changed, ignoring.");
        }

        if (intent.hasExtra("position") && intent.hasExtra("playing")) {
            MusicStateSpec stateSpec = new MusicStateSpec();
            stateSpec.position = intent.getIntExtra("position", 0) / 1000;
            stateSpec.state = (byte) (intent.getBooleanExtra("playing", true) ? MusicStateSpec.STATE_PLAYING : MusicStateSpec.STATE_PAUSED);
            if (!lastStatecSpec.equals(stateSpec)) {
                LOG.info("Update Music State: state=" + stateSpec.state + ", position= " + stateSpec.position);
                GBApplication.deviceService().onSetMusicState(stateSpec);
            } else {
                LOG.info("got state changed intent, but not enough has changed, ignoring.");
            }
            lastStatecSpec = stateSpec;
        }
    }
}
