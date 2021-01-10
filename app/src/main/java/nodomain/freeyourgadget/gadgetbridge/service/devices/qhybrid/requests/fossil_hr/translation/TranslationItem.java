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
