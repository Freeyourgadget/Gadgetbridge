/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CmfWatchProSupport extends AbstractBTLEDeviceSupport implements CmfCharacteristic.Handler {
    private static final Logger LOG = LoggerFactory.getLogger(CmfWatchProSupport.class);

    public static final UUID UUID_SERVICE_CMF_CMD = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CMF_COMMAND_READ = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CMF_COMMAND_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_SERVICE_CMF_DATA = UUID.fromString("02f00000-0000-0000-0000-00000000ffe0");
    public static final UUID UUID_CHARACTERISTIC_CMF_DATA_WRITE = UUID.fromString("02f00000-0000-0000-0000-00000000ffe1");
    public static final UUID UUID_CHARACTERISTIC_CMF_DATA_READ = UUID.fromString("02f00000-0000-0000-0000-00000000ffe2");

    // An a5 byte is used a lot in single payloads, probably as a "proof of encryption"?
    public static final byte A5 = (byte) 0xa5;

    private CmfCharacteristic characteristicCommandRead;
    private CmfCharacteristic characteristicCommandWrite;
    private CmfCharacteristic characteristicDataRead;
    private CmfCharacteristic characteristicDataWrite;

    private final CmfActivitySync activitySync = new CmfActivitySync(this);
    private final CmfPreferences preferences = new CmfPreferences(this);
    private CmfDataUploader dataUploader;

    protected MediaManager mediaManager = null;

    public CmfWatchProSupport() {
        super(LOG);
        addSupportedService(UUID_SERVICE_CMF_CMD);
        addSupportedService(UUID_SERVICE_CMF_DATA);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return false;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        final BluetoothGattCharacteristic btCharacteristicCommandRead = getCharacteristic(UUID_CHARACTERISTIC_CMF_COMMAND_READ);
        if (btCharacteristicCommandRead == null) {
            LOG.warn("Characteristic command read is null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicCommandWrite = getCharacteristic(UUID_CHARACTERISTIC_CMF_COMMAND_WRITE);
        if (btCharacteristicCommandWrite == null) {
            LOG.warn("Characteristic command write is null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicDataWrite = getCharacteristic(UUID_CHARACTERISTIC_CMF_DATA_WRITE);
        if (btCharacteristicDataWrite == null) {
            LOG.warn("Characteristic data write is null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicDataRead = getCharacteristic(UUID_CHARACTERISTIC_CMF_DATA_READ);
        if (btCharacteristicDataRead == null) {
            LOG.warn("Characteristic data read is null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        dataUploader = new CmfDataUploader(this);

        characteristicCommandRead = new CmfCharacteristic(btCharacteristicCommandRead, this);
        characteristicCommandWrite = new CmfCharacteristic(btCharacteristicCommandWrite, null);
        characteristicDataRead = new CmfCharacteristic(btCharacteristicDataRead, dataUploader);
        characteristicDataWrite = new CmfCharacteristic(btCharacteristicDataWrite, null);

        final byte[] secretKey = getSecretKey(getDevice());
        characteristicCommandRead.setSessionKey(secretKey);
        characteristicCommandWrite.setSessionKey(secretKey);
        characteristicDataRead.setSessionKey(secretKey);
        characteristicDataWrite.setSessionKey(secretKey);

        builder.notify(btCharacteristicCommandWrite, true);
        builder.notify(btCharacteristicCommandRead, true);
        builder.notify(btCharacteristicDataWrite, true);
        builder.notify(btCharacteristicDataRead, true);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.AUTHENTICATING, getContext()));

        sendCommand(builder, CmfCommand.AUTH_PHONE_NAME, ArrayUtils.addAll(new byte[]{A5}, Build.MODEL.getBytes(StandardCharsets.UTF_8)));

        return builder;
    }

    @Override
    public void setContext(final GBDevice device, final BluetoothAdapter adapter, final Context context) {
        super.setContext(device, adapter, context);

        mediaManager = new MediaManager(context);
    }

    @Override
    protected Prefs getDevicePrefs() {
        return super.getDevicePrefs();
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();
        final byte[] value = characteristic.getValue();

        if (characteristicUUID.equals(characteristicCommandRead.getCharacteristicUUID())) {
            characteristicCommandRead.onCharacteristicChanged(value);
            return true;
        } else if (characteristicUUID.equals(characteristicDataRead.getCharacteristicUUID())) {
            characteristicDataRead.onCharacteristicChanged(value);
            return true;
        }

        LOG.warn("Unhandled characteristic changed: {} {}", characteristicUUID, GB.hexdump(value));
        return false;
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        super.onMtuChanged(gatt, mtu, status);

        characteristicCommandRead.setMtu(mtu);
        characteristicCommandWrite.setMtu(mtu);
        characteristicDataRead.setMtu(mtu);
        characteristicDataWrite.setMtu(mtu);
    }

    @Override
    public void onCommand(final CmfCommand cmd, final byte[] payload) {
        if (activitySync.onCommand(cmd, payload)) {
            return;
        }

        if (preferences.onCommand(cmd, payload)) {
            return;
        }

        switch (cmd) {
            case AUTH_WATCH_MAC:
                LOG.debug("Got auth watch mac, requesting nonce");
                sendCommand("auth request nonce", CmfCommand.AUTH_NONCE_REQUEST, A5);
                return;
            case AUTH_NONCE_REPLY:
                LOG.debug("Got auth nonce");

                try {
                    final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    sha256.update(payload);
                    sha256.update(getSecretKey(getDevice()));
                    final byte[] digest = sha256.digest();
                    final byte[] sessionKey = ArrayUtils.subarray(digest, 0, 16);
                    LOG.debug("New session key: {}", GB.hexdump(sessionKey));
                    characteristicCommandRead.setSessionKey(sessionKey);
                    characteristicCommandWrite.setSessionKey(sessionKey);
                    characteristicDataRead.setSessionKey(sessionKey);
                    characteristicDataWrite.setSessionKey(sessionKey);
                } catch (final GeneralSecurityException e) {
                    LOG.error("Failed to compute session key from auth nonce", e);
                    return;
                }

                sendCommand("auth confirm", CmfCommand.AUTHENTICATED_CONFIRM_REQUEST, A5);
                return;
            case AUTHENTICATED_CONFIRM_REPLY:
                LOG.debug("Authentication confirmed, starting phase 2 initialization");

                final TransactionBuilder phase2builder = createTransactionBuilder("phase 2 initialize");
                setTime(phase2builder);
                sendCommand(phase2builder, CmfCommand.FIRMWARE_VERSION_GET);
                sendCommand(phase2builder, CmfCommand.SERIAL_NUMBER_GET);
                //sendCommand(phase2builder, CmfCommand.STANDING_REMINDER_GET);
                //sendCommand(phase2builder, CmfCommand.WATER_REMINDER_GET);
                //sendCommand(phase2builder, CmfCommand.CONTACTS_GET);
                //sendCommand(phase2builder, CmfCommand.ALARMS_GET);
                // TODO premature to mark as initialized?
                phase2builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
                phase2builder.queue(getQueue());
                return;
            case BATTERY:
                final int battery = payload[0] & 0xff;
                final boolean charging = payload[1] == 0x01;
                LOG.debug("Got battery: level={} charging={}", battery, charging);
                final GBDeviceEventBatteryInfo eventBatteryInfo = new GBDeviceEventBatteryInfo();
                eventBatteryInfo.level = battery;
                eventBatteryInfo.state = charging ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;
                evaluateGBDeviceEvent(eventBatteryInfo);
                return;
            case FIRMWARE_VERSION_RET:
                final String[] fwParts = new String[payload.length];
                for (int i = 0; i < payload.length; i++) {
                    fwParts[i] = String.valueOf(payload[i]);
                }
                final String fw = String.join(".", fwParts);
                LOG.debug("Got firmware version: {}", fw);
                final GBDeviceEventVersionInfo gbDeviceEventVersionInfo = new GBDeviceEventVersionInfo();
                gbDeviceEventVersionInfo.fwVersion = fw;
                gbDeviceEventVersionInfo.fwVersion2 = "N/A";
                //gbDeviceEventVersionInfo.hwVersion = "?"; // TODO how?
                evaluateGBDeviceEvent(gbDeviceEventVersionInfo);
                return;
            case SERIAL_NUMBER_RET:
                if (payload.length != (payload[0] & 0xff) + 1) {
                    LOG.warn("Unexpected serial number payload length: {}, expected {}", payload.length, (payload[0] & 0xff));
                    return;
                }
                final String serialNumber = new String(ArrayUtils.subarray(payload, 1, payload.length - 2));
                LOG.debug("Got serial number: {}", serialNumber);
                final GBDeviceEventUpdateDeviceInfo gbDeviceEventUpdateDeviceInfo = new GBDeviceEventUpdateDeviceInfo("SERIAL: ", serialNumber);
                evaluateGBDeviceEvent(gbDeviceEventUpdateDeviceInfo);
                return;
            case FIND_PHONE:
                final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                if (payload[0] == 1) {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                } else {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                }
                evaluateGBDeviceEvent(findPhoneEvent);
                return;
            case MUSIC_INFO_ACK:
                LOG.debug("Got music info ack");
                break;
            case MUSIC_BUTTON:
                final GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                switch (BLETypeConversions.toUint16(payload)) {
                    case 0x0003:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    case 0x0103:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case 0x0001:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case 0x0101:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case 0x0102:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case 0x0002:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    default:
                        LOG.warn("Unexpected media button key {}", GB.hexdump(payload));
                        return;
                }
                LOG.debug("Got media button {}", deviceEventMusicControl.event);
                evaluateGBDeviceEvent(deviceEventMusicControl);
                break;
            default:
                LOG.warn("Unhandled command: {}", cmd);
        }
    }

    public void sendCommand(final String taskName, final CmfCommand cmd, final byte... payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        sendCommand(builder, cmd, payload);
        builder.queue(getQueue());
    }

    public void sendCommand(final TransactionBuilder builder, final CmfCommand cmd, final byte... payload) {
        characteristicCommandWrite.sendCommand(builder, cmd, payload);
    }

    public void sendData(final String taskName, final CmfCommand cmd, final byte... payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        characteristicDataWrite.sendCommand(builder, cmd, payload);
        builder.queue(getQueue());
    }

    private static byte[] getSecretKey(final GBDevice device) {
        final byte[] authKeyBytes = new byte[16];

        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        final String authKey = sharedPrefs.getString("authkey", "").trim();
        if (StringUtils.isNotBlank(authKey)) {
            final byte[] srcBytes;
            // Allow both with and without 0x, to avoid user mistakes
            if (authKey.length() == 34 && authKey.startsWith("0x")) {
                srcBytes = GB.hexStringToByteArray(authKey.trim().substring(2));
            } else {
                srcBytes = GB.hexStringToByteArray(authKey.trim());
            }
            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring");
            return;
        }

        final String senderOrTitle = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(
                notificationSpec.sender,
                notificationSpec.title
        );

        final String body = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(notificationSpec.body, "");

        final byte[] senderOrTitleBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(senderOrTitle, 20); // TODO confirm max
        final byte[] bodyBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(body, 128); // TODO confirm max

        final ByteBuffer buf = ByteBuffer.allocate(7 + senderOrTitleBytes.length + bodyBytes.length)
                .order(ByteOrder.BIG_ENDIAN);

        buf.put(CmfNotificationIcon.forNotification(notificationSpec).getCode());
        buf.put((byte) 0x00); // ?
        buf.putInt((int) (notificationSpec.when / 1000));
        buf.put((byte) senderOrTitleBytes.length);
        buf.put(senderOrTitleBytes);
        buf.put(bodyBytes);

        sendCommand("send notification", CmfCommand.APP_NOTIFICATION, buf.array());
    }

    @Override
    public void onSetContacts(final ArrayList<? extends Contact> contacts) {
        final ByteBuffer buf = ByteBuffer.allocate(57 * contacts.size()).order(ByteOrder.BIG_ENDIAN);

        for (final Contact contact : contacts) {
            final byte[] nameBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(contact.getName(), 32);
            buf.put(nameBytes);
            buf.put(new byte[32 - nameBytes.length]);

            final byte[] numberBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(contact.getNumber(), 25);
            buf.put(numberBytes);
            buf.put(new byte[25 - numberBytes.length]);
        }

        sendCommand("set contacts", CmfCommand.CONTACTS_SET, ArrayUtils.subarray(buf.array(), 0, buf.position()));
    }

    @Override
    public void onSetTime() {
        final TransactionBuilder builder = createTransactionBuilder("set time");
        setTime(builder);
        builder.queue(getQueue());
    }

    private void setTime(final TransactionBuilder builder) {
        final Calendar cal = Calendar.getInstance();
        final ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        buf.putInt((int) (cal.getTimeInMillis() / 1000));
        buf.putInt(TimeZone.getDefault().getOffset(cal.getTimeInMillis()));
        sendCommand(builder, CmfCommand.TIME, buf.array());
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        final ByteBuffer buf = ByteBuffer.allocate(40 * alarms.size()).order(ByteOrder.BIG_ENDIAN);

        int i = 0;
        for (final Alarm alarm : alarms) {
            if (alarm.getUnused()) {
                continue;
            }

            buf.putInt(alarm.getHour() * 3600 + alarm.getMinute() * 60);
            buf.put((byte) i++);
            buf.put((byte) (alarm.getEnabled() ? 0x01 : 0x00));
            buf.put((byte) alarm.getRepetition());
            buf.put((byte) 0xff); // ?
            buf.put(new byte[24]); // ?

            // alarm labels do not show up on watch, even in official app
            final byte[] labelBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(alarm.getTitle(), 8);
            buf.put(new byte[8 - labelBytes.length]);
            buf.put(labelBytes);
        }

        sendCommand("set alarms", CmfCommand.ALARMS_SET, ArrayUtils.subarray(buf.array(), 0, buf.position()));
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        super.onSetCallState(callSpec); // TODO onSetCallState
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        super.onSetCannedMessages(cannedMessagesSpec); // TODO onSetCannedMessages
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        if (mediaManager.onSetMusicState(stateSpec)) {
            sendMusicStateToDevice();
        }
    }

    @Override
    public void onSetPhoneVolume(final float ignoredVolume) {
        sendMusicStateToDevice();
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        if (mediaManager.onSetMusicInfo(musicSpec)) {
            sendMusicStateToDevice();
        }
    }

    private void sendMusicStateToDevice() {
        final MusicSpec musicSpec = mediaManager.getBufferMusicSpec();
        final MusicStateSpec musicStateSpec = mediaManager.getBufferMusicStateSpec();

        final byte stateByte;
        if (musicSpec == null || musicStateSpec == null) {
            stateByte = 0x00;
        } else if (musicStateSpec.state == MusicStateSpec.STATE_PLAYING) {
            stateByte = 0x01;
        } else {
            stateByte = 0x02;
        }

        final byte[] track;
        final byte[] artist;

        if (musicSpec != null) {
            track = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(musicSpec.track, 63);
            artist = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(musicSpec.artist, 63);
        } else {
            track = new byte[0];
            artist = new byte[0];
        }

        final AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        final int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        final ByteBuffer buf = ByteBuffer.allocate(131);
        buf.put(stateByte);
        buf.put((byte) volumeLevel);
        buf.put((byte) volumeMax);
        buf.put(track);
        buf.put(new byte[64 - track.length]);
        buf.put(artist);
        buf.put(new byte[64 - artist.length]);

        sendCommand("set music info", CmfCommand.MUSIC_INFO_SET, buf.array());
    }

    @Override
    public void onInstallApp(final Uri uri) {
        dataUploader.onInstallApp(uri);
    }

    @Override
    public void onAppInfoReq() {
        super.onAppInfoReq(); // TODO onAppInfoReq
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        super.onAppStart(uuid, start); // TODO onAppStart for watchfaces
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        sendCommand("fetch recorded data step 1", CmfCommand.ACTIVITY_FETCH_1, A5);
    }

    @Override
    public void onReset(final int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
            sendCommand("factory reset", CmfCommand.FACTORY_RESET, A5);
        } else {
            LOG.warn("Unknown reset flags: {}", String.format("0x%x", flags));
        }
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        preferences.onSetHeartRateMeasurementInterval(seconds);
    }

    @Override
    public void onSendConfiguration(final String config) {
        preferences.onSendConfiguration(config);
    }

    @Override
    public void onFindDevice(final boolean start) {
        if (!start) {
            return;
        }

        sendCommand("find device", CmfCommand.FIND_WATCH);
    }

    @Override
    public void onSendWeather(final WeatherSpec weatherSpec) {
        // TODO onSendWeather
    }

    @Override
    public void onTestNewFunction() {

    }
}
