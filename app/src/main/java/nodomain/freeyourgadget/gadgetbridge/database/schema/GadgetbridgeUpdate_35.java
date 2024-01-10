/*  Copyright (C) 2021-2024 Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevelDao;

public class GadgetbridgeUpdate_35 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(BatteryLevelDao.TABLENAME, BatteryLevelDao.Properties.BatteryIndex.columnName, db)) {
            String MOVE_DATA_TO_TEMP_TABLE = "ALTER TABLE battery_level RENAME TO battery_levels_temp;";
            db.execSQL(MOVE_DATA_TO_TEMP_TABLE);

            String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS \"BATTERY_LEVEL\" (\"TIMESTAMP\" INTEGER  NOT NULL ," +
                    "\"DEVICE_ID\" INTEGER  NOT NULL ,\"LEVEL\" INTEGER NOT NULL ,\"BATTERY_INDEX\" INTEGER  NOT NULL ," +
                    "PRIMARY KEY (\"TIMESTAMP\" ,\"DEVICE_ID\" ,\"BATTERY_INDEX\" ) ON CONFLICT REPLACE) WITHOUT ROWID;";
            db.execSQL(CREATE_TABLE);

            String MIGATE_DATA = "insert into " + BatteryLevelDao.TABLENAME
                    + " (" + BatteryLevelDao.Properties.Timestamp.columnName + ","
                    + BatteryLevelDao.Properties.DeviceId.columnName + ","
                    + BatteryLevelDao.Properties.BatteryIndex.columnName + ","
                    + BatteryLevelDao.Properties.Level.columnName + ") "
                    + " select Timestamp, Device_ID, 0, Level from battery_levels_temp;";
            db.execSQL(MIGATE_DATA);

            String DROP_TEMP_TABLE = "drop table if exists battery_levels_temp";
            db.execSQL(DROP_TEMP_TABLE);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
