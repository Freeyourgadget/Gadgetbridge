package nodomain.freeyourgadget.gadgetbridge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GBApplication extends Application {
    private static GBApplication context;

    public GBApplication() {
        context = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupLogging();
//        For debugging problems with the logback configuration
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//         print logback's internal status
//        StatusPrinter.print(lc);
//        Logger logger = LoggerFactory.getLogger(GBApplication.class);
    }

    public static boolean isFileLoggingEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        return prefs.getBoolean(GBApplication.getContext().getString(R.string.pref_log_to_file), false);
    }

    private void setupLogging() {
        if (isFileLoggingEnabled()) {
            File dir = getExternalFilesDir(null);
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            // used by assets/logback.xml since the location cannot be statically determined
            System.setProperty("GB_LOGFILES_DIR", dir.getAbsolutePath());
        } else {
            System.setProperty("GB_LOGFILES_DIR", "/dev/null"); // just to please logback configuration, not used at all
            try {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                root.detachAppender("FILE");
            } catch (Throwable ex) {
                System.out.println("Error removing logger FILE appender");
                ex.printStackTrace();
            }
        }
    }

    public static Context getContext() {
        return context;
    }
}
