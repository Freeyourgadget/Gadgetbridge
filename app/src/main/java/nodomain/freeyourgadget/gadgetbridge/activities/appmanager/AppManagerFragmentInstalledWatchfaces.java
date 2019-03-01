/*  Copyright (C) 2016-2019 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

public class AppManagerFragmentInstalledWatchfaces extends AbstractAppManagerFragment {

    @Override
    protected List<GBDeviceApp> getSystemAppsInCategory() {
        List<GBDeviceApp> systemWatchfaces = new ArrayList<>();
        systemWatchfaces.add(new GBDeviceApp(UUID.fromString("8f3c8686-31a1-4f5f-91f5-01600c9bdc59"), "Tic Toc (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
        systemWatchfaces.add(new GBDeviceApp(UUID.fromString("3af858c3-16cb-4561-91e7-f1ad2df8725f"), "Kickstart (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
        return systemWatchfaces;
    }

    @Override
    protected boolean isCacheManager() {
        return false;
    }

    @Override
    protected String getSortFilename() {
        return mGBDevice.getAddress() + ".watchfaces";
    }

    @Override
    protected void onChangedAppOrder() {
        super.onChangedAppOrder();
        sendOrderToDevice(mGBDevice.getAddress() + ".watchapps");
    }

    @Override
    protected boolean filterApp(GBDeviceApp gbDeviceApp) {
        if (gbDeviceApp.getType() == GBDeviceApp.Type.WATCHFACE) {
            return true;
        }
        return false;
    }
}
