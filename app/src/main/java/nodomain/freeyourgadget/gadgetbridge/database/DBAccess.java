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
