/*  Copyright (C) 2019-2024 Andreas Shimokawa, José Rebelo

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

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class FetchDebugLogsOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchDebugLogsOperation.class);

    private FileOutputStream logOutputStream;

    public FetchDebugLogsOperation(final HuamiSupport support) {
        super(support);
        setName("fetch debug logs");
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_debug_logs);
    }

    @Override
    protected void startFetching(final TransactionBuilder builder) {
        File dir;
        try {
            dir = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            return;
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        final String filename = "huamidebug_" + dateFormat.format(new Date()) + ".log";

        File outputFile = new File(dir, filename);
        try {
            logOutputStream = new FileOutputStream(outputFile);
        } catch (IOException e) {
            LOG.warn("could not create file " + outputFile, e);
            return;
        }
        final GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        startFetching(builder, HuamiFetchDataType.DEBUG_LOGS.getCode(), sinceWhen);
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastDebugTimeMillis";
    }

    @Override
    protected boolean processBufferedData() {
        LOG.info("{} data has finished", getName());
        try {
            logOutputStream.close();
            logOutputStream = null;
        } catch (final IOException e) {
            LOG.error("could not close output stream", e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean validChecksum(int crc32) {
        // TODO actually check it?
        LOG.warn("Checksum not implemented for debug logs, assuming it's valid");
        return true;
    }

    @Override
    protected void bufferActivityData(@NonNull byte[] value) {
        try {
            logOutputStream.write(value, 1, value.length - 1);
        } catch (final IOException e) {
            LOG.error("could not write to output stream", e);
        }
    }
}
