package nodomain.freeyourgadget.gadgetbridge;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler mDelegate;

    public LoggingExceptionHandler(Thread.UncaughtExceptionHandler delegate) {
        mDelegate = delegate;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LOG.error("Uncaught exception: " + ex.getMessage(), ex);
        if (mDelegate != null) {
            mDelegate.uncaughtException(thread, ex);
        } else {
            System.exit(1);
        }
    }
}
