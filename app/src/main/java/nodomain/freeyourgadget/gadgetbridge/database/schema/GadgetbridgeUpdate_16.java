/*  Copyright (C) 2016-2017 Andreas Shimokawa

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
import android.os.Build;

import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;


// This is all JUST TO change the HR column to nullable - :(

public class GadgetbridgeUpdate_16 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE MI_BAND_ACTIVITY_SAMPLE_TEMP (" +
                "TIMESTAMP INTEGER  NOT NULL, " +
                "DEVICE_ID INTEGER  NOT NULL, " +
                "USER_ID INTEGER NOT NULL, " +
                "RAW_INTENSITY INTEGER NOT NULL, " +
                "STEPS INTEGER NOT NULL, " +
                "RAW_KIND INTEGER NOT NULL, " +
                "HEART_RATE INTEGER, " +
                "PRIMARY KEY (TIMESTAMP, DEVICE_ID) ON CONFLICT REPLACE)" +
                ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? " WITHOUT ROWID;" : ";")
        );

        db.execSQL("INSERT INTO MI_BAND_ACTIVITY_SAMPLE_TEMP SELECT * FROM MI_BAND_ACTIVITY_SAMPLE;");
        db.execSQL("DROP TABLE MI_BAND_ACTIVITY_SAMPLE;");
        db.execSQL("ALTER TABLE MI_BAND_ACTIVITY_SAMPLE_TEMP RENAME TO MI_BAND_ACTIVITY_SAMPLE;");

        db.execSQL("CREATE TABLE HPLUS_HEALTH_ACTIVITY_SAMPLE_TEMP (" +
                "TIMESTAMP INTEGER NOT NULL, " +
                "DEVICE_ID INTEGER NOT NULL, " +
                "USER_ID INTEGER NOT NULL, " +
                "RAW_HPLUS_HEALTH_DATA BLOB, " +
                "RAW_KIND INTEGER  NOT NULL, " +
                "RAW_INTENSITY INTEGER NOT NULL, " +
                "STEPS INTEGER NOT NULL, " +
                "HEART_RATE INTEGER, " +
                "DISTANCE INTEGER, " +
                "CALORIES INTEGER, " +
                "PRIMARY KEY (TIMESTAMP, DEVICE_ID) ON CONFLICT REPLACE)" +
                ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? " WITHOUT ROWID;" : ";")
        );
        db.execSQL("INSERT INTO HPLUS_HEALTH_ACTIVITY_SAMPLE_TEMP SELECT * FROM HPLUS_HEALTH_ACTIVITY_SAMPLE;");
        db.execSQL("DROP TABLE HPLUS_HEALTH_ACTIVITY_SAMPLE;");
        db.execSQL("ALTER TABLE HPLUS_HEALTH_ACTIVITY_SAMPLE_TEMP RENAME TO HPLUS_HEALTH_ACTIVITY_SAMPLE;");
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
