package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GBTextToSpeech {
    private static final Logger LOG = LoggerFactory.getLogger(GBTextToSpeech.class);
    private final Context context;
    private TextToSpeech textToSpeech;
    private boolean isConnected = false;
    private final AudioManager audioManager;
    private int audioFocus;

    public GBTextToSpeech(Context context, UtteranceProgressListener callback, int audioFocus) {
        this.context = context;
        initializeTTS(callback);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.audioFocus = audioFocus;
    }

    public void setAudioFocus(int audioFocus) {
        this.audioFocus = audioFocus;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void initializeTTS(UtteranceProgressListener callback) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if (textToSpeech.getVoice() == null) {
                    LOG.error("TTS returned error: No voice available.");
                } else {
                    this.isConnected = true;
                    textToSpeech.setOnUtteranceProgressListener(callback);
                }
            } else {
                LOG.error("TTS returned error: Initialization failed.");
            }
        });
    }

    public void speak(String text) {
        Bundle params = new Bundle();
        // Put the audio stream type into the Bundle
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_RING);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "call");
    }

    public void speakNotification(String text) {
        int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, this.audioFocus);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED != result)
            LOG.warn("AudioManager did not grant us the requested focus");
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "notification");
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public void abandonFocus() {
        audioManager.abandonAudioFocus(null);
    }
}
