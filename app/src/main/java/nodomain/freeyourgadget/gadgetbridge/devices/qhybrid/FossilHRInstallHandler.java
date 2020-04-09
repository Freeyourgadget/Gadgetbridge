/*  Copyright (C) 2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class FossilHRInstallHandler implements InstallHandler {
    private final Context mContext;
    private boolean mIsValid;

    FossilHRInstallHandler(Uri uri, Context context) {
        mContext = context;
        UriHelper uriHelper = null;
        try {
            uriHelper = UriHelper.get(uri, mContext);
        } catch (IOException e) {
            mIsValid = false;
            return;
        }
        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            byte[] bytes = new byte[16];
            in.read(bytes);
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int header0 = buf.getInt();
            int size = buf.getInt();
            int header2 = buf.getInt();
            int header3 = buf.getInt();
            if (header0 != 1 || header2 != 0x00012000 || header3 != 0x00012000) {
                mIsValid = false;
                return;
            }
        } catch (Exception e) {
            mIsValid = false;
            return;
        }
        mIsValid = true;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }
        if (device.getType() != DeviceType.FOSSILQHYBRID || !device.isConnected()) {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }
        GenericItem installItem = new GenericItem();
        installItem.setIcon(R.drawable.ic_firmware);
        installItem.setName("Fossil Hybrid HR Firmware");
        installItem.setDetails("Unknown version");

        installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, "(unknown)"));
        installActivity.setInstallEnabled(true);
        installActivity.setInstallItem(installItem);
    }


    @Override
    public void onStartInstall(GBDevice device) {
    }

    @Override
    public boolean isValid() {
        return mIsValid;
    }
}
