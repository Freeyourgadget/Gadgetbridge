package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.widget.Toast;

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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class OsmandEventReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(OsmandEventReceiver.class);

    private static final String OSMAND_PLUS_PACKAGE_NAME = "net.osmand.plus";
    private static final String OSMAND_PACKAGE_NAME = OSMAND_PLUS_PACKAGE_NAME;

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
            Intent intent = new Intent("net.osmand.aidl.OsmandAidlServiceV2");
            intent.setPackage(OSMAND_PACKAGE_NAME);
            boolean res = app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (res) {
                LOG.info("Bound to OsmAnd service");
                return true;
            } else {
                LOG.warn("Could not bind to OsmAnd service");
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
                LOG.error("could not subscribe to navication updates", e);
            }
        }
        return -1L;
    }

    private boolean shouldSendNavigation() {
        final Prefs prefs = GBApplication.getPrefs();

        final boolean navigationForward = prefs.getBoolean("navigation_forward", true);
        if (!navigationForward) {
            return false;
        }

        final boolean navigationScreenOn = prefs.getBoolean("nagivation_screen_on", true);
        if (!navigationScreenOn) {
            final PowerManager powermanager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
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
                    e.printStackTrace();
                }
            }
            return -1L;
        }
}