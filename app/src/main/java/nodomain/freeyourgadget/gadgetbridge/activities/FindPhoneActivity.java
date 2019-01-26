/*  Copyright (C) 2018 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.R;


public class FindPhoneActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(FindPhoneActivity.class);

    public static final String ACTION_FOUND
            = "nodomain.freeyourgadget.gadgetbridge.findphone.action.reply";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_FOUND: {
                        finish();
                        break;
                    }
                }
            }
        }
    };

    AudioManager mAudioManager;
    int userVolume;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_phone);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FOUND);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        registerReceiver(mReceiver, filter); // for ACTION_FOUND

        Button foundButton = (Button) findViewById(R.id.foundbutton);
        foundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        playRingtone();
    }

    public void playRingtone(){
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }
        mp = new MediaPlayer();

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        try {
            mp.setDataSource(this, ringtoneUri);
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (IOException ignore) {
            LOG.warn("problem playing ringtone");
        }

        if (mAudioManager != null) {
            userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), AudioManager.FLAG_PLAY_SOUND);
        }
    }

    public void stopSound() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, userVolume, AudioManager.FLAG_PLAY_SOUND);
        mp.stop();
        mp.reset();
        mp.release();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
    }
}
