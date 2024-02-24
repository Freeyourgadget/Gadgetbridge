/*  Copyright (C) 2023-2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

/**
 * A repeating fetch operation. This operation repeats the fetch up to a certain number of times, or
 * until the fetch timestamp matches the current time. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public abstract class AbstractRepeatingFetchOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRepeatingFetchOperation.class);

    protected final HuamiFetchDataType dataType;

    public AbstractRepeatingFetchOperation(final HuamiSupport support, final HuamiFetchDataType dataType) {
        super(support);
        this.dataType = dataType;
        setName("fetching " + dataType.name());
    }

    @Override
    protected void startFetching(final TransactionBuilder builder) {
        final GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        LOG.info("start {} since {}", getName(), sinceWhen.getTime());
        startFetching(builder, dataType.getCode(), sinceWhen);
    }

    /**
     * Handle the buffered data.
     *
     * @param timestamp The timestamp of the first sample. This function should update this to the
     *                  timestamp of the last processed sample.
     * @param bytes     the buffered bytes
     * @return true on success
     */
    protected abstract boolean handleActivityData(GregorianCalendar timestamp, byte[] bytes);

    @Override
    protected boolean processBufferedData() {
        LOG.info("{} has finished round {}, got {} bytes in buffer", getName(), fetchCount, buffer.size());

        if (buffer.size() == 0) {
            return true;
        }

        final byte[] bytes = buffer.toByteArray();
        final GregorianCalendar timestamp = (GregorianCalendar) this.startTimestamp.clone();

        // Uncomment to dump the bytes to external storage for debugging
        //dumpBytesToExternalStorage(bytes, timestamp);

        final boolean handleSuccess = handleActivityData(timestamp, bytes);

        if (!handleSuccess) {
            return false;
        }

        timestamp.add(Calendar.MINUTE, 1);
        saveLastSyncTimestamp(timestamp);

        if (needsAnotherFetch(timestamp)) {
            buffer.reset();

            getSupport().getFetchOperationQueue().add(0, this);
        }

        return true;
    }

    private boolean needsAnotherFetch(final GregorianCalendar lastSyncTimestamp) {
        final long lastFetchRange = lastSyncTimestamp.getTimeInMillis() - startTimestamp.getTimeInMillis();
        if (lastFetchRange < 1000L) {
            LOG.warn("Fetch round {} of {} got {} ms of data, stopping to avoid infinite loop", fetchCount, getName(), lastFetchRange);
            return false;
        }

        if (fetchCount > 5) {
            LOG.warn("Already have {} fetch rounds for {}, not doing another one", fetchCount, getName());
            return false;
        }

        if (lastSyncTimestamp.getTimeInMillis() >= System.currentTimeMillis()) {
            LOG.warn("Not doing another fetch since last synced timestamp is in the future: {}", lastSyncTimestamp.getTime());
            return false;
        }

        LOG.info("Doing another fetch since last sync timestamp is still too old: {}", lastSyncTimestamp.getTime());
        return true;
    }

    protected void dumpBytesToExternalStorage(final byte[] bytes, final GregorianCalendar timestamp) {
        try {
            final File externalFilesDir = FileUtils.getExternalFilesDir();
            final File targetDir = new File(externalFilesDir, "rawFetchOperations");
            targetDir.mkdirs();

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);
            final String filename = getClass().getSimpleName() + "_"
                    + timestamp.getTime().getTime() + "_"
                    + dateFormat.format(timestamp.getTime()) + ".bin";

            final File outputFile = new File(targetDir, filename);

            final OutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(bytes);
            outputStream.close();
        } catch (final Exception e) {
            LOG.error("Failed to dump bytes to storage", e);
        }
    }
}
