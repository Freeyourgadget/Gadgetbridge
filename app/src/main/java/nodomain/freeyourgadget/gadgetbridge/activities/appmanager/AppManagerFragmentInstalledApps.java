package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;

public class AppManagerFragmentInstalledApps extends AbstractAppManagerFragment {

    @Override
    protected List<GBDeviceApp> getSystemAppsInCategory() {
        List<GBDeviceApp> systemApps = new ArrayList<>();
        //systemApps.add(new GBDeviceApp(UUID.fromString("4dab81a6-d2fc-458a-992c-7a1f3b96a970"), "Sports (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        //systemApps.add(new GBDeviceApp(UUID.fromString("cf1e816a-9db0-4511-bbb8-f60c48ca8fac"), "Golf (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        systemApps.add(new GBDeviceApp(UUID.fromString("1f03293d-47af-4f28-b960-f2b02a6dd757"), "Music (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        systemApps.add(new GBDeviceApp(UUID.fromString("b2cae818-10f8-46df-ad2b-98ad2254a3c1"), "Notifications (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        systemApps.add(new GBDeviceApp(UUID.fromString("67a32d95-ef69-46d4-a0b9-854cc62f97f9"), "Alarms (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
        systemApps.add(new GBDeviceApp(UUID.fromString("18e443ce-38fd-47c8-84d5-6d0c775fbe55"), "Watchfaces (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));

        if (mGBDevice != null) {
            if (PebbleUtils.hasHealth(mGBDevice.getModel())) {
                systemApps.add(new GBDeviceApp(UUID.fromString("0863fc6a-66c5-4f62-ab8a-82ed00a98b5d"), "Send Text (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                systemApps.add(new GBDeviceApp(PebbleProtocol.UUID_PEBBLE_HEALTH, "Health (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
            }
            if (PebbleUtils.hasHRM(mGBDevice.getModel())) {
                systemApps.add(new GBDeviceApp(PebbleProtocol.UUID_WORKOUT, "Workout (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
            }
            if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) >= 4) {
                systemApps.add(new GBDeviceApp(PebbleProtocol.UUID_WEATHER, "Weather (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
            }
        }

        return systemApps;
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
