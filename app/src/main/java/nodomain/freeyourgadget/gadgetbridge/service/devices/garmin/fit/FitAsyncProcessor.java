package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import android.content.Context;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.PendingFileProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class FitAsyncProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FitAsyncProcessor.class);

    private final Context context;
    private final GBDevice gbDevice;
    private final Handler handler;

    public FitAsyncProcessor(final Context context, final GBDevice gbDevice) {
        this.context = context;
        this.gbDevice = gbDevice;
        this.handler = new Handler(context.getMainLooper());
    }

    /**
     * Process a list of files asynchronously. Callback is executed on the UI thread.
     */
    public void process(final List<File> files, final Callback callback) {
        LOG.debug("Starting processor for {} files", files.size());

        new Thread(() -> {
            try {
                int i = 0;
                for (final File file : files) {
                    i++;
                    LOG.debug("Parsing {}", file);

                    final int finalI = i;
                    FitAsyncProcessor.this.handler.post(() -> callback.onProgress(finalI));

                    try {
                        final FitImporter fitImporter = new FitImporter(context, gbDevice);
                        fitImporter.importFile(file);
                    } catch (final Exception ex) {
                        LOG.error("Exception while importing {}", file, ex);
                        continue; // do not remove from pending files
                    }

                    try (DBHandler handler = GBApplication.acquireDB()) {
                        final DaoSession session = handler.getDaoSession();

                        final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);

                        pendingFileProvider.removePendingFile(file.getPath());
                    } catch (final Exception e) {
                        LOG.error("Exception while removing pending file {}", file, e);
                    }
                }
            } catch (final Exception e) {
                LOG.error("Failed to parse from storage", e);
            }

            FitAsyncProcessor.this.handler.post(callback::onFinish);
        }).start();
    }

    public interface Callback {
        void onProgress(final int perc);

        void onFinish();
    }
}
