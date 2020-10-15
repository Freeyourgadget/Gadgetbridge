package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.translation;

import androidx.annotation.NonNull;

public class TranslationItem {
    private String originalTranslation;
    private String translated;

    public TranslationItem(String originalTranslation, String translated) {
        this.originalTranslation = originalTranslation;
        this.translated = translated;
    }

    public String getOriginal() {
        return originalTranslation;
    }

    public void setOriginalTranslation(String originalTranslation) {
        this.originalTranslation = originalTranslation;
    }

    public String getTranslated() {
        return translated;
    }

    public void setTranslated(String translated) {
        this.translated = translated;
    }

    @NonNull
    @Override
    public String toString() {
        return this.originalTranslation + " => " + this.translated;
    }
}
