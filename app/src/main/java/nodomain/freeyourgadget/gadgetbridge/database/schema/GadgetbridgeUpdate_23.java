/*  Copyright (C) 2017-2020 Andreas Shimokawa, protomors

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
package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.AlarmDao;

public class GadgetbridgeUpdate_23 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(AlarmDao.TABLENAME, AlarmDao.Properties.Snooze.columnName, db)) {
            // Setting default value of SNOOZE column to 1 (true), so that existing MiBand2 alarms
            // behave as before
            String ADD_COLUMN_SNOOZE = "ALTER TABLE " + AlarmDao.TABLENAME + " ADD COLUMN "
                    + AlarmDao.Properties.Snooze.columnName + " INTEGER NOT NULL DEFAULT 1;";
            db.execSQL(ADD_COLUMN_SNOOZE);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
