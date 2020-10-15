package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.translation;

import nodomain.freeyourgadget.gadgetbridge.service.btle.Transaction;

public class TranslationData {
    private String locale;
    private TranslationItem[] translations;

    public TranslationData(String locale, TranslationItem[] translations) {
        this.locale = locale;
        this.translations = translations;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public TranslationItem[] getTranslations() {
        return translations;
    }

    public void setTranslations(TranslationItem[] translations) {
        this.translations = translations;
    }

    public void replaceByOriginal(String originalKey, String translated){
        for(TranslationItem translationItem : this.translations){
            if(translationItem.getOriginal().equals(originalKey)){
                translationItem.setTranslated(translated);
            }
        }
    }

    public void replaceByTranslated(String translatedOld, String translatedNew){
        for(TranslationItem translationItem : this.translations){
            if(translationItem.getTranslated().equals(translatedOld)){
                translationItem.setTranslated(translatedNew);
            }
        }
    }
}
