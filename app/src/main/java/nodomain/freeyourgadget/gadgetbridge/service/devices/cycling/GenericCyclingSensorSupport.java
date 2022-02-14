package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling.CyclingSensorCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data.AverageCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data.DataAccumulator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol.CSCMeasurement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol.CSCProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GenericCyclingSensorSupport extends AbstractBTLEDeviceSupport {
    public static String UUID_CSC_MESAUREMENT = "00002a5b-0000-1000-8000-00805f9b34fb";
    final int AVERAGE_CALCULATION_MIN_TIME_UNITS = 1024 * 3; // a bit more than 3 seconds since sensor counts a seconds in 1024 units
    private static final String TAG = "GenericCyclingSensorSup";

    private int wheelCircumference;
    private int saveIntervalMinutes;

    BluetoothGattCharacteristic measurementCharacteristic;

    private CSCProtocol protocol;
    private DataAccumulator accumulator;
    private AverageCalculator averageCalculator;

    public GenericCyclingSensorSupport() {
        super(LoggerFactory.getLogger(GenericCyclingSensorSupport.class));
        addSupportedService(UUID.fromString(CyclingSensorCoordinator.UUID_CSC));

        protocol = new CSCProtocol();
        accumulator = new DataAccumulator();
        averageCalculator = new AverageCalculator(accumulator);
    }

    private void loadPrefs(){
        SharedPreferences deviceSpecificSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        wheelCircumference = Integer.parseInt(deviceSpecificSharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_CYCLING_SENSOR_WHEEL_CIRCUMFERENCE, "0"));
        saveIntervalMinutes = Integer.parseInt(deviceSpecificSharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_CYCLING_SENSOR_SAVE_INTERVAL, "5"));

        if(wheelCircumference == 0){
            GB.toast("please enter wheel circumference in device settings", Toast.LENGTH_LONG, GB.ERROR);
        }
        if(saveIntervalMinutes == 0){
            GB.toast("Save interval is 0, data will not be saved", Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        measurementCharacteristic = getCharacteristic(UUID.fromString(UUID_CSC_MESAUREMENT));
        builder.notify(measurementCharacteristic, true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        loadPrefs();

        return builder;
    }

    private void handleMeasurement(byte[] value){
        long timeOfArrival = System.currentTimeMillis();
        CSCMeasurement measurement = protocol.parsePacket(timeOfArrival, value);
        accumulator.captureCSCMeasurement(measurement);
        double rpm = averageCalculator.calculateAverageRevolutionsPerSecond(3000);

        Log.d(TAG, "handleMeasurement: " + rpm);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(characteristic.equals(measurementCharacteristic)){
            handleMeasurement(characteristic.getValue());
        }

        return true;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {

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
    public void onReset(int flags) {

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
        loadPrefs();
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

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
