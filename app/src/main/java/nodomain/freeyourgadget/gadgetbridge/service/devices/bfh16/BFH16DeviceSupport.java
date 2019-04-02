package nodomain.freeyourgadget.gadgetbridge.service.devices.bfh16;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.bfh16.BFH16Constants;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class BFH16DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(BFH16DeviceSupport.class);

    public BFH16DeviceSupport() {
        super(LOG);
        addSupportedService(BFH16Constants.BFH16_MAIN_SERVICE);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String notificationTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        byte icon;
        switch (notificationSpec.type) {
//            case GENERIC_SMS:
//                icon = JYouConstants.ICON_SMS;
//                break;
//            case FACEBOOK:
//            case FACEBOOK_MESSENGER:
//                icon = JYouConstants.ICON_FACEBOOK;
//                break;
//            case TWITTER:
//                icon = JYouConstants.ICON_TWITTER;
//                break;
//            case WHATSAPP:
//                icon = JYouConstants.ICON_WHATSAPP;
//                break;
//            default:
//                icon = JYouConstants.ICON_LINE;
//                break;
        }
        //showNotification(icon, notificationTitle, notificationSpec.body);     //TODO FIXME
    }

    private void showNotification(byte icon, String title, String message) {
        //TODO implement
//        try {
//            TransactionBuilder builder = performInitialized("ShowNotification");
//
//            byte[] titleBytes = stringToUTF8Bytes(title, 16);
//            byte[] messageBytes = stringToUTF8Bytes(message, 80);
//
//            for (int i = 1; i <= 7; i++)
//            {
//                byte[] currentPacket = new byte[20];
//                currentPacket[0] = JYouConstants.CMD_ACTION_SHOW_NOTIFICATION;
//                currentPacket[1] = 7;
//                currentPacket[2] = (byte)i;
//                switch(i) {
//                    case 1:
//                        currentPacket[4] = icon;
//                        break;
//                    case 2:
//                        if (titleBytes != null) {
//                            System.arraycopy(titleBytes, 0, currentPacket, 3, 6);
//                            System.arraycopy(titleBytes, 6, currentPacket, 10, 10);
//                        }
//                        break;
//                    default:
//                        if (messageBytes != null) {
//                            System.arraycopy(messageBytes, 16 * (i - 3), currentPacket, 3, 6);
//                            System.arraycopy(messageBytes, 6 + 16 * (i - 3), currentPacket, 10, 10);
//                        }
//                        break;
//                }
//                builder.write(ctrlCharacteristic, currentPacket);
//            }
//            builder.queue(getQueue());
//        } catch (IOException e) {
//            LOG.warn(e.getMessage());
//        }
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
//        try {
//            TransactionBuilder builder = performInitialized("SetTime");
//            syncDateAndTime(builder);
//            builder.queue(getQueue());
//        } catch(IOException e) {
//            LOG.warn(e.getMessage());
//        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
//        try {
//            TransactionBuilder builder = performInitialized("SetAlarms");
//
//            for (int i = 0; i < alarms.size(); i++)
//            {
//                byte cmd;
//                switch (i) {
//                    case 0:
//                        cmd = JYouConstants.CMD_SET_ALARM_1;
//                        break;
//                    case 1:
//                        cmd = JYouConstants.CMD_SET_ALARM_2;
//                        break;
//                    case 2:
//                        cmd = JYouConstants.CMD_SET_ALARM_3;
//                        break;
//                    default:
//                        return;
//                }
//                Calendar cal = AlarmUtils.toCalendar(alarms.get(i));
//                builder.write(ctrlCharacteristic, commandWithChecksum(
//                        cmd,
//                        alarms.get(i).getEnabled() ? cal.get(Calendar.HOUR_OF_DAY) : -1,
//                        alarms.get(i).getEnabled() ? cal.get(Calendar.MINUTE) : -1
//                ));
//            }
//            builder.queue(getQueue());
//            GB.toast(getContext(), "Alarm settings applied - do note that the current device does not support day specification", Toast.LENGTH_LONG, GB.INFO);
//        } catch(IOException e) {
//            LOG.warn(e.getMessage());
//        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
//        switch (callSpec.command) {
//            case CallSpec.CALL_INCOMING:
//                showNotification(JYouConstants.ICON_CALL, callSpec.name, callSpec.number);
//                break;
//        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        //onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    @Override
    public void onReset(int flags) {
//        try {
//            TransactionBuilder builder = performInitialized("Reboot");
//            builder.write(ctrlCharacteristic, commandWithChecksum(
//                    JYouConstants.CMD_ACTION_REBOOT_DEVICE, 0, 0
//            ));
//            builder.queue(getQueue());
//        } catch(Exception e) {
//            LOG.warn(e.getMessage());
//        }
    }

    @Override
    public void onHeartRateTest() {
//        try {
//            TransactionBuilder builder = performInitialized("HeartRateTest");
//            builder.write(ctrlCharacteristic, commandWithChecksum(
//                    JYouConstants.CMD_ACTION_HEARTRATE_SWITCH, 0, 1
//            ));
//            builder.queue(getQueue());
//        } catch(Exception e) {
//            LOG.warn(e.getMessage());
//        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
//        // TODO: test
//        try {
//            TransactionBuilder builder = performInitialized("RealTimeHeartMeasurement");
//            builder.write(ctrlCharacteristic, commandWithChecksum(
//                    JYouConstants.CMD_SET_HEARTRATE_AUTO, 0, enable ? 1 : 0
//            ));
//            builder.queue(getQueue());
//        } catch(Exception e) {
//            LOG.warn(e.getMessage());
//        }
    }

    @Override
    public void onFindDevice(boolean start) {
//        if (start) {
//            showNotification(JYouConstants.ICON_QQ, "Gadgetbridge", "Bzzt! Bzzt!");
//            GB.toast(getContext(), "As your device doesn't have sound, it will only vibrate 3 times consecutively", Toast.LENGTH_LONG, GB.INFO);
//        }
    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

}
