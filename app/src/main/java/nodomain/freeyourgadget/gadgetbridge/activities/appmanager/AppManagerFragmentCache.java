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
