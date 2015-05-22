package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSendBytes;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public abstract class AbstractBTDeviceSupport extends AbstractDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);

    private GBDeviceProtocol gbDeviceProtocol;
    private GBDeviceIoThread gbDeviceIOThread;

    protected abstract GBDeviceProtocol createDeviceProtocol();

    protected abstract GBDeviceIoThread createDeviceIOThread();

    @Override
    public void dispose() {
        // currently only one thread allowed
        if (gbDeviceIOThread != null) {
            gbDeviceIOThread.quit();
            try {
                gbDeviceIOThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gbDeviceIOThread = null;
        }
    }

    @Override
    public void pair() {
        // Default implementation does no manual pairing, use the Android
        // pairing dialog instead.
    }

    public synchronized GBDeviceProtocol getDeviceProtocol() {
        if (gbDeviceProtocol == null) {
            gbDeviceProtocol = createDeviceProtocol();
        }
        return gbDeviceProtocol;
    }

    public synchronized GBDeviceIoThread getDeviceIOThread() {
        if (gbDeviceIOThread == null) {
            gbDeviceIOThread = createDeviceIOThread();
        }
        return gbDeviceIOThread;
    }

    protected void sendToDevice(byte[] bytes) {
        if (bytes != null && gbDeviceIOThread != null) {
            gbDeviceIOThread.write(bytes);
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

    public void handleGBDeviceCommand(GBDeviceCommandSendBytes sendBytes) {
        sendToDevice(sendBytes.encodedBytes);
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
            case SEND_BYTES:
                handleGBDeviceCommand((GBDeviceCommandSendBytes) deviceCmd);
                break;
            default:
                break;
        }
    }

    @Override
    public void onSMS(String from, String body) {
        byte[] bytes = gbDeviceProtocol.encodeSMS(from, body);
        sendToDevice(bytes);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        byte[] bytes = gbDeviceProtocol.encodeEmail(from, subject, body);
        sendToDevice(bytes);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        byte[] bytes = gbDeviceProtocol.encodeGenericNotification(title, details);
        sendToDevice(bytes);
    }

    @Override
    public void onSetTime(long ts) {
        byte[] bytes = gbDeviceProtocol.encodeSetTime(ts);
        sendToDevice(bytes);
    }

    @Override
    public void onSetCallState(String number, String name, GBCommand command) {
        byte[] bytes = gbDeviceProtocol.encodeSetCallState(number, name, command);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        byte[] bytes = gbDeviceProtocol.encodeSetMusicInfo(artist, album, track);
        sendToDevice(bytes);
    }

    @Override
    public void onFirmwareVersionReq() {
        byte[] bytes = gbDeviceProtocol.encodeFirmwareVersionReq();
        sendToDevice(bytes);
    }

    @Override
    public void onBatteryInfoReq() {
        byte[] bytes = gbDeviceProtocol.encodeBatteryInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppInfoReq() {
        byte[] bytes = gbDeviceProtocol.encodeAppInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppStart(UUID uuid) {
        byte[] bytes = gbDeviceProtocol.encodeAppStart(uuid);
        sendToDevice(bytes);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        byte[] bytes = gbDeviceProtocol.encodeAppDelete(uuid);
        sendToDevice(bytes);
    }

    @Override
    public void onPhoneVersion(byte os) {
        byte[] bytes = gbDeviceProtocol.encodePhoneVersion(os);
        sendToDevice(bytes);
    }

    @Override
    public void onReboot() {
        byte[] bytes = gbDeviceProtocol.encodeReboot();
        sendToDevice(bytes);
    }
}
