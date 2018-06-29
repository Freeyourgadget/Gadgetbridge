/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, JohnnySun

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
package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

/**
 * Provides lowlevel access to the database.
 */
public interface DBHandler extends AutoCloseable {
    /**
     * Closes the database.
     */
    void closeDb();

    /**
     * Opens the database. Note that this is only possible after an explicit
     * #closeDb(). Initially the db is implicitly open.
     */
    void openDb();

    SQLiteOpenHelper getHelper();

    /**
     * Releases the DB handler. No DB access will be possible before
     * #openDb() will be called.
     */
    void close() throws Exception;

    SQLiteDatabase getDatabase();

    DaoMaster getDaoMaster();
    DaoSession getDaoSession();
}
