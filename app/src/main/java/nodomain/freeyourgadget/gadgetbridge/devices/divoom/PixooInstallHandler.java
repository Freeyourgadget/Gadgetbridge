/*  Copyright (C) 2023 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.divoom;

import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class PixooInstallHandler implements InstallHandler {
    private final Context mContext;
    private final Uri mUri;

    public PixooInstallHandler(Uri uri, Context context) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(GBDevice device) {

    }
}
