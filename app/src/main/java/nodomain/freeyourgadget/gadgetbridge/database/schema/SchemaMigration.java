/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer

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
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SchemaMigration {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaMigration.class);
    private final String classNamePrefix;

    public SchemaMigration(String updaterClassNamePrefix) {
        classNamePrefix = updaterClassNamePrefix;
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.info("ActivityDatabase: schema upgrade requested from " + oldVersion + " to " + newVersion);
        try {
            for (int i = oldVersion + 1; i <= newVersion; i++) {
                DBUpdateScript updater = getUpdateScript(db, i);
                if (updater != null) {
                    LOG.info("upgrading activity database to version " + i);
                    updater.upgradeSchema(db);
                }
            }
            LOG.info("activity database is now at version " + newVersion);
        } catch (RuntimeException ex) {
            GB.toast("Error upgrading database.", Toast.LENGTH_SHORT, GB.ERROR, ex);
            throw ex; // reject upgrade
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.info("ActivityDatabase: schema downgrade requested from " + oldVersion + " to " + newVersion);
        try {
            for (int i = oldVersion; i >= newVersion; i--) {
                DBUpdateScript updater = getUpdateScript(db, i);
                if (updater != null) {
                    LOG.info("downgrading activity database to version " + (i - 1));
                    updater.downgradeSchema(db);
                }
            }
            LOG.info("activity database is now at version " + newVersion);
        } catch (RuntimeException ex) {
            GB.toast("Error downgrading database.", Toast.LENGTH_SHORT, GB.ERROR, ex);
            throw ex; // reject downgrade
        }
    }

    private DBUpdateScript getUpdateScript(SQLiteDatabase db, int version) {
        try {
            Class<?> updateClass = getClass().getClassLoader().loadClass(getClass().getPackage().getName() + "." + classNamePrefix + version);
            return (DBUpdateScript) updateClass.newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error instantiating DBUpdate class for version " + version, e);
        }
    }
}
