package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
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

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class OsmandEventReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(OsmandEventReceiver.class);

    private static final String OSMAND_PLUS_PACKAGE_NAME = "net.osmand.plus";
    private static final String OSMAND_PACKAGE_NAME = OSMAND_PLUS_PACKAGE_NAME;

    private final Application app;
    private IOsmAndAidlInterface mIOsmAndAidlInterface;

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
            LOG.error("Distance: " + directionInfo.getDistanceTo() + " turnType: " + directionInfo.getTurnType());
        }

        @Override
        public void onContextMenuButtonClicked(int buttonId, String pointId, String layerId) {
        }

        @Override
        public void onVoiceRouterNotify(OnVoiceNavigationParams params) {
            List<String>  played = params.getPlayed();
            for (String play : played) {
                LOG.error("played: " + play);
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
            GB.toast("OsmAnd service conncted", Toast.LENGTH_SHORT, GB.INFO);
            registerForNavigationUpdates(true, 6666); // what is this id for?
            registerForVoiceRouterMessages(true,6667);
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("osmand.api://GET_INFO"));
//            OsmandEventReceiver.this.startActivityForResult(intent,666);
        }

        public void onServiceDisconnected(ComponentName className) {
            mIOsmAndAidlInterface = null;
            GB.toast("OsmAnd service disconnected", Toast.LENGTH_SHORT, GB.INFO);
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
                GB.toast("bound to OsmAnd service", Toast.LENGTH_SHORT, GB.INFO);
                return true;
            } else {
                GB.toast("could not bind to OsmAnd service", Toast.LENGTH_SHORT, GB.INFO);
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

        /**
         * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
         *
         * @param subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
         * @param callbackId         (long) - id of callback, needed to unsubscribe from messages
         * @param callback           (IOsmAndAidlCallback) - callback to notify user on voice message
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