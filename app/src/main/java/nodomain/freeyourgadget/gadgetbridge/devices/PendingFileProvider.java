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
package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.PendingFile;
import nodomain.freeyourgadget.gadgetbridge.entities.PendingFileDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public final class PendingFileProvider {
    private final DaoSession mSession;
    private final GBDevice mDevice;

    public PendingFileProvider(final GBDevice device, final DaoSession session) {
        mDevice = device;
        mSession = session;
    }

    @NonNull
    public List<PendingFile> getAllPendingFiles() {
        final QueryBuilder<PendingFile> qb = mSession.getPendingFileDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(mDevice, mSession);
        if (dbDevice == null) {
            // no device, no pending files
            return Collections.emptyList();
        }
        final Property deviceProperty = PendingFileDao.Properties.DeviceId;
        qb.where(deviceProperty.eq(dbDevice.getId()));
        final List<PendingFile> ret = qb.build().list();
        mSession.getPendingFileDao().detachAll();
        return ret;
    }

    public void removePendingFile(final String path) {
        final PendingFile pendingFile = findByPath(path);
        if (pendingFile != null) {
            pendingFile.delete();
        }
    }

    public void addPendingFile(final String path) {
        final PendingFile existingFile = findByPath(path);
        if (existingFile != null) {
            return;
        }

        final Device device = DBHelper.getDevice(mDevice, mSession);

        final PendingFile pendingFile = new PendingFile();
        pendingFile.setPath(path);
        pendingFile.setDevice(device);

        addPendingFile(pendingFile);
    }

    public void addPendingFile(final PendingFile pendingFile) {
        mSession.getPendingFileDao().insertOrReplace(pendingFile);
    }

    @Nullable
    private PendingFile findByPath(final String path) {
        final Device device = DBHelper.getDevice(mDevice, mSession);

        final PendingFileDao pendingFileDao = mSession.getPendingFileDao();
        final QueryBuilder<PendingFile> qb = pendingFileDao.queryBuilder();
        qb.where(PendingFileDao.Properties.DeviceId.eq(device.getId()));
        qb.where(PendingFileDao.Properties.Path.eq(path));
        final List<PendingFile> pendingFiles = qb.build().list();
        if (!pendingFiles.isEmpty()) {
            return pendingFiles.get(0);
        }
        return null;
    }
}
