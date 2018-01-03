/*  Copyright (C) 2015-2017 Carsten Pfeiffer

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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

/**
 * Catches otherwise uncaught exceptions, logs them and terminates the app.
 */
public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler mDelegate;

    public LoggingExceptionHandler(Thread.UncaughtExceptionHandler delegate) {
        mDelegate = delegate;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LOG.error("Uncaught exception: " + ex.getMessage(), ex);
        // flush the log buffers and stop logging
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();

        if (mDelegate != null) {
            mDelegate.uncaughtException(thread, ex);
        } else {
            System.exit(1);
        }
    }
}
