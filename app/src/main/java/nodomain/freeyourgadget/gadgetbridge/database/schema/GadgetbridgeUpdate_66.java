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
package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepTimeSampleDao;

public class GadgetbridgeUpdate_66 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        final List<String> newColumns = Arrays.asList(
                XiaomiSleepTimeSampleDao.Properties.TotalDuration.columnName,
                XiaomiSleepTimeSampleDao.Properties.DeepSleepDuration.columnName,
                XiaomiSleepTimeSampleDao.Properties.LightSleepDuration.columnName,
                XiaomiSleepTimeSampleDao.Properties.RemSleepDuration.columnName,
                XiaomiSleepTimeSampleDao.Properties.AwakeDuration.columnName
        );

        for (final String newColumn : newColumns) {
            if (!DBHelper.existsColumn(XiaomiSleepTimeSampleDao.TABLENAME, newColumn, db)) {
                final String SQL_ALTER_TABLE = String.format(
                        Locale.ROOT,
                        "ALTER TABLE %s ADD COLUMN %s INTEGER",
                        XiaomiSleepTimeSampleDao.TABLENAME,
                        newColumn
                );
                db.execSQL(SQL_ALTER_TABLE);
            }
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
