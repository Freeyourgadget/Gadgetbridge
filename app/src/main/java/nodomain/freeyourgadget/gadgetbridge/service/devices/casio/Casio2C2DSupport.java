/*  Copyright (C) 2023-2024 Johannes Krude

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Logging;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BcdUtil;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT_AUTO;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT_DAY_MONTH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT_MONTH_DAY;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OPERATING_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HOURLY_CHIME_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOLIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LIGHT_DURATION_LONGER;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_POWER_SAVING;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CONNECTION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIME_SYNC;


// this class is for those Casio watches which request reads on the 2C characteristic and write on the 2D characteristic

public abstract class Casio2C2DSupport extends CasioSupport {

    public static final byte FEATURE_CURRENT_TIME = 0x09;
    public static final byte FEATURE_ALERT_LEVEL = 0x0a;
    public static final byte FEATURE_BLE_FEATURES = 0x10;
    public static final byte FEATURE_SETTING_FOR_BLE = 0x11;
    public static final byte FEATURE_SETTING_FOR_BASIC = 0x13;
    public static final byte FEATURE_SETTING_FOR_ALM = 0x15;
    public static final byte FEATURE_SETTING_FOR_ALM2 = 0x16;
    public static final byte FEATURE_VERSION_INFORMATION = 0x20;
    public static final byte FEATURE_APP_INFORMATION = 0x22;
    public static final byte FEATURE_WATCH_NAME = 0x23;
    public static final byte FEATURE_MODULE_ID = 0x26;
    public static final byte FEATURE_WATCH_CONDITION = 0x28;
    public static final byte FEATURE_DST_WATCH_STATE = (byte) 0x1d;
    public static final byte FEATURE_DST_SETTING = (byte) 0x1e;
    public static final byte FEATURE_WORLD_CITY = (byte) 0x1f;
    public static final byte FEATURE_REMINDER_TITLE = 0x30;
    public static final byte FEATURE_REMINDER_TIME = 0x31;
    public static final byte FEATURE_CURRENT_TIME_MANAGER = 0x39;
    public static final byte FEATURE_CONNECTION_PARAMETER_MANAGER = 0x3a;
    public static final byte FEATURE_ADVERTISE_PARAMETER_MANAGER = 0x3b;
    public static final byte FEATURE_SETTING_FOR_TARGET_VALUE = 0x43;
    public static final byte FEATURE_SETTING_FOR_USER_PROFILE = 0x45;
    public static final byte FEATURE_SERVICE_DISCOVERY_MANAGER = 0x47;

    protected static Logger LOG;
    LinkedList<RequestWithHandler> requests = new LinkedList<>();

    public Casio2C2DSupport(Logger logger) {
        super(logger);
        LOG = logger;
    }

    @Override
    public boolean connect() {
        requests.clear();
        return super.connect();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        initializeDeviceSettings(builder);
        return builder;
    }

    public void writeAllFeatures(TransactionBuilder builder, byte[] arr) {
        if (!requests.isEmpty()) {
            LOG.warn("writing while waiting for a response may lead to incorrect received responses");
        }
        builder.write(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public void writeAllFeaturesRequest(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public interface ResponseHandler {
        void handle(byte[] response);
    }

    public class FeatureResponses extends HashMap<FeatureRequest, byte[]> {
        public byte[][] get(FeatureRequest[] requests) {
            byte[][] result = new byte[requests.length][];
            for (int i = 0; i < requests.length; i++) {
                byte[] response = get(requests[i]);
                if (response == null)
                    return null;
                result[i] = response;
            }
            return result;
        }
    }

    public interface ResponsesHandler {
        void handle(FeatureResponses responses);
    }

    public static class FeatureRequest {
        byte data[];

        public FeatureRequest(byte arg0) {
            data = new byte[] {arg0};
        }

        public FeatureRequest(byte arg0, byte arg1) {
            data = new byte[] {arg0, arg1};
        }

        public static FeatureRequest parse(String str) {
            byte[] data = RequestWithData.parseData(str);
            if (data == null)
                return null;
            if (data.length == 1) {
                return new FeatureRequest(data[0]);
            } else if (data.length == 2) {
                return new FeatureRequest(data[0], data[1]);
            } else {
                return null;
            }
        }

        public byte[] getData() {
            return data.clone();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof FeatureRequest))
                return false;

            FeatureRequest fr = (FeatureRequest) o;
            return Arrays.equals(data, fr.data);
        }

        public boolean matches(byte[] response) {
            // 0xff is of type int which goes out of range etc. byte is signed.
            if (response.length > 2 && response[0] == (byte) 0xFF && response[1] == (byte) 0x81) {
                if (data.length < response.length - 2)
                    return false;
                for (int i = 2; i < response.length; i++) {
                    if (response[i] != data[i-2])
                        return false;
                }
                return true;
            } else {
                if (response.length < data.length)
                    return false;
                for (int i = 0; i < data.length; i++) {
                    if (response[i] != data[i])
                        return false;
                }
                return true;
            }
        }
    }

    private static class RequestWithData {
        public FeatureRequest request;
        public byte[] data;

        public RequestWithData(FeatureRequest request, byte[] data) {
            this.request = request;
            this.data = data;
        }

        public static RequestWithData parse(String str) {
            String[] kv = str.split(";");
            if (kv.length != 2)
                return null;
            FeatureRequest request = FeatureRequest.parse(kv[0]);
            byte[] data = parseData(kv[1]);
            if (request == null || data == null)
                return null;
            return new RequestWithData(request, data);
        }

        public static byte[] parseData(String str) {
            if (!str.matches("\\A\\[[0-9]+(, [0-9]+)*\\]\\z"))
                return null;
            String[] strings = str.replace("[", "").replace("]", "").split(", ");
            byte[] result = new byte[strings.length];
            for (int i = 0; i < result.length; i++)
                result[i] = (byte) Integer.parseInt(strings[i]);
            return result;
        }
    }

    private static class RequestWithHandler {
        public FeatureRequest request;
        public ResponseHandler handler;

        public RequestWithHandler(FeatureRequest request, ResponseHandler handler) {
            this.request = request;
            this.handler = handler;
        }
    }

    public void requestFeature(TransactionBuilder builder, FeatureRequest request, ResponseHandler handler) {
        builder.notify(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), true);
        writeAllFeaturesRequest(builder, request.getData());
        builder.run((gatt) -> requests.add(new RequestWithHandler(request, handler)));
    }

    public void requestFeatures(TransactionBuilder builder, Set<FeatureRequest> requests, ResponsesHandler handler) {
        FeatureResponses responses = new FeatureResponses();

        HashSet<FeatureRequest> missing = new HashSet();
        for (FeatureRequest request: requests) {
            missing.add(request);
        }

        for (FeatureRequest request: requests) {
            requestFeature(builder, request, data -> {
                responses.put(request, data);
                missing.remove(request);
                if (missing.isEmpty()) {
                    handler.handle(responses);
                }
            });
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            byte[] response = characteristic.getValue();
            Iterator<RequestWithHandler> it = requests.iterator();
            while (it.hasNext()) {
                RequestWithHandler rh = it.next();
                if (rh.request.matches(response)) {
                    it.remove();
                    rh.handler.handle(response);
                    return true;
                }
            }
            LOG.warn("unhandled response: " + Logging.formatBytes(response));
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    public void writeCurrentTime(TransactionBuilder builder, ZonedDateTime time) {
        byte[] arr = new byte[11];
        arr[0] = FEATURE_CURRENT_TIME;
        byte[] tmp = prepareCurrentTime(time);
        System.arraycopy(tmp, 0, arr, 1, 10);

        writeAllFeatures(builder, arr);
    }

    FeatureCache featureCache = new FeatureCache();
    public class FeatureCache {

        Map<FeatureRequest, byte[]> values = null;

        // can not be done on initialization, since the SharedPreferences are not yet available
        void load() {
            values = new HashMap();
            Set<String> serialized = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getStringSet("casio_features_current_values", new HashSet());
            if (serialized == null)
                return;

            for (String str: serialized) {
                if (str == null)
                    continue;
                RequestWithData entry = RequestWithData.parse(str);
                if (entry == null) {
                    LOG.warn("invalid casio_features_current_values entry: " + str);
                    continue;
                }
                values.put(entry.request, entry.data);
            }
        }

        public void save(SharedPreferences.Editor editor) {
            if (values == null)
                return;

            Set<String> serialized = new HashSet();

            for (Map.Entry<FeatureRequest, byte[]> entry: values.entrySet()) {
                serialized.add(Arrays.toString(entry.getKey().getData()) + ";" + Arrays.toString(entry.getValue()));
            }

            editor.putStringSet("casio_features_current_values", serialized);
        }

        public byte[] get(FeatureRequest request) {
            if (values == null)
                load();
            return values.get(request);
        }

        public byte[][] get(FeatureRequest[] requests) {
            byte[][] result = new byte[requests.length][];
            for (int i = 0; i < requests.length; i++) {
                byte[] response = get(requests[i]);
                if (response == null)
                    return null;
                result[i] = response;
            }
            return result;
        }

        public void add(Map<FeatureRequest, byte[]> entries, SharedPreferences.Editor editor) {
            if (values == null)
                load();
            for (Map.Entry<FeatureRequest, byte[]> entry: entries.entrySet()) {
                values.put(entry.getKey(), entry.getValue());
            }
            save(editor);
        }
    }

    public interface UpdateValuesHandler {
        public void apply(byte[][] data);
    }
    public interface ReadValuesHandler {
        public boolean apply(byte[][] data);
    }

    public abstract class DeviceSetting {
        // which features to request
        public abstract FeatureRequest[] getFeatureRequests();
        // compares and updates watch data, cached previous data and GB state, returns true if data was changed
        public abstract boolean mergeValues(byte[][] data, byte[][] previous, SharedPreferences.Editor editor);

        public final boolean mergeValues(byte[][] data, byte[][] previous, UpdateValuesHandler updateValues, ReadValuesHandler readValues) {
            boolean needsUpdating = false;
            // check if GB state has changed
            if (previous != null) {
                byte[][] copies = new byte[previous.length][];
                for (int i = 0; i < previous.length; i++) {
                       copies[i] = previous[i].clone();
                }
                updateValues.apply(copies);
                for (int i = 0; i < previous.length; i++) {
                    if (!Arrays.equals(previous[i], copies[i])) {
                        needsUpdating = true;
                        break;
                    }
                }
            }
            // update GB state and check if data needs change
            if (!needsUpdating) {
                needsUpdating = readValues.apply(data);
            }
            // maybe update data
            if (needsUpdating) {
                updateValues.apply(data);
                return true;
            }
            return false;
        }
    };

    ArrayList<DeviceSetting> deviceSettings = new ArrayList();
    DeviceAlarms deviceAlarms = new DeviceAlarms();
    DeviceReminders deviceReminders = new DeviceReminders();
    HashMap<String, DevicePreference> devicePreferenceByName = new HashMap();
    {
        deviceSettings.add(deviceAlarms);
        deviceSettings.add(deviceReminders);

        for (DevicePreference pref: supportedDevicePreferences()) {
            deviceSettings.add(pref);
            devicePreferenceByName.put(pref.getName(), pref);
        }
    }

    void initializeDeviceSettings(TransactionBuilder builder) {
        Set<FeatureRequest> deviceSettingFeatures = new LinkedHashSet();
        for (DeviceSetting ds: deviceSettings)
            deviceSettingFeatures.addAll(Arrays.asList(ds.getFeatureRequests()));

        requestFeatures(builder, deviceSettingFeatures, responses -> {
            LinkedHashSet<FeatureRequest> override = new LinkedHashSet();

            SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
            for (DeviceSetting ds: deviceSettings) {
                FeatureRequest[] requests = ds.getFeatureRequests();
                byte[][] data = responses.get(requests);
                if (data == null)
                    continue;
                byte[][] previous = featureCache.get(requests);
                if (ds.mergeValues(data, previous, editor)) {
                    override.addAll(Arrays.asList(requests));
                }
            }

            featureCache.add(responses, editor);
            editor.apply();

            if (!override.isEmpty()) {
                ArrayList<byte[]> updatedSettings = new ArrayList();
                for (FeatureRequest fr: override) {
                    updatedSettings.add(responses.get(fr));
                }
                sendSettingsToDevice(updatedSettings.toArray(new byte[][] {}));
            }
        });
    }

    void sendSettingsToDevice(byte[][] settings) {
        TransactionBuilder builder = createTransactionBuilder("DeviceSetting.write");
        for (byte[] data: settings) {
            writeAllFeatures(builder, data);
        }
        builder.run((gatt) -> GB.toast(getContext(), getContext().getString(R.string.user_feedback_set_settings_ok), Toast.LENGTH_SHORT, GB.INFO));
        builder.queue(getQueue());
    }

    public abstract class DeviceItems<Item> extends DeviceSetting {

        public void onSet(ArrayList<? extends Item> items) {
            if (!isInitialized()) {
                return;
            }
            byte[][] currentValues = featureCache.get(getFeatureRequests());
            if (currentValues == null) {
                return;
            }
            updateValues(currentValues, items);

            SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
            featureCache.save(editor);
            editor.apply();

            sendSettingsToDevice(currentValues);
        }

        public abstract void updateValues(byte[][] data, List<? extends Item> items);

    }

    public class DeviceAlarms extends DeviceItems<Alarm> {

        @Override
        public FeatureRequest[] getFeatureRequests() {
            final int maxAlarms = gbDevice.getDeviceCoordinator().getAlarmSlotCount(gbDevice);

            if (maxAlarms == 0) {
                return new FeatureRequest[] {};
            } else if (maxAlarms == 1) {
                return new FeatureRequest[] {new FeatureRequest(FEATURE_SETTING_FOR_ALM)};
            } else {
                return new FeatureRequest[] {new FeatureRequest(FEATURE_SETTING_FOR_ALM), new FeatureRequest(FEATURE_SETTING_FOR_ALM2)};
            }
        };

        @Override
        public void updateValues(byte[][] data, List<? extends Alarm> alarms) {
            for (Alarm alarm: alarms) {
                updateValues(data, alarm);
            }
        }

        public void updateValues(byte[][] data, Alarm alarm) {
            int pos = alarm.getPosition();
            int alm = pos == 0 ? 0 : 1;
            int index = pos == 0 ? 1 : 1 + 4 * (pos-1);
            if (data.length <= alm || index + 4 > data[alm].length) {
                LOG.error("alarm data too small");
            }
            updateValue(data[alm], index, alarm);
        }

        public void updateValue(byte[] data, int index, Alarm alarm) {
            if(alarm.getEnabled()) {
                data[index] |= 0x40;
            } else {
                data[index] &= ~0x40;
            }
            //data[index+1] = 0x40;
            data[index+2] = (byte) alarm.getHour();
            data[index+3] = (byte) alarm.getMinute();
        }

        @Override
        public final boolean mergeValues(byte[][] data, byte[][] previous, SharedPreferences.Editor editor) {
            boolean changed = false;

            LinkedHashSet<Integer> foundAlarms = new LinkedHashSet();
            for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm: DBHelper.getAlarms(gbDevice)) {
                foundAlarms.add(alarm.getPosition());
                if (mergeValues(data, previous, uData -> updateValues(uData, alarm), rData -> readValues(rData, alarm, false))) {
                    changed = true;
                }
            }

            final int maxAlarms = gbDevice.getDeviceCoordinator().getAlarmSlotCount(gbDevice);
            for (int i = 0; i < maxAlarms; i++) {
                if (!foundAlarms.contains(i)) {
                    nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm = AlarmUtils.createDefaultAlarm(gbDevice, i);
                    if (alarm == null) {
                        continue;
                    }
                    readValues(data, alarm, true);
                }
            }
            return changed;
        }

        public boolean readValues(byte[][] data, nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm, boolean forceStore) {
            int pos = alarm.getPosition();
            int alm = pos == 0 ? 0 : 1;
            int index = pos == 0 ? 1 : 1+ 4 * (pos-1);
            if (data.length <= alm || index + 4 > data[alm].length) {
                LOG.error("alarm data too small");
            }
            return readValues(data[alm], index, alarm, forceStore);
        }

        public boolean readValues(byte[] data, int index, nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm, boolean forceStore) {
            boolean enabled = (data[index] & 0x40) == 0x40;
            byte hour   = data[index+2];
            byte minute = data[index+3];

            if (forceStore || alarm.getEnabled() != enabled || alarm.getHour() != hour || alarm.getMinute() != minute) {
                alarm.setEnabled(enabled);
                alarm.setHour(hour);
                alarm.setMinute(minute);
                alarm.setRepetition(Alarm.ALARM_MON | Alarm.ALARM_TUE | Alarm.ALARM_WED | Alarm.ALARM_THU | Alarm.ALARM_FRI | Alarm.ALARM_SAT | Alarm.ALARM_SUN);
                DBHelper.store(alarm);
                Intent intent = new Intent(DeviceService.ACTION_SAVE_ALARMS);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            }
            return false;
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        deviceAlarms.onSet(alarms);
    }

    public class DeviceReminders extends DeviceItems<Reminder> {

        @Override
        public FeatureRequest[] getFeatureRequests() {
            final int maxReminders = gbDevice.getDeviceCoordinator().getReminderSlotCount(gbDevice);

            FeatureRequest[] requests = new FeatureRequest[maxReminders*2];
            for (int i = 0; i < maxReminders; i++) {
                requests[2*i]   = new FeatureRequest(FEATURE_REMINDER_TITLE, (byte) (i+1));
                requests[2*i+1] = new FeatureRequest(FEATURE_REMINDER_TIME, (byte) (i+1));
            }
            return requests;
        };

        @Override
        public void updateValues(byte[][] data, List<? extends Reminder> reminders) {
            final int maxReminders = gbDevice.getDeviceCoordinator().getReminderSlotCount(gbDevice);
            for (int i = 0; i < maxReminders; i++) {
                if (i < reminders.size()) {
                    updateValues(data, i, reminders.get(i));
                } else {
                    Arrays.fill(data[2*i],   2, 20, (byte) 0);
                    Arrays.fill(data[2*i+1], 2, 10, (byte) 0);
                }
            }
        }

        public void updateValues(byte[][] data, int pos, Reminder reminder) {
            final byte[] message = reminder.getMessage().getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(message, 0, data[2*pos], 2, Math.min(message.length, 18));
            Arrays.fill(data[2*pos], 2+message.length, 20, (byte) 0);

            LocalDate start = LocalDate.ofInstant(reminder.getDate().toInstant(), ZoneId.of("UTC"));
            if (start.getYear() < 2000) {
                if (reminder.getRepetition() == Reminder.EVERY_WEEK) {
                    final LocalDate first = LocalDate.of(2000, 1, 1);
                    start = first.plusDays((start.getDayOfWeek().getValue()-first.getDayOfWeek().getValue())%7);
                } else {
                    start = start.withYear(2000);
                }
            } else if (start.getYear() > 2099) {
                if (reminder.getRepetition() == Reminder.EVERY_WEEK) {
                    final LocalDate last = LocalDate.of(2099, 31, 12);
                    start = last.minusDays((last.getDayOfWeek().getValue()-start.getDayOfWeek().getValue())%7);
                } else {
                    final int dayOfMonth = start.getDayOfMonth();
                    start = start.withYear(2099);
                    if (start.getDayOfMonth() != dayOfMonth) {
                        start = start.withYear(2096); // leap-year
                    }
                }
            }

            byte mode = 0;
            LocalDate end = start;
            byte weekdays = 0;

            switch (reminder.getRepetition()) {
            case Reminder.EVERY_DAY:
                mode = 0x0B;
                end = LocalDate.of(2099,12,31);
                break;
            case Reminder.EVERY_WEEK:
                mode = 0x05;
                weekdays = (byte) (1 << (start.getDayOfWeek().getValue()%7)); // Sunday is 1<<0
                break;
            case Reminder.EVERY_MONTH:
                mode = 0x11;
                break;
            case Reminder.EVERY_YEAR:
                mode = 0x09;
                break;
            default:
                mode = 0x01;
            }

            data[2*pos+1][2] = mode;
            data[2*pos+1][3] = BcdUtil.toBcd8(start.getYear()-2000);
            data[2*pos+1][4] = BcdUtil.toBcd8(start.getMonthValue());
            data[2*pos+1][5] = BcdUtil.toBcd8(start.getDayOfMonth());
            data[2*pos+1][6] = BcdUtil.toBcd8(end.getYear()-2000);
            data[2*pos+1][7] = BcdUtil.toBcd8(end.getMonthValue());
            data[2*pos+1][8] = BcdUtil.toBcd8(end.getDayOfMonth());
            data[2*pos+1][9] = weekdays;
        }

        @Override
        public final boolean mergeValues(byte[][] data, byte[][] previous, SharedPreferences.Editor editor) {
            List<? extends Reminder> reminders = DBHelper.getReminders(gbDevice);
            // always overwrite watch values
            return mergeValues(data, previous, uData -> updateValues(uData, reminders), rData -> false);
        }
    }

    @Override
    public void onSetReminders(ArrayList<? extends Reminder> reminders) {
        deviceReminders.onSet(reminders);
    }

    public abstract class DevicePreference extends DeviceSetting {
        byte feature;
        public final FeatureRequest getFeatureRequest() {
            return new FeatureRequest(feature);
        };

        @Override
        public final FeatureRequest[] getFeatureRequests() {
            return new FeatureRequest[] {getFeatureRequest()};
        };

        String name;
        public final String getName() {
            return name;
        }

        public abstract void updateValue(byte[] data);

        @Override
        public final boolean mergeValues(byte[][] data, byte[][] previous, SharedPreferences.Editor editor) {
            return mergeValues(data, previous, uData -> updateValue(uData[0]), rData -> readValue(rData[0], editor));
        }

        public abstract boolean readValue(byte[] data, SharedPreferences.Editor editor);

    };

    public abstract Casio2C2DSupport.DevicePreference[] supportedDevicePreferences();

    @Override
    public void onSendConfiguration(String config) {
        DevicePreference pref = devicePreferenceByName.get(config);
        if (pref == null) {
            LOG.warn("received configuration change for unsupported setting " + config);
            return;
        }
        if (!isInitialized()) {
            return;
        }
        byte[] currentValue = featureCache.get(pref.getFeatureRequest());
        if (currentValue == null) {
            LOG.error("unknown current watch value for config " + config);
            return;
        }
        pref.updateValue(currentValue);

        SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
        featureCache.save(editor);
        editor.apply();

        sendSettingsToDevice(new byte[][] {currentValue});
    }

    public class UnsignedByteDevicePreference extends DevicePreference {
        int index;

        @Override
        public void updateValue(byte[] data) {
            data[index] = (byte) getDevicePrefsValue();
        }

        @Override
        public boolean readValue(byte[] data, SharedPreferences.Editor editor) {
            return setDevicePrefsValue(editor, data[index] & 0xff);
        }

        public int getDevicePrefsValue() {
            return getDevicePrefs().getInt(name, -1);
        }

        public boolean setDevicePrefsValue(SharedPreferences.Editor editor, int value) {
            if (value != getDevicePrefsValue()) {
                editor.putString(name, Integer.toString(value));
            }
            return false;
        }
    }

    public class BoolDevicePreference extends DevicePreference {
        int index;
        byte mask;

        @Override
        public void updateValue(byte[] data) {
            if (getDevicePrefsValue()) {
                data[index] &= ~mask;
            } else {
                data[index] |= mask;
            }
        }

        @Override
        public boolean readValue(byte[] data, SharedPreferences.Editor editor) {
            if ((data[index] & mask) == 0) {
                return setDevicePrefsValue(editor, true);
            } else {
                return setDevicePrefsValue(editor, false);
            }
        }

        public boolean getDevicePrefsValue() {
            return getDevicePrefs().getBoolean(name, false);
        }

        public boolean setDevicePrefsValue(SharedPreferences.Editor editor, boolean value) {
            if (value != getDevicePrefsValue()) {
                editor.putBoolean(name, value);
            }
            return false;
        }
    }

    public class InvertedBoolDevicePreference extends BoolDevicePreference {
        @Override
        public void updateValue(byte[] data) {
            if (getDevicePrefsValue()) {
                data[index] |= mask;
            } else {
                data[index] &= ~mask;
            }
        }

        @Override
        public boolean readValue(byte[] data, SharedPreferences.Editor editor) {
            if ((data[index] & mask) == 0) {
                return setDevicePrefsValue(editor, false);
            } else {
                return setDevicePrefsValue(editor, true);
            }
        }
    }

    interface AutoGetter {
        public String get(DevicePrefs devicePrefs);
    }

    public class AutoBoolDevicePreference extends BoolDevicePreference {
        AutoGetter getter;

        String autoValue;
        String trueValue;
        String falseValue;

        @Override
        public boolean getDevicePrefsValue() {
            return getter.get(getDevicePrefs()).equals(trueValue);
        }

        @Override
        public boolean setDevicePrefsValue(SharedPreferences.Editor editor, boolean value) {
            String strValue = value ? trueValue : falseValue;
            if (!getter.get(getDevicePrefs()).equals(strValue)) {
                if (getDevicePrefs().getString(name, autoValue).equals(autoValue)) {
                    return true;
                } else {
                    editor.putString(name, strValue);
                }
            }
            return false;
        }
    }

    public class TimeSyncPreference extends BoolDevicePreference {
        { name = PREF_TIME_SYNC; }
        { feature = FEATURE_SETTING_FOR_BLE; }
        { index = 12; mask = (byte) (0x80 & 0xff); }
    }

    public class ConnectionDurationPreference extends UnsignedByteDevicePreference {
        { name = PREF_CONNECTION_DURATION; }
        { feature = FEATURE_SETTING_FOR_BLE; }
        { index = 14; }

        @Override
        public int getDevicePrefsValue() {
            return getDevicePrefs().getInt(name, -1);
        }

        @Override
        public boolean setDevicePrefsValue(SharedPreferences.Editor editor, int value) {
            if (value != getDevicePrefsValue()) {
                editor.putString(name, Integer.toString(value));
            }
            return false;
        }
    }

    public class TimeFormatPreference extends AutoBoolDevicePreference {
        { name = PREF_TIMEFORMAT; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 1; mask = 0x01; }
        { getter = gbPrefs -> gbPrefs.getTimeFormat(); }
        { autoValue = PREF_TIMEFORMAT_AUTO; }
        { trueValue = PREF_TIMEFORMAT_12H; }
        { falseValue = PREF_TIMEFORMAT_24H; }
    }

    public class OperatingSoundPreference extends BoolDevicePreference {
        { name = PREF_OPERATING_SOUNDS; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 1; mask = 0x02; }
    }

    public class AutoLightPreference extends BoolDevicePreference {
        { name = PREF_AUTOLIGHT; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 1; mask = 0x04; }
    }

    public class PowerSavingPreference extends BoolDevicePreference {
        { name = PREF_POWER_SAVING; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 1; mask = 0x10; }
    }

    public class LongerLightDurationPreference extends InvertedBoolDevicePreference {
        { name = PREF_LIGHT_DURATION_LONGER; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 2; mask = 0x01; }
    }

    public class DayMonthOrderPreference extends AutoBoolDevicePreference {
        { name = PREF_DATEFORMAT; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 4; mask = 0x01; }
        { getter = gbPrefs -> gbPrefs.getDateFormatDayMonthOrder(); }
        { autoValue = PREF_DATEFORMAT_AUTO; }
        { trueValue = PREF_DATEFORMAT_MONTH_DAY; }
        { falseValue = PREF_DATEFORMAT_DAY_MONTH; }
    }

    public class LanguagePreference extends UnsignedByteDevicePreference {
        { name = PREF_LANGUAGE; }
        { feature = FEATURE_SETTING_FOR_BASIC; }
        { index = 5; }

        String[] languages = { "en_US", "es_ES", "fr_FR"," de_DE", "it_IT", "ru_RU" };

        @Override
        public int getDevicePrefsValue() {
            String value = getDevicePrefs().getString(name, PREF_LANGUAGE_AUTO);
            int number = 0;
            if (value.equals(PREF_LANGUAGE_AUTO)) {
                String lang = Locale.getDefault().getLanguage() + "_";
                for (int i=0; i < languages.length; i++) {
                    if (languages[i].startsWith(lang)) {
                        number = i;
                        break;
                    }
                }
            } else {
                for (int i=0; i < languages.length; i++) {
                    if (value.equals(languages[i])) {
                        number = i;
                        break;
                    }
                }
            }
            return number;
        }

        @Override
        public boolean setDevicePrefsValue(SharedPreferences.Editor editor, int value) {
            if (getDevicePrefsValue() != value) {
                if (getDevicePrefs().getString(name, PREF_LANGUAGE_AUTO).equals(PREF_LANGUAGE_AUTO)) {
                    return true;
                } else {
                    if (value < languages.length) {
                        editor.putString(name, languages[value]);
                    } else {
                        editor.putString(name, "unknown");
                    }
                }
            }
            return false;
        }
    }

    public class HourlyChimePreference extends InvertedBoolDevicePreference {
        { name = PREF_HOURLY_CHIME_ENABLE; }
        { feature = FEATURE_SETTING_FOR_ALM; }
        { index = 1; mask = (byte) (0x80 & 0xff); }
    }

}
