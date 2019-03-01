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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

public class AppManagerFragmentCache extends AbstractAppManagerFragment {
    @Override
    public void refreshList() {
        appList.clear();
        appList.addAll(getCachedApps(null));
    }

    @Override
    protected boolean isCacheManager() {
        return true;
    }

    @Override
    protected List<GBDeviceApp> getSystemAppsInCategory() {
        return null;
    }

    @Override
    public String getSortFilename() {
        return "pbwcacheorder.txt";
    }

    @Override
    protected boolean filterApp(GBDeviceApp gbDeviceApp) {
        return true;
    }
}
