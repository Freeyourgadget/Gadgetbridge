package nodomain.freeyourgadget.gadgetbridge.activities;

import java.util.Locale;

public interface GBActivity {
    void setLanguage(Locale language, boolean recreate);
    void setTheme(int themeId);

}
