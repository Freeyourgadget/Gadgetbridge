package nodomain.freeyourgadget.gadgetbridge;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.util.StatusPrinter;

public abstract class Logging {
    public static final String PROP_LOGFILES_DIR = "GB_LOGFILES_DIR";

    private FileAppender<ILoggingEvent> fileLogger;

    public void setupLogging(boolean enable) {
        try {
            if (fileLogger == null) {
                init();
            }
            if (enable) {
                startFileLogger();
            } else {
                stopFileLogger();
            }
            getLogger().info("Gadgetbridge version: " + BuildConfig.VERSION_NAME);
        } catch (IOException ex) {
            Log.e("GBApplication", "External files dir not available, cannot log to file", ex);
            stopFileLogger();
        }
    }

    public void debugLoggingConfiguration() {
        // For debugging problems with the logback configuration
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        // print logback's internal status
        StatusPrinter.print(lc);
//        Logger logger = LoggerFactory.getLogger(Logging.class);
    }

    protected abstract String createLogDirectory() throws IOException;

    protected void init() throws IOException {
        String dir = createLogDirectory();
        if (dir == null) {
            throw new IllegalArgumentException("log directory must not be null");
        }
        // used by assets/logback.xml since the location cannot be statically determined
        System.setProperty(PROP_LOGFILES_DIR, dir);
        rememberFileLogger();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(Logging.class);
    }

    private void startFileLogger() {
        if (fileLogger != null && !fileLogger.isStarted()) {
            addFileLogger(fileLogger);
            fileLogger.setLazy(false); // hack to make sure that start() actually opens the file
            fileLogger.start();
        }
    }

    private void stopFileLogger() {
        if (fileLogger != null && fileLogger.isStarted()) {
            fileLogger.stop();
            removeFileLogger(fileLogger);
        }
    }

    private void rememberFileLogger() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        fileLogger = (FileAppender<ILoggingEvent>) root.getAppender("FILE");
    }

    private void addFileLogger(Appender<ILoggingEvent> fileLogger) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (!root.isAttached(fileLogger)) {
                root.addAppender(fileLogger);
            }
        } catch (Throwable ex) {
            Log.e("GBApplication", "Error adding logger FILE appender", ex);
        }
    }

    private void removeFileLogger(Appender<ILoggingEvent> fileLogger) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (root.isAttached(fileLogger)) {
                root.detachAppender(fileLogger);
            }
        } catch (Throwable ex) {
            Log.e("GBApplication", "Error removing logger FILE appender", ex);
        }
    }

    public FileAppender<ILoggingEvent> getFileLogger() {
        return fileLogger;
    }

    public boolean setImmediateFlush(boolean enable) {
        FileAppender<ILoggingEvent> fileLogger = getFileLogger();
        Encoder<ILoggingEvent> encoder = fileLogger.getEncoder();
        if (encoder instanceof LayoutWrappingEncoder) {
            ((LayoutWrappingEncoder) encoder).setImmediateFlush(enable);
            return true;
        }
        return false;
    }

    public boolean isImmediateFlush() {
        FileAppender<ILoggingEvent> fileLogger = getFileLogger();
        Encoder<ILoggingEvent> encoder = fileLogger.getEncoder();
        if (encoder instanceof LayoutWrappingEncoder) {
            return ((LayoutWrappingEncoder) encoder).isImmediateFlush();
        }
        return false;
    }

    public static String formatBytes(byte[] bytes) {
        if (bytes == null) {
            return "(null)";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 5);
        for (byte b : bytes) {
            builder.append(String.format("0x%2x", b));
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    public static void logBytes(Logger logger, byte[] value) {
        if (value != null) {
            for (byte b : value) {
                logger.warn("DATA: " + String.format("0x%2x", b));
            }
        }
    }
}
