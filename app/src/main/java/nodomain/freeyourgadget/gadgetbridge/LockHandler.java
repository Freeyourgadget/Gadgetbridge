/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer, Taavi Eomäe

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

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

/**
 * Provides low-level access to the database.
 */
public class LockHandler implements DBHandler {

    private DaoMaster daoMaster = null;
    private DaoSession session = null;
    private SQLiteOpenHelper helper = null;

    public LockHandler() {
    }

    public void init(DaoMaster daoMaster, DaoMaster.OpenHelper helper) {
        if (isValid()) {
            throw new IllegalStateException("DB must be closed before initializing it again");
        }
        if (daoMaster == null) {
            throw new IllegalArgumentException("daoMaster must not be null");
        }
        if (helper == null) {
            throw new IllegalArgumentException("helper must not be null");
        }
        this.daoMaster = daoMaster;
        this.helper = helper;

        session = daoMaster.newSession();
        if (session == null) {
            throw new RuntimeException("Unable to create database session");
        }
    }

    @Override
    public DaoMaster getDaoMaster() {
        return daoMaster;
    }

    private boolean isValid() {
        return daoMaster != null;
    }

    private void ensureValid() {
        if (!isValid()) {
            throw new IllegalStateException("LockHandler is not in a valid state");
        }
    }

    @Override
    public void close() {
        ensureValid();
        GBApplication.releaseDB();
    }

    @Override
    public synchronized void openDb() {
        if (session != null) {
            throw new IllegalStateException("session must be null");
        }
        // this will create completely new db instances and in turn update this handler through #init()
        GBApplication.app().setupDatabase();
    }

    @Override
    public synchronized void closeDb() {
        if (session == null) {
            throw new IllegalStateException("session must not be null");
        }
        session.clear();
        session.getDatabase().close();
        session = null;
        helper = null;
        daoMaster = null;
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        ensureValid();
        return helper;
    }

    @Override
    public DaoSession getDaoSession() {
        ensureValid();
        return session;
    }

    @Override
    public SQLiteDatabase getDatabase() {
        ensureValid();
        return daoMaster.getDatabase();
    }
}
