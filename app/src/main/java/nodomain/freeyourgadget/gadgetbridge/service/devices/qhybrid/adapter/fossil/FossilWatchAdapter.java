/*  Copyright (C) 2019-2020 Daniel Dakhno

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig.ConfigFileBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig.ConfigPayload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm.AlarmsSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.NotificationFilterPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayTextNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_STEP_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_TIMEZONE_OFFSET;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_VIBRATION_STRENGTH;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_EVENT_BUTTON_PRESS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_EVENT_MULTI_BUTTON_PRESS;

public class FossilWatchAdapter extends WatchAdapter {
    private ArrayList<Request> requestQueue = new ArrayList<>();

    private FossilRequest fossilRequest;

    private int MTU = 23;

    private final String ITEM_MTU = "MTU";
    static public final String ITEM_BUTTONS = "BUTTONS";

    private final String CONFIG_ITEM_STEP_GOAL = "step_goal";
    private final String CONFIG_ITEM_VIBRATION_STRENGTH = "vibration_strength";
    private final String CONFIG_ITEM_TIMEZONE_OFFSET = "timezone_offset";
    public final String CONFIG_ITEM_BUTTONS = "buttons";

    private int lastButtonIndex = -1;

    protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public FossilWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }


    @Override
    public void initialize() {
        playPairingAnimation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512), false);
        }
        // queueWrite(new FileCloseRequest((short) 0xFFFF));
        // queueWrite(new ConfigurationGetRequest(this), false);

        syncConfiguration();

        syncNotificationSettings();

        syncButtonSettings();

        /* queueWrite(new ButtonConfigurationGetRequest(this) {
            @Override
            public void onConfigurationsGet(ConfigPayload[] configs) {
                super.onConfigurationsGet(configs);

                JSONArray buttons = new JSONArray();
                for (ConfigPayload payload : configs) buttons.put(String.valueOf(payload));
                String json = buttons.toString();
                getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(ITEM_BUTTONS, json));
            }
        }); */

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED), false);
    }

    private void syncButtonSettings(){
        String buttonConfig = getDeviceSpecificPreferences().getString(CONFIG_ITEM_BUTTONS, null);
        getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(ITEM_BUTTONS, buttonConfig));
        overwriteButtons(buttonConfig);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if(status != BluetoothGatt.GATT_SUCCESS){
            if(characteristic.getUuid().toString().equals("3dda0005-957f-7d4a-34a6-74696673696d")){
                GB.log("authentication failed", GB.ERROR, null);
                setDeviceState(GBDevice.State.AUTHENTICATION_REQUIRED);
                requestQueue.clear();
            }
            log("characteristic write failed: " + status);
            fossilRequest = null;

            queueNextRequest();
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        log("status " + status + " newState: " + newState);
        if(newState != BluetoothGatt.STATE_CONNECTED){
            log("status " + newState + "  clearing queue...");
            requestQueue.clear();
            fossilRequest = null;
        }
    }

    protected SharedPreferences getDeviceSpecificPreferences(){
        return GBApplication.getDeviceSpecificSharedPrefs(
                getDeviceSupport().getDevice().getAddress()
        );
    }

    private void syncConfiguration(){
        SharedPreferences preferences = getDeviceSpecificPreferences();

        int stepGoal = preferences.getInt(CONFIG_ITEM_STEP_GOAL, 1000000);
        byte vibrationStrength = (byte) preferences.getInt(CONFIG_ITEM_VIBRATION_STRENGTH, 100);
        int timezoneOffset = preferences.getInt(CONFIG_ITEM_TIMEZONE_OFFSET, 0);

        GBDevice device = getDeviceSupport().getDevice();

        device.addDeviceInfo(new GenericItem(ITEM_STEP_GOAL, String.valueOf(stepGoal)));
        device.addDeviceInfo(new GenericItem(ITEM_VIBRATION_STRENGTH, String.valueOf(vibrationStrength)));
        device.addDeviceInfo(new GenericItem(ITEM_TIMEZONE_OFFSET, String.valueOf(timezoneOffset)));

        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[]{
                new ConfigurationPutRequest.DailyStepGoalConfigItem(stepGoal),
                new ConfigurationPutRequest.VibrationStrengthConfigItem(vibrationStrength),
                new ConfigurationPutRequest.TimezoneOffsetConfigItem((short) timezoneOffset)
        }, this));
    }

    public int getMTU() {
        if (this.MTU < 0) throw new RuntimeException("MTU not configured");

        return this.MTU;
    }

    @Override
    public void playPairingAnimation() {
        queueWrite(new AnimationRequest(), false);
    }

    @Override
    public void playNotification(NotificationConfiguration config) {
        if (config.getPackageName() == null) {
            log("package name in notification not set");
            return;
        }
        queueWrite(new PlayTextNotificationRequest(config.getPackageName(), this), false);
    }

    @Override
    public void setTime() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();

        queueWrite(
                new ConfigurationPutRequest(
                        new ConfigurationPutRequest.TimeConfigItem(
                                (int) (millis / 1000 + getDeviceSupport().getTimeOffset() * 60),
                                (short) (millis % 1000),
                                (short) ((zone.getRawOffset() + (zone.inDaylightTime(new Date()) ? 1 : 0)) / 60000)
                        ),
                        this), false
        );
    }

    @Override
    public void overwriteButtons(String jsonConfigString) {
        try {
            if(jsonConfigString == null) return;
            getDeviceSpecificPreferences()
                    .edit()
                    .putString(CONFIG_ITEM_BUTTONS, jsonConfigString)
                    .apply();
            JSONArray buttonConfigJson = new JSONArray(jsonConfigString);
            // JSONArray buttonConfigJson = new JSONArray(getDeviceSupport().getDevice().getDeviceInfo(ITEM_BUTTONS).getDetails());

            ConfigPayload[] payloads = new ConfigPayload[buttonConfigJson.length()];

            for (int i = 0; i < buttonConfigJson.length(); i++) {
                try {
                    payloads[i] = ConfigPayload.valueOf(buttonConfigJson.getString(i));
                } catch (IllegalArgumentException e) {
                    payloads[i] = ConfigPayload.FORWARD_TO_PHONE;
                }
            }

            ConfigFileBuilder builder = new ConfigFileBuilder(payloads);

            FilePutRequest fileUploadRequets = new FilePutRequest((short) 0x0600, builder.build(true), this) {
                @Override
                public void onFilePut(boolean success) {
                    if (success)
                        GB.toast("successfully overwritten button settings", Toast.LENGTH_SHORT, GB.INFO);
                    else GB.toast("error overwriting button settings", Toast.LENGTH_SHORT, GB.INFO);
                }
            };
            queueWrite(fileUploadRequets);
        } catch (JSONException e) {
            GB.log("error", GB.ERROR, e);
        }
    }

    @Override
    public void setActivityHand(double progress) {
        queueWrite(new ConfigurationPutRequest(
                new ConfigurationPutRequest.CurrentStepCountConfigItem(Math.min(999999, (int) (1000000 * progress))),
                this
        ), false);
    }

    @Override
    public void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1), false);
    }


    public void vibrate(nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest.VibrationType vibration) {
        // queueWrite(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest(vibration, -1, -1));
    }

    @Override
    public void vibrateFindMyDevicePattern() {

    }


    @Override
    public void requestHandsControl() {
        queueWrite(new RequestHandControlRequest(), false);
    }

    @Override
    public void releaseHandsControl() {
        queueWrite(new ReleaseHandsControlRequest(), false);
    }

    @Override
    public void setStepGoal(int stepGoal) {
        getDeviceSpecificPreferences()
                .edit()
                .putInt(CONFIG_ITEM_STEP_GOAL, stepGoal)
                .apply();

        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.DailyStepGoalConfigItem(stepGoal), this) {
            @Override
            public void onFilePut(boolean success) {
                if (success)
                    GB.toast("successfully updated step goal", Toast.LENGTH_SHORT, GB.INFO);
                else GB.toast("error updating step goal", Toast.LENGTH_SHORT, GB.INFO);
            }
        }, false);
    }

    @Override
    public void setVibrationStrength(short strength) {
        getDeviceSpecificPreferences()
                .edit()
                .putInt(CONFIG_ITEM_VIBRATION_STRENGTH, (byte) strength)
                .apply();

        ConfigurationPutRequest.ConfigItem vibrationItem = new ConfigurationPutRequest.VibrationStrengthConfigItem((byte) strength);


        queueWrite(
                new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[]{vibrationItem}, this) {
                    @Override
                    public void onFilePut(boolean success) {
                        if (success)
                            GB.toast("successfully updated vibration strength", Toast.LENGTH_SHORT, GB.INFO);
                        else
                            GB.toast("error updating vibration strength", Toast.LENGTH_SHORT, GB.INFO);
                    }
                }, false
        );
        // queueWrite(new FileVerifyRequest((short) 0x0800));
    }

    @Override
    public void syncNotificationSettings() {
        log("syncing notification settings...");
        try {
            PackageConfigHelper helper = new PackageConfigHelper(getContext());
            final ArrayList<NotificationConfiguration> configurations = helper.getNotificationConfigurations();
            if (configurations.size() == 1) configurations.add(configurations.get(0));
            queueWrite(new NotificationFilterPutRequest(configurations, FossilWatchAdapter.this) {
                @Override
                public void onFilePut(boolean success) {
                    super.onFilePut(success);

                    if (!success) {
                        GB.toast("error writing notification settings", Toast.LENGTH_SHORT, GB.ERROR);

                        getDeviceSupport().getDevice().setState(GBDevice.State.NOT_CONNECTED);
                        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                    }

                    getDeviceSupport().getDevice().setState(GBDevice.State.INITIALIZED);
                    getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                }
            }, false);
        } catch (Exception e) {
            GB.log("error", GB.ERROR, e);
        }
    }

    @Override
    public void onTestNewFunction() {
        queueWrite(new FilePutRequest(
                (short) 0x0600,
                new byte[]{
                        (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x01, (byte) 0x01, (byte) 0x24, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x30, (byte) 0x52, (byte) 0xFF, (byte) 0x26, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x09, (byte) 0x04, (byte) 0x01, (byte) 0x03, (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x09, (byte) 0x04, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x24, (byte) 0x00, (byte) 0x00, (byte) 0x24, (byte) 0x00, (byte) 0x08, (byte) 0x01, (byte) 0x50, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x1F, (byte) 0xBE, (byte) 0xB4, (byte) 0x1B
                },
                this)
        );
    }

    @Override
    public void setTimezoneOffsetMinutes(short offset) {
        getDeviceSpecificPreferences()
                .edit()
                .putInt(CONFIG_ITEM_TIMEZONE_OFFSET, offset)
                .apply();

        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.TimezoneOffsetConfigItem(offset), this){
            @Override
            public void onFilePut(boolean success) {
                super.onFilePut(success);

                if(success) GB.toast("successfully updated timezone", Toast.LENGTH_SHORT, GB.INFO);
                else GB.toast("error updating timezone", Toast.LENGTH_SHORT, GB.ERROR);
            }
        });
    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsExtendedVibration() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
            case "DN.1.0":
                return true;
        }
        throw new UnsupportedOperationException("model " + modelNumber + " not supported");
    }

    @Override
    public boolean supportsActivityHand() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
            case "DN.1.0":
                return false;
        }
        throw new UnsupportedOperationException("Model " + modelNumber + " not supported");
    }

    @Override
    public void onFetchActivityData() {

        // queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[0], this));
        setVibrationStrength((byte) 50);
        // queueWrite(new FileCloseRequest((short) 0x0800));
        // queueWrite(new ConfigurationGetRequest(this));
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
       //  throw new RuntimeException("noope");
        ArrayList<nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm.Alarm> activeAlarms = new ArrayList<>();
        for (Alarm alarm : alarms){
            if(!alarm.getEnabled()) continue;
            if(alarm.getRepetition() == 0){
                activeAlarms.add(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm.Alarm(
                        (byte) alarm.getMinute(),
                        (byte) alarm.getHour(),
                        alarm.getTitle(),
                        alarm.getDescription()
                ));
                continue;
            }
            int repitition = alarm.getRepetition();
            repitition = (repitition << 1) | ((repitition >> 6) & 1);
            activeAlarms.add(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm.Alarm(
                    (byte) alarm.getMinute(),
                    (byte) alarm.getHour(),
                    (byte) repitition,
                    alarm.getTitle(),
                    alarm.getDescription()
            ));
        }
        queueWrite(new AlarmsSetRequest(activeAlarms.toArray(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm.Alarm[0]), this){
            @Override
            public void onFilePut(boolean success) {
                super.onFilePut(success);
                if(success) GB.toast("successfully set alarms", Toast.LENGTH_SHORT, GB.INFO);
                else  GB.toast("error setting alarms", Toast.LENGTH_SHORT, GB.INFO);
            }
        });
    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()) {
            case "3dda0006-957f-7d4a-34a6-74696673696d": {
                handleBackgroundCharacteristic(characteristic);
                break;
            }
            case "00002a37-0000-1000-8000-00805f9b34fb": {
                handleHeartRateCharacteristic(characteristic);
                break;
            }
            case "3dda0002-957f-7d4a-34a6-74696673696d":
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0005-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                if (fossilRequest != null) {
                    boolean requestFinished;
                    try {
                        if (characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")) {
                            byte requestType = (byte) (characteristic.getValue()[0] & 0x0F);

                            if (requestType != 0x0A && requestType != fossilRequest.getType()) {
                                // throw new RuntimeException("Answer type " + requestType + " does not match current request " + fossilRequest.getType());
                            }
                        }

                        fossilRequest.handleResponse(characteristic);
                        requestFinished = fossilRequest.isFinished();
                    } catch (RuntimeException e) {
                        if(characteristic.getUuid().toString().equals("3dda0005-957f-7d4a-34a6-74696673696d")){
                            GB.log("authentication failed", GB.ERROR, null);
                            setDeviceState(GBDevice.State.AUTHENTICATION_REQUIRED);
                            requestQueue.clear();
                        }

                        GB.log("error", GB.ERROR, e);
                        getDeviceSupport().notifiyException(fossilRequest.getName(), e);
                        GB.toast(fossilRequest.getName() + " failed", Toast.LENGTH_SHORT, GB.ERROR);
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log(fossilRequest.getName() + " finished");
                        fossilRequest = null;
                    } else {
                        return true;
                    }
                }
                queueNextRequest();
            }
        }
        return true;
    }

    public void handleHeartRateCharacteristic(BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            if (this.supportsExtendedVibration()) {
                GB.toast("Device does not support brr brr", Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (UnsupportedOperationException e) {
            getDeviceSupport().notifiyException(e);
        }

        if (start && getDeviceSupport().searchDevice) return;

        getDeviceSupport().searchDevice = start;

        if (start) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int i = 0;
                    while (getDeviceSupport().searchDevice) {
                        vibrateFindMyDevicePattern();
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            GB.log("error", GB.ERROR, e);
                        }
                    }
                }
            }).start();
        }
    }

    protected void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        switch (value[1]) {
            case 2: {
                byte syncId = value[2];
                getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(QHybridSupport.ITEM_LAST_HEARTBEAT, DateFormat.getTimeInstance().format(new Date())));
                break;
            }
            case 8: {
                if (value.length != 12) {
                    throw new RuntimeException("wrong button message");
                }
                int index = value[2] & 0xFF;
                int button = value[9] >> 4 & 0xFF;

                if (index != this.lastButtonIndex) {
                    lastButtonIndex = index;
                    log("Button press on button " + button);

                    Intent i = new Intent(QHYBRID_EVENT_BUTTON_PRESS);
                    i.putExtra("BUTTON", button);
                    getContext().sendBroadcast(i);
                }
                break;
            }

            case 5: {
                if (value.length != 4) {
                    throw new RuntimeException("wrong button message");
                }
                int action = value[3];

                String actionString = "SINGLE";
                if(action == 3) actionString = "DOUBLE";
                else if(action == 4) actionString = "LONG";

                // lastButtonIndex = index;
                log(actionString + " button press");

                Intent i = new Intent(QHYBRID_EVENT_MULTI_BUTTON_PRESS);
                i.putExtra("ACTION", actionString);
                getContext().sendBroadcast(i);
                break;
            }
        }
    }


    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);

        if(this.MTU == mtu){
            log("MTU changed, same value tho");
            return;
        }

        log("MTU changed: " + mtu);

        this.MTU = mtu;

        getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(ITEM_MTU, String.valueOf(mtu)));
        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());

        ((RequestMtuRequest) fossilRequest).setFinished(true);
        queueNextRequest();
    }

    public void queueWrite(RequestMtuRequest request, boolean priorise) {
        log("is connected: " + getDeviceSupport().isConnected());
        if(!getDeviceSupport().isConnected()){
            log("dropping requetst " + request.getName());
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new TransactionBuilder("requestMtu")
                    .requestMtu(512)
                    .queue(getDeviceSupport().getQueue());

            this.fossilRequest = request;
        }
    }

    private void log(String message) {
        logger.debug(message);
    }

    public void queueWrite(SetDeviceStateRequest request, boolean priorise) {
        if (fossilRequest != null && !fossilRequest.isFinished()) {
            log("queing request: " + request.getName());
            if (priorise) {
                requestQueue.add(0, request);
            } else {
                requestQueue.add(request);
            }
            return;
        }
        log("setting device state: " + request.getDeviceState());
        setDeviceState(request.getDeviceState());
        queueNextRequest();
    }

    private void setDeviceState(GBDevice.State state){
        getDeviceSupport().getDevice().setState(state);
        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
    }

    public void queueWrite(FossilRequest request, boolean priorise) {
        log("is connected: " + getDeviceSupport().isConnected());
        if(!getDeviceSupport().isConnected()){
            log("dropping requetst " + request.getName());
            return;
        }
        if (fossilRequest != null && !fossilRequest.isFinished()) {
            log("queing request: " + request.getName());
            if (priorise) {
                requestQueue.add(0, request);
            } else {
                requestQueue.add(request);
            }
            return;
        }
        log("executing request: " + request.getName());
        this.fossilRequest = request;
        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());

        if(request.isFinished()){
            this.fossilRequest = null;
            queueNextRequest();
        }
    }

    public void queueWrite(Request request, boolean priorise) {
        log("is connected: " + getDeviceSupport().isConnected());
        if(!getDeviceSupport().isConnected()){
            log("dropping requetst " + request.getName());
            return;
        }
        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());

        queueNextRequest();
    }

    protected void queueWrite(Request request) {
        log("is connected: " + getDeviceSupport().isConnected());
        if(!getDeviceSupport().isConnected()){
            log("dropping requetst " + request.getName());
            return;
        }
        if (request instanceof SetDeviceStateRequest)
            queueWrite((SetDeviceStateRequest) request, false);
        else if (request instanceof RequestMtuRequest)
            queueWrite((RequestMtuRequest) request, false);
        else if (request instanceof FossilRequest) queueWrite((FossilRequest) request, false);
        else queueWrite(request, false);
    }

    private void queueNextRequest() {
        try {
            Request request = requestQueue.remove(0);
            queueWrite(request);
        } catch (IndexOutOfBoundsException e) {
            log("requestsQueue empty");
        }
    }
}
