package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

public class AppManagerFragmentInstalledWatchfaces extends AbstractAppManagerFragment {
    @Override
    protected void refreshList() {
        appList.clear();
        ArrayList uuids = AppManagerActivity.getUuidsFromFile(getSortFilename());
        if (uuids.isEmpty()) {
            appList.addAll(getSystemWatchfaces());
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
