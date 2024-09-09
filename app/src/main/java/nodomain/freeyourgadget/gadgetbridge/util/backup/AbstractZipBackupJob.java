/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.backup;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.util.gson.GsonUtcDateAdapter;

public abstract class AbstractZipBackupJob implements Runnable {
    public static final String METADATA_FILENAME = "gadgetbridge.json";
    public static final String DATABASE_FILENAME = "database/Gadgetbridge";
    public static final String PREFS_GLOBAL_FILENAME = "preferences/global.json";
    public static final String PREFS_DEVICE_FILENAME = "preferences/device_%s.json";
    public static final String EXTERNAL_FILES_FOLDER = "files";

    public static final int VERSION = 1;

    protected static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonUtcDateAdapter())
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private final Context mContext;
    private final Handler mHandler;
    private final ZipBackupCallback mCallback;

    private final AtomicBoolean aborted = new AtomicBoolean(false);

    private long lastProgressUpdateTs;
    private long lastProgressUpdateMessage;

    public AbstractZipBackupJob(final Context context, final ZipBackupCallback callback) {
        this.mContext = context;
        this.mHandler = new Handler(context.getMainLooper());
        this.mCallback = callback;
    }

    public Context getContext() {
        return mContext;
    }

    public void abort() {
        aborted.set(true);
    }

    public boolean isAborted() {
        return aborted.get();
    }

    @WorkerThread
    protected void updateProgress(final int percentage, @StringRes final int message, final Object... formatArgs) {
        final long now = System.currentTimeMillis();
        if (percentage != 100 && now - lastProgressUpdateTs < 1000L) {
            // Avoid updating the notification too frequently, but still do if the message changed
            if (lastProgressUpdateMessage == message) {
                return;
            }
        }
        lastProgressUpdateTs = now;
        lastProgressUpdateMessage = message;
        mHandler.post(() -> {
            mCallback.onProgress(percentage, getContext().getString(message, formatArgs));
        });
    }

    @WorkerThread
    protected void onSuccess(final String warnings) {
        mHandler.post(() -> mCallback.onSuccess(warnings));
    }

    @WorkerThread
    protected void onFailure(final String errorMessage) {
        mHandler.post(() -> mCallback.onFailure(errorMessage));
    }
}
