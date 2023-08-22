/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.network;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InternetHelper {
    private static final Logger LOG = LoggerFactory.getLogger(InternetHelper.class);

    private final Context mContext;

    public InternetHelper(final Context context) {
        this.mContext = context;
    }

    public String getPackageName() {
        return "nodomain.freeyourgadget.internethelper";
    }

    public String getPermission() {
        return getPackageName() + ".INTERNET";
    }

    public boolean requestPermissions(final Activity activity) {
        if (ActivityCompat.checkSelfPermission(mContext, getPermission()) != PackageManager.PERMISSION_GRANTED) {
            LOG.warn("No permission to access internet, requesting");
            ActivityCompat.requestPermissions(activity, new String[]{getPermission()}, 0);
            return false;
        }

        return true;
    }
}
