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

public class GadgetbridgeUpdate_24 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        // Probably it isbetter to hardcode then using constants since constants could change after refactoring and potentially break migration
        db.execSQL("UPDATE DEVICE SET Type=170 where (Type=80 and Manufacturer like 'Fossil%')");
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
