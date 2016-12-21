package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ConditionalWriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;


public class HPlusSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(HPlusSupport.class);

    private BluetoothGattCharacteristic ctrlCharacteristic = null;
    private BluetoothGattCharacteristic measureCharacteristic = null;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            if (s.equals(DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    };

    public HPlusSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(HPlusConstants.UUID_SERVICE_HP);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter intentFilter = new IntentFilter();

        broadcastManager.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void dispose() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.unregisterReceiver(mReceiver);
        super.dispose();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {

        measureCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_CONTROL);


        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        //Fill device info
        requestDeviceInfo(builder);

        getDevice().setFirmwareVersion("0");
        getDevice().setFirmwareVersion2("0");

        //Initialize device
        setInitValues(builder);

        builder.notify(getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_MEASURE), true);

        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = measureCharacteristic.getDescriptor(uuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        setInitialized(builder);
        return builder;
    }

    private HPlusSupport setInitValues(TransactionBuilder builder){
        LOG.debug("Set Init Values");
        builder.write(ctrlCharacteristic, HPlusConstants.COMMAND_SET_INIT1);
        builder.write(ctrlCharacteristic, HPlusConstants.COMMAND_SET_INIT2);
        return this;
    }

    private HPlusSupport sendUserInfo(TransactionBuilder builder){
        builder.write(ctrlCharacteristic, HPlusConstants.COMMAND_SET_PREF_START);
        builder.write(ctrlCharacteristic, HPlusConstants.COMMAND_SET_PREF_START1);

        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.COMMAND_SET_CONF_SAVE});
        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.COMMAND_SET_CONF_END});
        return this;
    }


    private HPlusSupport setCountry(TransactionBuilder transaction) {
        LOG.info("Attempting to set country...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getCountry(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_COUNTRY,
                        (byte) value
                };
            }
        });
        return this;
    }


    private HPlusSupport setTimeMode(TransactionBuilder transaction) {
        LOG.info("Attempting to set Time Mode...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getTimeMode(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_TIMEMODE,
                        (byte) value
                };
            }
        });
        return this;
    }

    private HPlusSupport setUnit(TransactionBuilder transaction) {
        LOG.info("Attempting to set Units...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getUnit(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_UNIT,
                        (byte) value
                };
            }
        });
        return this;
    }

    private HPlusSupport setCurrentDate(TransactionBuilder transaction) {
        LOG.info("Attempting to set Current Date...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR) - 1900;
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_DATE,
                        (byte)  (year / 256),
                        (byte)  (year % 256),
                        (byte)  (month),
                        (byte)  (day)
                };
            }
        });
        return this;
    }

    private HPlusSupport setCurrentTime(TransactionBuilder transaction) {
        LOG.info("Attempting to set Current Time...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {
                Calendar c = Calendar.getInstance();

                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_TIME,
                        (byte) c.get(Calendar.HOUR_OF_DAY),
                        (byte) c.get(Calendar.MINUTE),
                        (byte) c.get(Calendar.SECOND)
                };
            }
        });
        return this;
    }


    private HPlusSupport setDayOfWeek(TransactionBuilder transaction) {
        LOG.info("Attempting to set Day Of Week...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {
                Calendar c = Calendar.getInstance();

                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_WEEK,
                        (byte) c.get(Calendar.DAY_OF_WEEK)
                };
            }
        });
        return this;
    }


    private HPlusSupport setSIT(TransactionBuilder transaction) {
        LOG.info("Attempting to set SIT...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {
                Calendar c = Calendar.getInstance();

                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_SIT,
                        0, 0, 0, 0, 0
                };
            }
        });
        return this;
    }

    private HPlusSupport setWeight(TransactionBuilder transaction) {
        LOG.info("Attempting to set Weight...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getUserWeight(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_WEIGHT,
                        (byte) value
                };
            }
        });
        return this;
    }

    private HPlusSupport setHeight(TransactionBuilder transaction) {
        LOG.info("Attempting to set Height...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getUserHeight(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_HEIGHT,
                        (byte) value
                };
            }
        });
        return this;
    }


    private HPlusSupport setAge(TransactionBuilder transaction) {
        LOG.info("Attempting to set Age...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getUserAge(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_AGE,
                        (byte) value
                };
            }
        });
        return this;
    }

    private HPlusSupport setSex(TransactionBuilder transaction) {
        LOG.info("Attempting to set Sex...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getUserSex(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_SEX,
                        (byte) value
                };
            }
        });
        return this;
    }


    private HPlusSupport setGoal(TransactionBuilder transaction) {
        LOG.info("Attempting to set Sex...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                int value = HPlusCoordinator.getGoal(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_GOAL,
                        (byte) (value / 256),
                        (byte) (value % 256)
                };
            }
        });
        return this;
    }


    private HPlusSupport setScreenTime(TransactionBuilder transaction) {
        LOG.info("Attempting to set Screentime...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getScreenTime(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_SCREENTIME,
                        (byte) value
                };
            }
        });
        return this;
    }

    private HPlusSupport setAllDayHeart(TransactionBuilder transaction) {
        LOG.info("Attempting to set All Day HR...");
        transaction.add(new ConditionalWriteAction(ctrlCharacteristic) {
            @Override
            protected byte[] checkCondition() {

                byte value = HPlusCoordinator.getAllDayHR(getDevice().getAddress());
                return new byte[]{
                        HPlusConstants.COMMAND_SET_PREF_ALLDAYHR,
                        (byte) value
                };
            }
        });
        return this;
    }


    private HPlusSupport setAlarm(TransactionBuilder transaction) {
        LOG.info("Attempting to set Alarm...");
        return this;
    }

    private HPlusSupport setBlood(TransactionBuilder transaction) {
        LOG.info("Attempting to set Blood...");
        return this;
    }


    private HPlusSupport setFindMe(TransactionBuilder transaction) {
        LOG.info("Attempting to set Findme...");
        return this;
    }

    private HPlusSupport requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        BluetoothGattCharacteristic deviceName = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_GAP_DEVICE_NAME);
        builder.read(deviceName);
        return this;
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }


    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void pair() {
        LOG.debug("Pair");
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.warn("Device info: " + info);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        LOG.debug("Got Notification");
        showText(notificationSpec.body);
    }


    @Override
    public void onSetTime() {
        TransactionBuilder builder = new TransactionBuilder("vibration");
        setCurrentDate(builder);
        setCurrentTime(builder);

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch(callSpec.command){
            case CallSpec.CALL_INCOMING: {
                showText(callSpec.name, callSpec.number);
                break;
            }
        }

    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        LOG.debug("Canned Messages: "+cannedMessagesSpec);
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
    public void onAppConfiguration(UUID appUuid, String config) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchActivityData() {

    }

    @Override
    public void onReboot() {

    }

    @Override
    public void onHeartRateTest() {
        LOG.debug("On HeartRateTest");

        getQueue().clear();

        TransactionBuilder builder = new TransactionBuilder("HeartRateTest");
        byte state = 0;

        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.COMMAND_SET_PREF_ALLDAYHR, 0x10}); //Set Real Time... ?
        builder.queue(getQueue());

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        LOG.debug("Set Real Time HR Measurement: " + enable);

        getQueue().clear();

        TransactionBuilder builder = new TransactionBuilder("realTimeHeartMeasurement");
        byte state = 0;

        if(enable)
            state = HPlusConstants.HEARTRATE_ALLDAY_ON;
        else
            state = HPlusConstants.HEARTRATE_ALLDAY_OFF;

        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.COMMAND_SET_PREF_ALLDAYHR, state});
        builder.queue(getQueue());
    }

    @Override
    public void onFindDevice(boolean start) {
        LOG.debug("Find Me");

        getQueue().clear();
        ctrlCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_CONTROL);

        TransactionBuilder builder = new TransactionBuilder("findMe");

        byte[] msg = new byte[2];
        msg[0] = HPlusConstants.COMMAND_SET_PREF_FINDME;

        if(start)
            msg[1] = 1;
        else
            msg[1] = 0;
        builder.write(ctrlCharacteristic, msg);
        builder.queue(getQueue());
    }

    @Override
    public void onSetConstantVibration(int intensity) {
        LOG.debug("Vibration Trigger");

        getQueue().clear();

        ctrlCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_CONTROL);

        TransactionBuilder builder = new TransactionBuilder("vibration");

        byte[] msg = new byte[15];
        msg[0] = HPlusConstants.COMMAND_SET_DISPLAY_ALERT;

        for(int i = 0;i<msg.length - 1; i++)
            msg[i + 1] = (byte) "GadgetBridge".charAt(i);

        builder.write(ctrlCharacteristic, msg);
        builder.queue(getQueue());
    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {
        LOG.debug("Send Configuration: "+config);

    }

    @Override
    public void onTestNewFunction() {
        LOG.debug("Test New Function");

    }


    private void showText(String message){
        showText(null, message);
    }

    private void showText(String title, String body){
        LOG.debug("Show Notification");

            TransactionBuilder builder = new TransactionBuilder("showText");
            if(ctrlCharacteristic == null)
                ctrlCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_CONTROL);

            byte[] msg = new byte[20];
            for(int i = 0; i < msg.length; i++)
                msg[i] = 32;

            msg[0] = HPlusConstants.COMMAND_SET_DISPLAY_TEXT;

            String message = "";

            if(title != null){
                if(title.length() > 12) {
                    message = title.substring(0, 12);
                }else {
                    message = title;
                    for(int i = message.length(); i < 12; i++)
                        message += "";
                }
            }
            message += body;

            int length = message.length() / 17;

            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.COMMAND_SET_INCOMMING_SOCIAL, (byte) length});

            int remaining = 0;

            if(message.length() % 17 > 0)
                remaining = length + 1;
            else
                remaining = length;

            msg[1] = (byte) remaining;
            int message_index = 0;
            int i = 3;

            for(int j=0; j < message.length(); j++){
                msg[i++] = (byte) message.charAt(j);

                if(i == msg.length){
                    message_index ++;
                    msg[2] = (byte) message_index;
                    builder.write(ctrlCharacteristic, msg);

                    msg = msg.clone();
                    for(i=3; i < msg.length; i++)
                        msg[i] = 32;

                    if(message_index < remaining)
                        i = 3;
                    else
                        break;
                }
            }

            msg[2] = (byte) remaining;

            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
    }

    public boolean isExpectedDevice(BluetoothDevice device) {
        return true;
    }

    public void close() {
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if(data.length == 0)
            return true;

        switch(data[0]){
            case HPlusConstants.DATA_STATS:
                    return processDataStats(data);
            case HPlusConstants.DATA_SLEEP:
                    return processSleepStats(data);
            default:
                LOG.info("Unhandled characteristic changed: " + characteristicUUID);

        }
        return false;
    }

    private boolean processSleepStats(byte[] data){
        LOG.debug("Process Sleep Stats");

        if(data.length < 19) {
            LOG.error("Invalid Sleep Message Length " + data.length);
            return false;
        }
        HPlusSleepRecord record = new HPlusSleepRecord(data);

        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            Device device = DBHelper.getDevice(getDevice(), session);
            User user = DBHelper.getUser(session);
            int ts = (int) (System.currentTimeMillis() / 1000);
            HPlusSampleProvider provider = new HPlusSampleProvider(gbDevice, session);

            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, record.getRawData() )
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, System.currentTimeMillis());

            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

        }catch (GBException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


    private boolean processDataStats(byte[]data){
        LOG.debug("Process Data Stats");

        if(data.length < 15) {
            LOG.error("Invalid Stats Message Length " + data.length);
            return false;
        }
        double distance = ( (int) data[4] * 256 + data[3]) / 100.0;

        int x = (int) data[6] * 256 + data[5];
        int y = (int) data[8] * 256 + data[7];
        int calories = x + y;

        int bpm = (data[11] == -1) ? HPlusHealthActivitySample.NOT_MEASURED : data[11];

        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            Device device = DBHelper.getDevice(getDevice(), session);
            User user = DBHelper.getUser(session);
            int ts = (int) (System.currentTimeMillis() / 1000);
            HPlusSampleProvider provider = new HPlusSampleProvider(gbDevice, session);


            if (bpm != HPlusHealthActivitySample.NOT_MEASURED) {
                HPlusHealthActivitySample sample = createActivitySample(device, user, ts, provider);
                sample.setHeartRate(bpm);
                provider.addGBActivitySample(sample);
            }
        }catch (GBException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return true;
    }

    public HPlusHealthActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        HPlusHealthActivitySample sample = new HPlusHealthActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);

        return sample;
    }
}
