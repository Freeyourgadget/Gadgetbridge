package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;


public class GBTextToSpeech {
    private static final Logger LOG = LoggerFactory.getLogger(GBTextToSpeech.class);
    private final Context context;
    private TextToSpeech textToSpeech;
    private boolean isConnected = false;

    public GBTextToSpeech(Context context, UtteranceProgressListener callback) {
        this.context = context;
        initializeTTS(callback);
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void initializeTTS(UtteranceProgressListener callback) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    LOG.error("TTS returned error: Language not supported.");
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
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utteranceId");
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


}
