package nodomain.freeyourgadget.gadgetbridge.service.devices.id115;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class ID115Support extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ID115Support.class);

    public BluetoothGattCharacteristic normalWriteCharacteristic = null;
    public BluetoothGattCharacteristic normalNotifyCharacteristic = null;
    public BluetoothGattCharacteristic healthWriteCharacteristic = null;

    byte[] currentNotificationBuffer;
    int currentNotificationSize;
    int currentNotificationIndex;
    byte currentNotificationType;

    public ID115Support() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(ID115Constants.UUID_SERVICE_ID115);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        normalWriteCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_NORMAL);
        normalNotifyCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_NORMAL);
        healthWriteCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_HEALTH);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        builder.setGattCallback(this);
        builder.notify(normalNotifyCharacteristic, true);

        setTime(builder)
                .setScreenOrientation(builder)
                .setGoal(builder)
                .setInitialized(builder);

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        sendMessageNotification(notificationSpec);
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("time");
            setTime(builder);
            performConnected(builder.getTransaction());
        } catch(IOException e) {
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            sendCallNotification(callSpec);
        } else {
            sendStopCallNotification();
        }
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
    public void onReboot() {
        try {
            getQueue().clear();

            TransactionBuilder builder = performInitialized("reboot");
            builder.write(normalWriteCharacteristic, new byte[] {
                    ID115Constants.CMD_ID_DEVICE_RESTART, ID115Constants.CMD_KEY_REBOOT
            });
            performConnected(builder.getTransaction());
        } catch(Exception e) {
        }
    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {

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
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (!characteristicUUID.equals(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_NORMAL)) {
            return false;
        }

        if (data.length < 2) {
            LOG.warn("short GATT response");
            return false;
        }
        if (data[0] == ID115Constants.CMD_ID_NOTIFY) {
            if (data.length < 4) {
                LOG.warn("short GATT response for NOTIFY");
                return false;
            }
            if (data[1] == currentNotificationType) {
                if (data[3] == currentNotificationIndex) {
                    if (currentNotificationIndex != currentNotificationSize) {
                        sendNotificationChunk(currentNotificationIndex + 1);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    ID115Support setTime(TransactionBuilder builder) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());

        int day = c.get(Calendar.DAY_OF_WEEK);

        byte dayOfWeek;
        if (day == Calendar.SUNDAY) {
            dayOfWeek = 6;
        } else {
            dayOfWeek = (byte)(day - 2);
        }

        int year = c.get(Calendar.YEAR);
        builder.write(normalWriteCharacteristic, new byte[] {
                ID115Constants.CMD_ID_SETTINGS, ID115Constants.CMD_KEY_SET_TIME,
                (byte)(year & 0xff),
                (byte)(year >> 8),
                (byte)(1 + c.get(Calendar.MONTH)),
                (byte)c.get(Calendar.DAY_OF_MONTH),
                (byte)c.get(Calendar.HOUR_OF_DAY),
                (byte)c.get(Calendar.MINUTE),
                (byte)c.get(Calendar.SECOND),
                dayOfWeek
        });
        return this;
    }

    ID115Support setScreenOrientation(TransactionBuilder builder) {
        String value = GBApplication.getPrefs().getString(ID115Constants.PREF_SCREEN_ORIENTATION,
                "horizontal");
        LOG.warn("value: '" + value + "'");

        byte orientation;
        if (value.equals("horizontal")) {
            orientation = ID115Constants.CMD_ARG_HORIZONTAL;
        } else {
            orientation = ID115Constants.CMD_ARG_VERTICAL;
        }

        LOG.warn("Screen orientation: " + orientation);
        builder.write(normalWriteCharacteristic, new byte[] {
                ID115Constants.CMD_ID_SETTINGS, ID115Constants.CMD_KEY_SET_DISPLAY_MODE,
                orientation
        });
        return this;
    }

    private ID115Support setGoal(TransactionBuilder transaction) {
        ActivityUser activityUser = new ActivityUser();
        int value = activityUser.getStepsGoal();

        transaction.write(normalWriteCharacteristic, new byte[]{
                ID115Constants.CMD_ID_SETTINGS,
                ID115Constants.CMD_KEY_SET_GOAL,
                0,
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
                0, 0
        });
        return this;
    }

    void sendCallNotification(CallSpec callSpec) {
        String number = "";
        if (callSpec.number != null) {
            number = callSpec.number;
        }

        String name = "";
        if (callSpec.name != null) {
            name = callSpec.name;
        }

        currentNotificationBuffer = encodeCallNotification(name, number);
        currentNotificationSize = (currentNotificationBuffer.length + 15) / 16;
        currentNotificationType = ID115Constants.CMD_KEY_NOTIFY_CALL;
        sendNotificationChunk(1);
    }

    void sendStopCallNotification() {
        try {
            TransactionBuilder builder = performInitialized("stop_call_notification");
            builder.write(normalWriteCharacteristic, new byte[] {
                    ID115Constants.CMD_ID_NOTIFY,
                    ID115Constants.CMD_KEY_NOTIFY_STOP,
                    1
            });
            performConnected(builder.getTransaction());
        } catch(IOException e) {
        }
    }

    void sendMessageNotification(NotificationSpec notificationSpec) {
        String phone = "";
        if (notificationSpec.phoneNumber != null) {
            phone = notificationSpec.phoneNumber;
        }

        String title = "";
        if (notificationSpec.sender != null) {
            title = notificationSpec.sender;
        } else if (notificationSpec.title != null) {
            title = notificationSpec.title;
        } else if (notificationSpec.subject != null) {
            title = notificationSpec.subject;
        }

        String text = "";
        if (notificationSpec.body != null) {
            text = notificationSpec.body;
        }

        currentNotificationBuffer = encodeMessageNotification(notificationSpec.type, title, phone, text);
        currentNotificationSize = (currentNotificationBuffer.length + 15) / 16;
        currentNotificationType = ID115Constants.CMD_KEY_NOTIFY_MSG;
        sendNotificationChunk(1);
    }

    void sendNotificationChunk(int chunkIndex) {
        currentNotificationIndex = chunkIndex;

        int offset = (chunkIndex - 1) * 16;
        int tailSize = currentNotificationBuffer.length - offset;
        int chunkSize = (tailSize > 16)? 16 : tailSize;

        byte raw[] = new byte[16];
        System.arraycopy(currentNotificationBuffer, offset, raw, 0, chunkSize);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(ID115Constants.CMD_ID_NOTIFY);
            outputStream.write(currentNotificationType);
            outputStream.write((byte)currentNotificationSize);
            outputStream.write((byte)currentNotificationIndex);
            outputStream.write(raw);
            byte cmd[] = outputStream.toByteArray();

            TransactionBuilder builder = performInitialized("notification");
            builder.write(normalWriteCharacteristic, cmd);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
        }
    }

    byte[] encodeCallNotification(String name, String phone) {
        if (name.length() > 20) {
            name = name.substring(0, 20);
        }
        if (phone.length() > 20) {
            phone = phone.substring(0, 20);
        }

        byte[] name_bytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] phone_bytes = phone.getBytes(StandardCharsets.UTF_8);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write((byte) phone_bytes.length);
            outputStream.write((byte) name_bytes.length);
            outputStream.write(phone_bytes);
            outputStream.write(name_bytes);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    byte[] encodeMessageNotification(NotificationType type, String title, String phone, String text) {
        if (title.length() > 20) {
            title = title.substring(0, 20);
        }
        if (phone.length() > 20) {
            phone = phone.substring(0, 20);
        }
        if (text.length() > 20) {
            text = text.substring(0, 20);
        }
        byte[] title_bytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] phone_bytes = phone.getBytes(StandardCharsets.UTF_8);
        byte[] text_bytes = text.getBytes(StandardCharsets.UTF_8);

        byte nativeType = ID115Constants.getNotificationType(type);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(nativeType);
            outputStream.write((byte) text_bytes.length);
            outputStream.write((byte) phone_bytes.length);
            outputStream.write((byte) title_bytes.length);
            outputStream.write(phone_bytes);
            outputStream.write(title_bytes);
            outputStream.write(text_bytes);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
