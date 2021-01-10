/*  Copyright (C) 2020-2021 Daniel Dakhno

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
