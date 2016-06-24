package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

public class AppManagerFragmentInstalledApps extends AbstractAppManagerFragment {
    @Override
    protected void refreshList() {
        appList.clear();
        ArrayList uuids = AppManagerActivity.getUuidsFromFile(getSortFilename());
        if (uuids.isEmpty()) {
            appList.addAll(getSystemApps());
            for (GBDeviceApp gbDeviceApp : appList) {
                uuids.add(gbDeviceApp.getUUID());
            }
            AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuids);
        } else {
            appList.addAll(getCachedApps(uuids));
        }
    }

    @Override
    protected boolean isCacheManager() {
        return false;
    }

    @Override
    protected String getSortFilename() {
        return mGBDevice.getAddress() + ".watchapps";
    }

    @Override
    protected void onChangedAppOrder() {
        super.onChangedAppOrder();
        sendOrderToDevice(mGBDevice.getAddress() + ".watchfaces");
    }

    @Override
    protected boolean filterApp(GBDeviceApp gbDeviceApp) {
        return gbDeviceApp.getType() == GBDeviceApp.Type.APP_ACTIVITYTRACKER || gbDeviceApp.getType() == GBDeviceApp.Type.APP_GENERIC;
    }
}
