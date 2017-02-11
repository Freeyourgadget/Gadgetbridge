package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;

public class MusicPlaybackReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlaybackReceiver.class);
    private static MusicSpec lastMusicSpec = new MusicSpec();
    private static MusicStateSpec lastStateSpec = new MusicStateSpec();

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
        MusicSpec musicSpec = new MusicSpec(lastMusicSpec);
        MusicStateSpec stateSpec = new MusicStateSpec(lastStateSpec);

        Bundle incomingBundle = intent.getExtras();
        for (String key : incomingBundle.keySet()) {
            Object incoming = incomingBundle.get(key);
            if (incoming instanceof String && "artist".equals(key)) {
                musicSpec.artist = (String) incoming;
            } else if (incoming instanceof String && "album".equals(key)) {
                musicSpec.album = (String) incoming;
            } else if (incoming instanceof String && "track".equals(key)) {
                musicSpec.track = (String) incoming;
            } else if (incoming instanceof String && "title".equals(key) && musicSpec.track == null) {
                musicSpec.track = (String) incoming;
            } else if (incoming instanceof Integer && "duration".equals(key)) {
                musicSpec.duration = (Integer) incoming / 1000;
            } else if (incoming instanceof Long && "duration".equals(key)) {
                musicSpec.duration = ((Long) incoming).intValue() / 1000;
            } else if (incoming instanceof Integer && "position".equals(key)) {
                stateSpec.position = (Integer) incoming / 1000;
            } else if (incoming instanceof Long && "position".equals(key)) {
                stateSpec.position = ((Long) incoming).intValue() / 1000;
            } else if (incoming instanceof Boolean && "playing".equals(key)) {
                stateSpec.state = (byte) (((Boolean) incoming) ? MusicStateSpec.STATE_PLAYING : MusicStateSpec.STATE_PAUSED);
            } else if (incoming instanceof String && "duration".equals(key)) {
                musicSpec.duration = Integer.valueOf((String) incoming) / 1000;
            } else if (incoming instanceof String && "trackno".equals(key)) {
                musicSpec.trackNr = Integer.valueOf((String) incoming);
            } else if (incoming instanceof String && "totaltrack".equals(key)) {
                musicSpec.trackCount = Integer.valueOf((String) incoming);
            } else if (incoming instanceof Integer && "pos".equals(key)) {
                stateSpec.position = (Integer) incoming;
            }
        }

        if (!lastMusicSpec.equals(musicSpec)) {
            lastMusicSpec = musicSpec;
            LOG.info("Update Music Info: " + musicSpec.artist + " / " + musicSpec.album + " / " + musicSpec.track);
            GBApplication.deviceService().onSetMusicInfo(musicSpec);
        } else {
            LOG.info("Got metadata changed intent, but nothing changed, ignoring.");
        }

        if (!lastStateSpec.equals(stateSpec)) {
            lastStateSpec = stateSpec;
            LOG.info("Update Music State: state=" + stateSpec.state + ", position= " + stateSpec.position);
            GBApplication.deviceService().onSetMusicState(stateSpec);
        } else {
            LOG.info("Got state changed intent, but not enough has changed, ignoring.");
        }
    }
}
