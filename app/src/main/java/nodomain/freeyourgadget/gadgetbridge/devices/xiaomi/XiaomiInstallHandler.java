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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType.AGPS_UIHH;

import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;

public class XiaomiInstallHandler implements InstallHandler {
    protected final Uri mUri;
    protected final Context mContext;

    public XiaomiInstallHandler(final Uri uri, final Context context) {
        this.mUri = uri;
        this.mContext = context;
    }

    @Override
    public boolean isValid() {
        // TODO
        return false;
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        // TODO
    }

    @Override
    public void onStartInstall(final GBDevice device) {
        // nothing to do
    }
}
