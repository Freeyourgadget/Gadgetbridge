/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class AbstractHuami2021FWInstallHandler extends AbstractMiBandFWInstallHandler {
    public AbstractHuami2021FWInstallHandler(final Uri uri, final Context context) {
        super(uri, context);
    }

    @Override
    public void onStartInstall(GBDevice device) {
        // Unset the firmware bytes
        // Huami2021 firmwares are large (> 130MB). With the current architecture, the update operation
        // will re-read them to memory, and we run out-of-memory.
        helper.unsetFwBytes();
    }
}
