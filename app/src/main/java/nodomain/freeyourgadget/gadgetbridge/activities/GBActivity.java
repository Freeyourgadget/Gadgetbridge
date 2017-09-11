package nodomain.freeyourgadget.gadgetbridge.activities;

import java.util.Locale;

public interface GBActivity {
    void setLanguage(Locale language, boolean invalidateLanguage);
    void setTheme(int themeId);

}
