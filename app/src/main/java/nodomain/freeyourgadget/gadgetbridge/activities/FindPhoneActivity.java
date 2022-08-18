/*  Copyright (C) 2018-2020 Andreas Shimokawa, Carsten Pfeiffer, Cre3per,
    Daniele Gobbetti

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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;


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

    Vibrator mVibrator;
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

        GB.removeNotification(GB.NOTIFICATION_ID_PHONE_FIND, this);

        vibrate();
        playRingtone();
    }

    private void vibrate(){
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        long[] vibrationPattern = new long[]{ 1000, 1000 };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, 0);

            mVibrator.vibrate(vibrationEffect);
        } else {
            mVibrator.vibrate(vibrationPattern, 0);
        }
    }

    private void playRingtone() {
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }
        mp = new MediaPlayer();

        if (!playConfiguredRingtone()) {
            playFallbackRingtone();
        }

        if (mAudioManager != null) {
            userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), AudioManager.FLAG_PLAY_SOUND);
        }
    }

    private void stopVibration() {
        mVibrator.cancel();
    }

    /**
     * Attempt to play the configured ringtone. This fails to get the default ringtone on some ROMs
     * (https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/2697)
     *
     * @return whether playing the configured ringtone was successful or not.
     */
    private boolean playConfiguredRingtone() {
        try {
            Uri ringtoneUri = Uri.parse(GBApplication.getPrefs().getString(GBPrefs.PING_TONE, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()));
            mp.setDataSource(this, ringtoneUri);
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (final Exception e) {
            LOG.warn("Failed to play configured ringtone", e);
            return false;
        }

        return true;
    }

    private void playFallbackRingtone() {
        try {
            final AssetFileDescriptor afd = getBaseContext().getResources().openRawResourceFd(R.raw.ping_tone);
            if (afd == null) {
                LOG.error("Failed to load fallback ringtone");
                return;
            }

            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (final Exception e) {
            LOG.warn("Failed to play fallback ringtone", e);
        }
    }

    private void stopSound() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, userVolume, AudioManager.FLAG_PLAY_SOUND);
        mp.stop();
        mp.reset();
        mp.release();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopVibration();
        stopSound();

        GBApplication.deviceService().onPhoneFound();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
    }
}
