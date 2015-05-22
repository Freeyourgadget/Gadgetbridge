package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSendBytes;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandVersionInfo;

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

    public void evaluateGBDeviceCommand(GBDeviceCommand deviceCmd) {

        switch (deviceCmd.commandClass) {
            case MUSIC_CONTROL:
                handleGBDeviceCommand((GBDeviceCommandMusicControl) deviceCmd);
                break;
            case CALL_CONTROL:
                handleGBDeviceCommand((GBDeviceCommandCallControl) deviceCmd);
                break;
            case VERSION_INFO:
                handleGBDeviceCommand((GBDeviceCommandVersionInfo) deviceCmd);
                break;
            case APP_INFO:
                handleGBDeviceCommand((GBDeviceCommandAppInfo) deviceCmd);
                break;
            case SLEEP_MONITOR_RES:
                handleGBDeviceCommand((GBDeviceCommandSleepMonitorResult) deviceCmd);
                break;
            default:
                break;
        }
    }

    public void handleGBDeviceCommand(GBDeviceCommandMusicControl musicCmd) {
        Context context = getContext();
        LOG.info("Got command for MUSIC_CONTROL");
        Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
        musicIntent.putExtra("command", musicCmd.command.ordinal());
        musicIntent.setPackage(context.getPackageName());
        context.sendBroadcast(musicIntent);
    }

    public void handleGBDeviceCommand(GBDeviceCommandCallControl callCmd) {
        Context context = getContext();
        LOG.info("Got command for CALL_CONTROL");
        Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
        callIntent.putExtra("command", callCmd.command.ordinal());
        callIntent.setPackage(context.getPackageName());
        context.sendBroadcast(callIntent);
    }

    public void handleGBDeviceCommand(GBDeviceCommandVersionInfo infoCmd) {
        Context context = getContext();
        LOG.info("Got command for VERSION_INFO");
        if (gbDevice == null) {
            return;
        }
        gbDevice.setFirmwareVersion(infoCmd.fwVersion);
        gbDevice.setHardwareVersion(infoCmd.hwVersion);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    public void handleGBDeviceCommand(GBDeviceCommandAppInfo appInfoCmd) {
        Context context = getContext();
        LOG.info("Got command for APP_INFO");

        Intent appInfoIntent = new Intent(AppManagerActivity.ACTION_REFRESH_APPLIST);
        int appCount = appInfoCmd.apps.length;
        appInfoIntent.putExtra("app_count", appCount);
        for (Integer i = 0; i < appCount; i++) {
            appInfoIntent.putExtra("app_name" + i.toString(), appInfoCmd.apps[i].getName());
            appInfoIntent.putExtra("app_creator" + i.toString(), appInfoCmd.apps[i].getCreator());
            appInfoIntent.putExtra("app_uuid" + i.toString(), appInfoCmd.apps[i].getUUID().toString());
            appInfoIntent.putExtra("app_type" + i.toString(), appInfoCmd.apps[i].getType().ordinal());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
    }

    public void handleGBDeviceCommand(GBDeviceCommandSleepMonitorResult sleepMonitorResult) {
        Context context = getContext();
        LOG.info("Got command for SLEEP_MONIOR_RES");
        Intent sleepMontiorIntent = new Intent(SleepMonitorActivity.ACTION_REFRESH);
        sleepMontiorIntent.putExtra("smartalarm_from", sleepMonitorResult.smartalarm_from);
        sleepMontiorIntent.putExtra("smartalarm_to", sleepMonitorResult.smartalarm_to);
        sleepMontiorIntent.putExtra("recording_base_timestamp", sleepMonitorResult.recording_base_timestamp);
        sleepMontiorIntent.putExtra("alarm_gone_off", sleepMonitorResult.alarm_gone_off);
        sleepMontiorIntent.putExtra("points", sleepMonitorResult.points);

        LocalBroadcastManager.getInstance(context).sendBroadcast(sleepMontiorIntent);
    }
}
