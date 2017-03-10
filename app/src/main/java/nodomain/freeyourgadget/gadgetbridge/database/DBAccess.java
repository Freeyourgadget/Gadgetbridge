/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
import android.os.AsyncTask;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class DBAccess extends AsyncTask {
    private final String mTask;
    private final Context mContext;
    private Exception mError;

    public DBAccess(String task, Context context) {
        mTask = task;
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    protected abstract void doInBackground(DBHandler handler);

    @Override
    protected Object doInBackground(Object[] params) {
        try (DBHandler db = GBApplication.acquireDB()) {
            doInBackground(db);
        } catch (Exception e) {
            mError = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        if (mError != null) {
            displayError(mError);
        }
    }

    protected void displayError(Throwable error) {
        GB.toast(getContext(), getContext().getString(R.string.dbaccess_error_executing, error.getMessage()), Toast.LENGTH_LONG, GB.ERROR, error);
    }
}
