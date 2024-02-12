/*  Copyright (C) 2021-2024 Andreas Shimokawa, Arjan Schrijver, Gordon
    Williams, José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.view.KeyEvent;

import net.osmand.aidlapi.IOsmAndAidlCallback;
import net.osmand.aidlapi.IOsmAndAidlInterface;
import net.osmand.aidlapi.gpx.AGpxBitmap;
import net.osmand.aidlapi.navigation.ADirectionInfo;
import net.osmand.aidlapi.navigation.ANavigationUpdateParams;
import net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams;
import net.osmand.aidlapi.navigation.OnVoiceNavigationParams;
import net.osmand.aidlapi.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class OsmandEventReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(OsmandEventReceiver.class);

    private final Application app;
    private IOsmAndAidlInterface mIOsmAndAidlInterface;

    private NavigationInfoSpec navigationInfoSpec = new NavigationInfoSpec();

    private final IOsmAndAidlCallback.Stub mIOsmAndAidlCallback = new IOsmAndAidlCallback.Stub() {
        @Override
        public void onSearchComplete(List<SearchResult> resultSet) {
        }

        @Override
        public void onUpdate() {
        }

        @Override
        public void onAppInitialized() {
        }

        @Override
        public void onGpxBitmapCreated(AGpxBitmap bitmap) {
        }

        @Override
        public void updateNavigationInfo(ADirectionInfo directionInfo) {
            navigationInfoSpec.nextAction = directionInfo.getTurnType();
            navigationInfoSpec.distanceToTurn = directionInfo.getDistanceTo()+"m";

            if (shouldSendNavigation()) {
                GBApplication.deviceService().onSetNavigationInfo(navigationInfoSpec);
            }

            LOG.debug("Distance: {}, turnType: {}", directionInfo.getDistanceTo(), directionInfo.getTurnType());
        }

        @Override
        public void onContextMenuButtonClicked(int buttonId, String pointId, String layerId) {
        }

        @Override
        public void onVoiceRouterNotify(OnVoiceNavigationParams params) {
            List<String> played = params.getPlayed();
            for (String instuction : played) {
                navigationInfoSpec.instruction = instuction;
                LOG.debug("instruction: {}", instuction);
                // only first one for now
                break;
            }
        }

        @Override
        public void onKeyEvent(KeyEvent keyEvent) {

        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service);
            LOG.info("OsmAnd service connected");
            registerForNavigationUpdates(true, 6666); // what is this id for?
            registerForVoiceRouterMessages(true, 6667);
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("osmand.api://GET_INFO"));
//            OsmandEventReceiver.this.startActivityForResult(intent,666);
        }

        public void onServiceDisconnected(ComponentName className) {
            mIOsmAndAidlInterface = null;
            LOG.info("OsmAnd service disconnected");
        }
    };

    public OsmandEventReceiver(Application application) {
        this.app = application;
        if (!bindService()) {
            LOG.warn("Could not bind to OsmAnd");
        }
    }

    private boolean bindService() {
        if (mIOsmAndAidlInterface == null) {
            List<CharSequence> installedOsmandPackages = findInstalledOsmandPackages();
            if (installedOsmandPackages.isEmpty()) {
                LOG.warn("OsmAnd is not installed");
                return false;
            }
            Prefs prefs = GBApplication.getPrefs();
            String packageName = prefs.getString("pref_key_osmand_packagename", "autodetect");
            if (packageName.equals("autodetect")) packageName = installedOsmandPackages.get(0).toString();
            Intent intent = new Intent("net.osmand.aidl.OsmandAidlServiceV2");
            intent.setPackage(packageName);
            boolean res = app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (res) {
                LOG.info("Bound to OsmAnd service (package "+packageName+")");
                return true;
            } else {
                LOG.warn("Could not bind to OsmAnd service (package "+packageName+")");
                return false;
            }
        } else {
            return true;
        }
    }

    public void cleanupResources() {
        if (mIOsmAndAidlInterface != null) {
            app.unbindService(mConnection);
        }
    }

    public long registerForNavigationUpdates(boolean subscribeToUpdates, long callbackId) {
        if (mIOsmAndAidlInterface != null) {
            try {
                ANavigationUpdateParams params = new ANavigationUpdateParams();
                params.setCallbackId(callbackId);
                params.setSubscribeToUpdates(subscribeToUpdates);

                return mIOsmAndAidlInterface.registerForNavigationUpdates(params, mIOsmAndAidlCallback);
            } catch (RemoteException e) {
                LOG.error("could not subscribe to navigation updates", e);
            }
        }
        return -1L;
    }

    private boolean shouldSendNavigation() {
        Prefs prefs = GBApplication.getPrefs();

        boolean navigationForward = prefs.getBoolean("navigation_forward", true);
        boolean navigationOsmAnd = prefs.getBoolean("navigation_app_osmand", true);
        if (!navigationForward || !navigationOsmAnd) {
            return false;
        }

        boolean navigationScreenOn = prefs.getBoolean("nagivation_screen_on", true);
        if (!navigationScreenOn) {
            PowerManager powermanager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
            if (powermanager != null && powermanager.isScreenOn()) {
                LOG.info("Not forwarding navigation instructions, screen seems to be on and settings do not allow this");
                return false;
            }
        }

        return true;
    }

    /**
     * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
     *
     * @param subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
     * @param callbackId         (long) - id of callback, needed to unsubscribe from messages
     */
    public long registerForVoiceRouterMessages(boolean subscribeToUpdates, long callbackId) {
        ANavigationVoiceRouterMessageParams params = new ANavigationVoiceRouterMessageParams();
        params.setCallbackId(callbackId);
        params.setSubscribeToUpdates(subscribeToUpdates);
        if (mIOsmAndAidlInterface != null) {
            try {
                return mIOsmAndAidlInterface.registerForVoiceRouterMessages(params, mIOsmAndAidlCallback);
            } catch (RemoteException e) {
                LOG.error("could not register for voice router messages", e);
            }
        }
        return -1L;
    }

    public List<CharSequence> findInstalledOsmandPackages() {
        List<CharSequence> installedPackages = new ArrayList<>();
        for (String knownPackage : app.getBaseContext().getResources().getStringArray(R.array.osmand_package_names)) {
            if (isPackageInstalled(knownPackage)) {
                installedPackages.add(knownPackage);
            }
        }
        return installedPackages;
    }

    private boolean isPackageInstalled(final String packageName) {
        try {
            return app.getBaseContext().getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}