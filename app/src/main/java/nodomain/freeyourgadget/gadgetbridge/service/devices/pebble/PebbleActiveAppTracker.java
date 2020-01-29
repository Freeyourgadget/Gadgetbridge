/*  Copyright (C) 2019-2020 Matej Drobnič

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
