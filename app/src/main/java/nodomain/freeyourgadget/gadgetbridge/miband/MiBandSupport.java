package nodomain.freeyourgadget.gadgetbridge.miband;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import java.text.DateFormat;
import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.GBCommand;
import nodomain.freeyourgadget.gadgetbridge.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.btle.TransactionBuilder;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_GENERIC;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_K9MAIL;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_SMS;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.getNotificationPrefIntValue;

public class MiBandSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiBandSupport.class);

    private ByteBuffer activityDataHolder = null;


    public MiBandSupport() {
        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        pair(builder)
                .sendUserInfo(builder)
                .enableNotifications(builder, true)
                .setCurrentTime(builder)
                .requestBatteryInfo(builder);

        return builder;
    }

    // TODO: tear down the notifications on quit
    private MiBandSupport enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_NOTIFICATION), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_BATTERY), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA), enable);

        return this;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void pair() {
        for (int i = 0; i < 5; i++) {
            if (connect()) {
                return;
            }
        }
    }

    private byte[] getDefaultNotification() {
        final int vibrateTimes = 1;
        final long vibrateDuration = 250l;
        final int flashTimes = 1;
        final int flashColour = 0xFFFFFFFF;
        final int originalColour = 0xFFFFFFFF;
        final long flashDuration = 250l;

        return getNotification(vibrateDuration, vibrateTimes, flashTimes, flashColour, originalColour, flashDuration);
    }

    private void sendDefaultNotification(TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        LOG.info("Sending notification to MiBand: " + characteristic);
        builder.write(characteristic, getDefaultNotification()).queue(getQueue());
    }

    private void sendCustomNotification(int vibrateDuration, int vibrateTimes, int pause, int flashTimes, int flashColour, int originalColour, long flashDuration, TransactionBuilder builder) {
        BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        int vDuration = Math.min(500, vibrateDuration); // longer than 500ms is not possible
        for (int i = 0; i < vibrateTimes; i++) {
            builder.write(controlPoint, startVibrate);
            builder.wait(vDuration);
            builder.write(controlPoint, stopVibrate);
            if (pause > 0) {
                builder.wait(pause);
            }
        }

        LOG.info("Sending notification to MiBand: " + controlPoint);
        builder.queue(getQueue());
    }

    private static final byte[] startVibrate = new byte[]{MiBandService.COMMAND_SEND_NOTIFICATION, 1};
    private static final byte[] stopVibrate = new byte[]{MiBandService.COMMAND_STOP_MOTOR_VIBRATE};
    private static final byte[] reboot = new byte[]{MiBandService.COMMAND_REBOOT};
    private static final byte[] fetch = new byte[]{6};

    private byte[] getNotification(long vibrateDuration, int vibrateTimes, int flashTimes, int flashColour, int originalColour, long flashDuration) {
        byte[] vibrate = new byte[]{MiBandService.COMMAND_SEND_NOTIFICATION, (byte) 1};
        byte r = 6;
        byte g = 0;
        byte b = 6;
        boolean display = true;
        //      byte[] flashColor = new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 };
        return vibrate;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private MiBandSupport sendUserInfo(TransactionBuilder builder) {
        LOG.debug("Writing User Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_USER_INFO);
        builder.write(characteristic, MiBandCoordinator.getAnyUserInfo(getDevice().getAddress()).getData());
        return this;
    }

    private MiBandSupport requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_BATTERY);
        builder.read(characteristic);
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param transaction
     * @return
     */
    private MiBandSupport pair(TransactionBuilder transaction) {
        LOG.info("Attempting to pair MI device...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_PAIR);
        if (characteristic != null) {
            transaction.write(characteristic, new byte[]{2});
        } else {
            LOG.info("Unable to pair MI device -- characteristic not available");
        }
        return this;
    }

    private void performDefaultNotification(String task) {
        try {
            TransactionBuilder builder = performInitialized(task);
            sendDefaultNotification(builder);
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

//    private void performCustomNotification(String task, int vibrateDuration, int vibrateTimes, int pause, int flashTimes, int flashColour, int originalColour, long flashDuration) {
//        try {
//            TransactionBuilder builder = performInitialized(task);
//            sendCustomNotification(vibrateDuration, vibrateTimes, pause, flashTimes, flashColour, originalColour, flashDuration, builder);
//        } catch (IOException ex) {
//            LOG.error("Unable to send notification to MI device", ex);
//        }
//    }

    private void performPreferredNotification(String task, String notificationOrigin) {
        try {
            TransactionBuilder builder = performInitialized(task);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int vibrateDuration = getPreferredVibrateDuration(notificationOrigin, prefs);
            int vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            int vibratePause = getPreferredVibratePause(notificationOrigin, prefs);

            int flashTimes = getPreferredFlashCount(notificationOrigin, prefs);
            int flashColour = getPreferredFlashColour(notificationOrigin, prefs);
            int originalColour = getPreferredOriginalColour(notificationOrigin, prefs);
            int flashDuration = getPreferredFlashDuration(notificationOrigin, prefs);

            sendCustomNotification(vibrateDuration, vibrateTimes, vibratePause, flashTimes, flashColour, originalColour, flashDuration, builder);
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

    private int getPreferredFlashDuration(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_DURATION, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_DURATION);
    }

    private int getPreferredOriginalColour(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_ORIGINAL_COLOUR, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR);
    }

    private int getPreferredFlashColour(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_COLOUR, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_COLOUR);
    }

    private int getPreferredFlashCount(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_COUNT);
    }

    private int getPreferredVibratePause(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(VIBRATION_PAUSE, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_PAUSE);
    }

    private int getPreferredVibrateCount(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(VIBRATION_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_COUNT);
    }

    private int getPreferredVibrateDuration(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(VIBRATION_DURATION, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_DURATION);
    }

    @Override
    public void onSMS(String from, String body) {
//        performCustomNotification("sms received", 500, 3, 2000, 0, 0, 0, 0);
        performPreferredNotification("sms received", ORIGIN_SMS);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        performPreferredNotification("email received", ORIGIN_K9MAIL);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        performPreferredNotification("generic notification received", ORIGIN_GENERIC);
    }

    @Override
    public void onSetTime(long ts) {
        try {
            TransactionBuilder builder = performInitialized("Set date and time");
            setCurrentTime(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set time on MI device", ex);
        }
    }

    /**
     * Sets the current time to the Mi device using the given builder.
     *
     * @param builder
     */
    private MiBandSupport setCurrentTime(TransactionBuilder builder) {
        Calendar now = Calendar.getInstance();
        byte[] time = new byte[]{
                (byte) (now.get(Calendar.YEAR) - 2000),
                (byte) now.get(Calendar.MONTH),
                (byte) now.get(Calendar.DATE),
                (byte) now.get(Calendar.HOUR_OF_DAY),
                (byte) now.get(Calendar.MINUTE),
                (byte) now.get(Calendar.SECOND),
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f
        };
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DATE_TIME);
        if (characteristic != null) {
            builder.write(characteristic, time);
        } else {
            LOG.info("Unable to set time -- characteristic not available");
        }
        return this;
    }

    @Override
    public void onSetCallState(String number, String name, GBCommand command) {
        if (GBCommand.CALL_INCOMING.equals(command)) {
            performDefaultNotification("incoming call");
        }
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        // not supported
    }

    @Override
    public void onFirmwareVersionReq() {
        try {
            TransactionBuilder builder = performInitialized("Get MI Band device info");
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO);
            builder.read(characteristic).queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read device info from MI", ex);
        }
    }

    @Override
    public void onBatteryInfoReq() {
        try {
            TransactionBuilder builder = performInitialized("Get MI Band battery info");
            requestBatteryInfo(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read battery info from MI", ex);
        }
    }

    @Override
    public void onReboot() {
        try {
            TransactionBuilder builder = performInitialized("fetch activity data");
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), fetch);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to fetch MI activity data", ex);
        }
    }

    @Override
    public void onAppInfoReq() {
        // not supported
    }

    @Override
    public void onAppStart(UUID uuid) {
        // not supported
    }

    @Override
    public void onAppDelete(UUID uuid) {
        // not supported
    }

    @Override
    public void onPhoneVersion(byte os) {
        // not supported
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityNotif(characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO.equals(characteristicUUID)) {
            handleDeviceInfo(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_PAIR.equals(characteristicUUID)) {
            handlePairResult(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_USER_INFO.equals(characteristicUUID)) {
            handleUserInfoResult(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT.equals(characteristicUUID)) {
            handleControlPointResult(characteristic.getValue(), status);
        }
    }

    private void handleDeviceInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            DeviceInfo info = new DeviceInfo(value);
            getDevice().setFirmwareVersion(info.getFirmwareVersion());
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    private void handleActivityNotif(byte[] value) {
       LOG.info("NOTIF GOT " + value.length + " BYTES.");
       if (this.activityDataHolder == null && value.length == 11 ) {
          //I know what to do:
          // byte 0 is the data type
          int dataType = value[0];
          // byte 1 to 6 represent a timestamp
          GregorianCalendar timestamp = new GregorianCalendar(value[1]+2000,
                        value[2],
                        value[3],
                        value[4],
                        value[5],
                        value[6]);
          // no idea about the following two bytes
          int i = value[7] & 0xff;
          int j = value[8] & 0xff;
          // but combined they tell us how to proceed:
          int k = i | (j << 8);
          if (dataType == 1) {
              k *= 3;
          }
          int totalDataToRead = k;

          // no idea about the following two bytes
          int i1 = value[9] & 0xff ;
          int j1 = value[10] & 0xff;

          // but combined they tell us how to proceed:
          int k1 = i1 | (j1 << 8);
          if (dataType == 1) {
              k1 *= 3;
          }
          int dataUntilNextHeader = k1;

          // there is a total of totalDataToRead that will come in (3 bytes per minute),
          // however, after dataUntilNextHeader bytes we will get a new packet of 11 bytes that should be parsed
          // as we just did
                
          LOG.info("total data to read: "+  totalDataToRead   +" len: " + (totalDataToRead / 3) + " minute(s)");
          LOG.info("data to read until next header: "+  dataUntilNextHeader   +" len: " + (dataUntilNextHeader / 3) + " minute(s)");
          LOG.info("RAW DATA: i="+ i +" j="+ j +" k="+ k +" i1="+ i1 +" j1="+ j1 +" k1="+ k1 +"");
          LOG.info("TIMESTAMP: " + DateFormat.getDateTimeInstance().format(timestamp.getTime()).toString() + " magic byte: " + dataUntilNextHeader);
          if (createActivityDataHolder(dataUntilNextHeader)) {
              sendAckDataTransfer(timestamp, dataUntilNextHeader);
          }
      } else {
          for (byte b: value){
              this.activityDataHolder.put(b);
          }
          LOG.info("Buffer remaining bytes: " + this.activityDataHolder.remaining());
          if (this.activityDataHolder.remaining() == 0) {
              consumeActivityDataHolder();
          }
      }
    }

    private boolean createActivityDataHolder(int size) {
 	if(this.activityDataHolder == null) {
            this.activityDataHolder = ByteBuffer.allocate(size);
            return true;
	}
        return false;
    }

    private void consumeActivityDataHolder() {
        int currentSize = this.activityDataHolder.capacity();
        this.activityDataHolder.rewind();
	/*
		intensity = byte1;
		steps = byte2;
		category = byte0;
	*/
        for ( int i = 0 ; i < currentSize; i+=3 ) {
            LOG.info("index: "+i/3+" category:"+this.activityDataHolder.get()+" intensity:"+this.activityDataHolder.get()+" steps:"+this.activityDataHolder.get());	
        }
        this.activityDataHolder = null;
    }

    private void handleControlPointResult(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            for (byte b: value){
                LOG.info("3GOT DATA:" + String.format("0x%20x", b));
            }
        } else {
            LOG.info("BOOOOOOOOO");
        }

    }
    private void sendAckDataTransfer(Calendar time, int unknown_code) {
        byte[] ack = new byte[]{
                MiBandService.COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE,
                (byte) (time.get(Calendar.YEAR) - 2000),
                (byte) time.get(Calendar.MONTH),
                (byte) time.get(Calendar.DATE),
                (byte) time.get(Calendar.HOUR_OF_DAY),
                (byte) time.get(Calendar.MINUTE),
                (byte) time.get(Calendar.SECOND),
                (byte) (unknown_code & 0xff),
                (byte) (0xff & (unknown_code >> 8))
        };
        LOG.info("WRITING: " + DateFormat.getDateTimeInstance().format(time.getTime()).toString());
        try {
            TransactionBuilder builder = performInitialized("send acknowledge");
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), ack);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send ack to MI", ex);
        }
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BatteryInfo info = new BatteryInfo(value);
            getDevice().setBatteryLevel((short) info.getLevelInPercent());
            getDevice().setBatteryState(info.getStatus());
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    private void handleUserInfoResult(byte[] value, int status) {
        // successfully transfered user info means we're initialized
        if (status == BluetoothGatt.GATT_SUCCESS) {
            setConnectionState(State.INITIALIZED);
        }
    }

    private void setConnectionState(State newState) {
        getDevice().setState(newState);
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    private void handlePairResult(byte[] pairResult, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.info("Pairing MI device failed: " + status);
            return;
        }

        String value = null;
        if (pairResult != null) {
            if (pairResult.length == 1) {
                try {
                    if (pairResult[0] == 2) {
                        LOG.info("Successfully paired  MI device");
                        return;
                    }
                } catch (Exception ex) {
                    LOG.warn("Error identifying pairing result", ex);
                    return;
                }
            }
            value = Arrays.toString(pairResult);
        }
        LOG.info("MI Band pairing result: " + value);
    }

    @Override
    protected TransactionBuilder createTransactionBuilder(String taskName) {
        return new MiBandTransactionBuilder(taskName);
    }
}
