package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.activities.SleepChartActivity;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;

// TODO: support option for a single reminder notification when notifications could not be delivered?
// conditions: app was running and received notifications, but device was not connected.
// maybe need to check for "unread notifications" on device for that.
public abstract class AbstractDeviceSupport implements DeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);

    protected GBDevice gbDevice;
    private BluetoothAdapter btAdapter;
    private Context context;

    public void initialize(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        this.gbDevice = gbDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    @Override
    public boolean isConnected() {
        return gbDevice.isConnected();
    }

    protected boolean isInitialized() {
        return gbDevice.isInitialized();
    }

    @Override
    public GBDevice getDevice() {
        return gbDevice;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {

        switch (deviceEvent.eventClass) {
            case MUSIC_CONTROL:
                handleGBDeviceEvent((GBDeviceEventMusicControl) deviceEvent);
                break;
            case CALL_CONTROL:
                handleGBDeviceEvent((GBDeviceEventCallControl) deviceEvent);
                break;
            case VERSION_INFO:
                handleGBDeviceEvent((GBDeviceEventVersionInfo) deviceEvent);
                break;
            case APP_INFO:
                handleGBDeviceEvent((GBDeviceEventAppInfo) deviceEvent);
                break;
            case SLEEP_MONITOR_RES:
                handleGBDeviceEvent((GBDeviceEventSleepMonitorResult) deviceEvent);
                break;
            default:
                break;
        }
    }

    public void handleGBDeviceEvent(GBDeviceEventMusicControl musicEvent) {
        Context context = getContext();
        LOG.info("Got event for MUSIC_CONTROL");
        Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
        musicIntent.putExtra("event", musicEvent.event.ordinal());
        musicIntent.setPackage(context.getPackageName());
        context.sendBroadcast(musicIntent);
    }

    public void handleGBDeviceEvent(GBDeviceEventCallControl callEvent) {
        Context context = getContext();
        LOG.info("Got event for CALL_CONTROL");
        Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
        callIntent.putExtra("event", callEvent.event.ordinal());
        callIntent.setPackage(context.getPackageName());
        context.sendBroadcast(callIntent);
    }

    public void handleGBDeviceEvent(GBDeviceEventVersionInfo infoEvent) {
        Context context = getContext();
        LOG.info("Got event for VERSION_INFO");
        if (gbDevice == null) {
            return;
        }
        gbDevice.setFirmwareVersion(infoEvent.fwVersion);
        gbDevice.setHardwareVersion(infoEvent.hwVersion);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    public void handleGBDeviceEvent(GBDeviceEventAppInfo appInfoEvent) {
        Context context = getContext();
        LOG.info("Got event for APP_INFO");

        Intent appInfoIntent = new Intent(AppManagerActivity.ACTION_REFRESH_APPLIST);
        int appCount = appInfoEvent.apps.length;
        appInfoIntent.putExtra("app_count", appCount);
        for (Integer i = 0; i < appCount; i++) {
            appInfoIntent.putExtra("app_name" + i.toString(), appInfoEvent.apps[i].getName());
            appInfoIntent.putExtra("app_creator" + i.toString(), appInfoEvent.apps[i].getCreator());
            appInfoIntent.putExtra("app_uuid" + i.toString(), appInfoEvent.apps[i].getUUID().toString());
            appInfoIntent.putExtra("app_type" + i.toString(), appInfoEvent.apps[i].getType().ordinal());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
    }

    public void handleGBDeviceEvent(GBDeviceEventSleepMonitorResult sleepMonitorResult) {
        Context context = getContext();
        LOG.info("Got event for SLEEP_MONIOR_RES");
        Intent sleepMontiorIntent = new Intent(SleepChartActivity.ACTION_REFRESH);
        sleepMontiorIntent.putExtra("smartalarm_from", sleepMonitorResult.smartalarm_from);
        sleepMontiorIntent.putExtra("smartalarm_to", sleepMonitorResult.smartalarm_to);
        sleepMontiorIntent.putExtra("recording_base_timestamp", sleepMonitorResult.recording_base_timestamp);
        sleepMontiorIntent.putExtra("alarm_gone_off", sleepMonitorResult.alarm_gone_off);

        LocalBroadcastManager.getInstance(context).sendBroadcast(sleepMontiorIntent);
    }
}
