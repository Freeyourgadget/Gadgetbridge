/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer

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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.schema.SchemaMigration;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;

public class DBOpenHelper extends DaoMaster.OpenHelper {
    private final String updaterClassNamePrefix;
    private final Context context;

    public DBOpenHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory) {
        super(context, dbName, factory);
        updaterClassNamePrefix = dbName + "Update_";
        this.context = context;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DaoMaster.createAllTables(db, true);
        new SchemaMigration(updaterClassNamePrefix).onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DaoMaster.createAllTables(db, true);
        new SchemaMigration(updaterClassNamePrefix).onDowngrade(db, oldVersion, newVersion);
    }

    public Context getContext() {
        return context;
    }
}
