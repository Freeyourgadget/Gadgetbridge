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
