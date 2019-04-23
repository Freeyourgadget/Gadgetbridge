package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public class PebbleActiveAppTracker {
    private @Nullable UUID mPreviousRunningApp = null;
    private @Nullable UUID mCurrentRunningApp = null;

    @Nullable
    public UUID getPreviousRunningApp() {
        return mPreviousRunningApp;
    }

    @Nullable
    public UUID getCurrentRunningApp() {
        return mCurrentRunningApp;
    }

    public void markAppClosed(@NonNull UUID app) {
        if (mCurrentRunningApp == app) {
            if (mPreviousRunningApp != null) {
                markAppOpened(mPreviousRunningApp);
            } else {
                mCurrentRunningApp = null;
            }
        }
    }

    public void markAppOpened(@NonNull UUID openedApp) {
        if (openedApp.equals(mCurrentRunningApp)) {
            return;
        }

        mPreviousRunningApp = mCurrentRunningApp;
        mCurrentRunningApp = openedApp;
    }
}
