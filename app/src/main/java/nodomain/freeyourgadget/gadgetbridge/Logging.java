/*  Copyright (C) 2016-2021 Carsten Pfeiffer, Daniele Gobbetti, Pavel Elagin

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
package nodomain.freeyourgadget.gadgetbridge;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;

public abstract class Logging {
    // Only used for tests
    public static final String PROP_LOGFILES_DIR = "GB_LOGFILES_DIR";

    private String logDirectory;
    private FileAppender<ILoggingEvent> fileLogger;

    public void setupLogging(boolean enable) {
        try {
            if (!isFileLoggerInitialized()) {
                init();
            }
            if (enable) {
                startFileLogger();
            } else {
                stopFileLogger();
            }
            getLogger().info("Gadgetbridge version: {}-{}", BuildConfig.VERSION_NAME, BuildConfig.GIT_HASH_SHORT);
        } catch (Exception ex) {
            Log.e("GBApplication", "External files dir not available, cannot log to file", ex);
            stopFileLogger();
        }
    }

    public String getLogPath() {
        if (fileLogger != null)
            return fileLogger.getFile();
        else
            return null;
    }

    public boolean isFileLoggerInitialized() {
        return logDirectory != null;
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
        Log.i("GBApplication", "Initializing logging");
        logDirectory = createLogDirectory();
        if (logDirectory == null) {
            throw new IllegalArgumentException("log directory must not be null");
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(Logging.class);
    }

    private void startFileLogger() {
        if (fileLogger != null) {
            Log.w("GBApplication", "Logger already started");
            return;
        }

        if (logDirectory == null) {
            Log.e("GBApplication", "Can't start file logging without a log directory");
            return;
        }

        final FileAppender fileAppender = createFileAppender(logDirectory);
        fileAppender.start();
        attachLogger(fileAppender);
        fileLogger = fileAppender;
    }

    private void stopFileLogger() {
        if (fileLogger == null) {
            return;
        }

        if (fileLogger.isStarted()) {
            fileLogger.stop();
        }

        detachLogger(fileLogger);

        fileLogger = null;
    }

    private void attachLogger(Appender<ILoggingEvent> logger) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (!root.isAttached(logger)) {
                root.addAppender(logger);
            }
        } catch (Throwable ex) {
            Log.e("GBApplication", "Error attaching logger appender", ex);
        }
    }

    private void detachLogger(Appender<ILoggingEvent> logger) {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (logger != null && root.isAttached(logger)) {
                root.detachAppender(logger);
            }
        } catch (Throwable ex) {
            Log.e("GBApplication", "Error detaching logger appender", ex);
        }
    }

    public FileAppender<ILoggingEvent> getFileLogger() {
        return fileLogger;
    }

    public static String formatBytes(byte[] bytes) {
        if (bytes == null) {
            return "(null)";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 5);
        for (byte b : bytes) {
            builder.append(String.format("0x%02x", b));
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    public static void logBytes(Logger logger, byte[] value) {
        if (value != null) {
            logger.warn("DATA: " + GB.hexdump(value, 0, value.length));
        }
    }

    public static FileAppender createFileAppender(final String logDirectory) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final PatternLayoutEncoder ple = new PatternLayoutEncoder();
        //ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{1} - %msg%n");
        ple.setContext(lc);
        ple.start();

        final SizeAndTimeBasedRollingPolicy rollingPolicy = new SizeAndTimeBasedRollingPolicy();
        final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<ILoggingEvent>();

        rollingPolicy.setContext(lc);
        rollingPolicy.setFileNamePattern(logDirectory + "/gadgetbridge-%d{yyyy-MM-dd}.%i.log.zip");
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setMaxFileSize(FileSize.valueOf("2MB"));
        rollingPolicy.setMaxHistory(10);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("100MB"));
        rollingPolicy.start();

        fileAppender.setContext(lc);
        fileAppender.setName("FILE");
        fileAppender.setLazy(true);
        fileAppender.setFile(logDirectory + "/gadgetbridge.log");
        fileAppender.setEncoder(ple);
        // to debug crashes, set immediateFlush to true, otherwise keep it false to improve throughput
        fileAppender.setImmediateFlush(false);
        fileAppender.setRollingPolicy(rollingPolicy);

        return fileAppender;
    }
}
