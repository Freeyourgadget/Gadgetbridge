/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.divoom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class PixooProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(PixooProtocol.class);

    private boolean isFirstExchange = true;

    protected PixooProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        List<GBDeviceEvent> devEvts = new ArrayList<>();

        if (isFirstExchange) {
            isFirstExchange = false;
            devEvts.add(new GBDeviceEventVersionInfo()); //TODO: this is a weird hack to make the DBHelper happy. Replace with proper firmware detection
        }

        ByteBuffer incoming = ByteBuffer.wrap(responseData);
        incoming.order(ByteOrder.LITTLE_ENDIAN);
        if (incoming.get() != 0x01) {
            LOG.warn("first byte not 0x01");
            return devEvts.toArray(new GBDeviceEvent[0]);
        }
        int length = incoming.getShort() & 0xffff;
        byte status = incoming.get(); // unsure
        if (status != 0x04) {
            LOG.warn("status byte not 0x04");
            return devEvts.toArray(new GBDeviceEvent[0]);
        }
        byte endpoint = incoming.get(); // unsure
        LOG.info("endpoint " + endpoint);
        if (endpoint == 0x42) {
            decodeAlarms(incoming);
        }
        return devEvts.toArray(new GBDeviceEvent[0]);
    }

    private void decodeAlarms(ByteBuffer incoming) {
        byte unknown = incoming.get();
        if (unknown != 0x55) { // expected
            LOG.warn("unexpected byte when decoding Alarms " + unknown);
            return;
        }
        // Map of alarm position to Alarm, as returned by the band
        final Map<Integer, nodomain.freeyourgadget.gadgetbridge.model.Alarm> payloadAlarms = new HashMap<>();

        while (incoming.remaining() > 10) {
            int position = incoming.get();
            boolean enabled = incoming.get() == 1;
            int hour = incoming.get();
            int minute = incoming.get();
            int repeatMask = incoming.get();
            int unknown2 = incoming.getInt();
            byte unknown3 = incoming.get(); // normally 0x01, on fresh alarms 0x32
            final Alarm alarm = new nodomain.freeyourgadget.gadgetbridge.entities.Alarm();
            alarm.setEnabled(enabled);
            alarm.setPosition(position);
            alarm.setHour(hour);
            alarm.setMinute(minute);
            alarm.setRepetition(repeatMask);
            alarm.setUnused(unknown3 == 0x32 && !enabled);
            payloadAlarms.put(position, alarm);
        }
        final List<nodomain.freeyourgadget.gadgetbridge.entities.Alarm> dbAlarms = DBHelper.getAlarms(getDevice());
        int numUpdatedAlarms = 0;

        for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm : dbAlarms) {
            final int pos = alarm.getPosition();
            final nodomain.freeyourgadget.gadgetbridge.model.Alarm updatedAlarm = payloadAlarms.get(pos);
            final boolean alarmNeedsUpdate = updatedAlarm == null ||
                    alarm.getUnused() != updatedAlarm.getUnused() ||
                    alarm.getEnabled() != updatedAlarm.getEnabled() ||
                    alarm.getSmartWakeup() != updatedAlarm.getSmartWakeup() ||
                    alarm.getHour() != updatedAlarm.getHour() ||
                    alarm.getMinute() != updatedAlarm.getMinute() ||
                    alarm.getRepetition() != updatedAlarm.getRepetition();

            if (alarmNeedsUpdate) {
                numUpdatedAlarms++;
                LOG.info("Updating alarm index={}, unused={}", pos, updatedAlarm == null);
                alarm.setUnused(updatedAlarm == null);
                if (updatedAlarm != null) {
                    alarm.setEnabled(updatedAlarm.getEnabled());
                    alarm.setUnused(updatedAlarm.getUnused());
                    alarm.setSmartWakeup(updatedAlarm.getSmartWakeup());
                    alarm.setHour(updatedAlarm.getHour());
                    alarm.setMinute(updatedAlarm.getMinute());
                    alarm.setRepetition(updatedAlarm.getRepetition());
                }
                DBHelper.store(alarm);
            }
        }

        if (numUpdatedAlarms > 0) {
            final Intent intent = new Intent(DeviceService.ACTION_SAVE_ALARMS);
            LocalBroadcastManager.getInstance(GBApplication.getContext()).sendBroadcast(intent);
        }
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_DEVICE_NAME:
                final byte[] deviceName = StringUtils.truncateToBytes(prefs.getString(DeviceSettingsPreferenceConst.PREF_DEVICE_NAME, ""), 26);
                return encodeProtocol(ByteBuffer.allocate(2 + deviceName.length)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .put((byte) 0x75)
                        .put((byte) deviceName.length)
                        .put(deviceName)
                        .array());
            case DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS:
                byte brightness = (byte) prefs.getInt(DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS, 50);
                LOG.debug("setting brightness to " + brightness);
                return encodeProtocol(new byte[]{
                        0x74,
                        brightness
                });
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                final String timeFormat = getDevicePrefs().getTimeFormat();
                final boolean is24hour = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H.equals(timeFormat);

                return encodeProtocol(new byte[]{
                        0x2d,
                        (byte) (is24hour ? 1 : 0),
                });
            case DeviceSettingsPreferenceConst.PREF_CLAP_HANDS_TO_WAKEUP_DEVICE:
                boolean clap = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CLAP_HANDS_TO_WAKEUP_DEVICE, false);

                return encodeProtocol(new byte[]{
                        (byte) 0xa7,
                        (byte) (clap ? 1 : 0),
                });
            case DeviceSettingsPreferenceConst.PREF_POWER_SAVING:
                boolean power_saving = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_POWER_SAVING, false);

                return encodeProtocol(new byte[]{
                        (byte) 0xb2,
                        (byte) (power_saving ? 1 : 0),
                });

        }

        return super.encodeSendConfiguration(config);
    }

    @Override
    public byte[] encodeSetCallState(String number, String name, int command) {
        if (command == CallSpec.CALL_OUTGOING) {
            return null;
        }
        return encodeProtocol(new byte[]{
                0x50,
                (byte) (command == CallSpec.CALL_INCOMING ? 5 : 6),
        });
    }

    @Override
    public byte[] encodeNotification(NotificationSpec notificationSpec) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring");
            return null;
        }

        byte iconID;
        switch (notificationSpec.type) {
            case KAKAO_TALK:
                iconID = 0;
                break;
            case INSTAGRAM:
                iconID = 1;
                break;
            case SNAPCHAT:
                iconID = 2;
                break;
            case FACEBOOK:
                iconID = 3;
                break;
            case TWITTER:
                iconID = 4;
                break;
            case WHATSAPP:
            case GENERIC_SMS:
                iconID = 8;
                break;
            case SKYPE:
                iconID = 9;
                break;
            case LINE:
                iconID = 10;
                break;
            case WECHAT:
                iconID = 11;
                break;
            case VIBER:
                iconID = 12;
                break;
            case GENERIC_ALARM_CLOCK:
                iconID = 17;
                break;
            default:
                iconID = 0x20;
        }

        return encodeProtocol(new byte[]{
                0x50,
                iconID,
        });

    }

    @Override
    public byte[] encodeSetTime() {
        Calendar now = BLETypeConversions.createCalendar();
        return encodeProtocol(new byte[]{
                0x18,
                (byte) (now.get(Calendar.YEAR) % 100),
                (byte) (now.get(Calendar.YEAR) / 100),
                (byte) (now.get(Calendar.MONTH) + 1),
                (byte) now.get(Calendar.DAY_OF_MONTH),
                (byte) now.get(Calendar.HOUR_OF_DAY),
                (byte) now.get(Calendar.MINUTE),
                (byte) now.get(Calendar.SECOND)
        });

    }

    @Override
    public byte[] encodeSetAlarms(ArrayList<? extends nodomain.freeyourgadget.gadgetbridge.model.Alarm> alarms) {
        byte[] complete_command = new byte[]{};
        for (nodomain.freeyourgadget.gadgetbridge.model.Alarm alarm : alarms) {
            byte[] cmd = new byte[]{
                    0x43,
                    (byte) alarm.getPosition(),
                    (byte) (alarm.getEnabled() && !alarm.getUnused() ? 1 : 0),
                    (byte) alarm.getHour(),
                    (byte) alarm.getMinute(),
                    (byte) alarm.getRepetition(),
                    0, 0, 0, 0,
                    (byte) (alarm.getUnused() ? 0x32 : 0x00)};

            complete_command = ArrayUtils.addAll(complete_command, encodeProtocol(cmd));
        }
        return complete_command;
    }


    @Override
    public byte[] encodeSendWeather(WeatherSpec weatherSpec) {
        byte pixooWeatherCode = 0;
        if (weatherSpec.currentConditionCode >= 200 && weatherSpec.currentConditionCode <= 299) {
            pixooWeatherCode = 5;
        } else if (weatherSpec.currentConditionCode >= 300 && weatherSpec.currentConditionCode < 600) {
            pixooWeatherCode = 6;
        } else if (weatherSpec.currentConditionCode >= 600 && weatherSpec.currentConditionCode < 700) {
            pixooWeatherCode = 8;
        } else if (weatherSpec.currentConditionCode >= 700 && weatherSpec.currentConditionCode < 800) {
            pixooWeatherCode = 9;
        } else if (weatherSpec.currentConditionCode == 800) {
            pixooWeatherCode = 1;
        } else if (weatherSpec.currentConditionCode >= 801 && weatherSpec.currentConditionCode <= 804) {
            pixooWeatherCode = 3;
        }

        byte temp = (byte) (weatherSpec.currentTemp - 273);
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            temp = (byte) WeatherUtils.celsiusToFahrenheit(temp);
        }

        return encodeProtocol(new byte[]{
                0x5f,
                temp,
                pixooWeatherCode,
        });
    }

    private byte[] encodeDrawIcon(String packageName) {
        Drawable icon = NotificationUtils.getAppIcon(GBApplication.getContext(), packageName);
        if (icon == null) {
            LOG.warn("could not get icon for package: " + packageName);
            return null;
        }
        final Bitmap bmp = BitmapUtil.toBitmap(icon);

        return encodeShowFrame(bmp);
    }

    protected byte[] encodeShowFrame(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(GBApplication.getContext().getContentResolver(), uri);
            return encodeShowFrame(bitmap);
        } catch (IOException e) {
            LOG.error("could not decode Image",e);
        }
        return null;
    }

    private byte[] encodeShowFrame(Bitmap bitmap) {
        final Bitmap bmpResized = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmpResized);
        final Rect rect = new Rect(0, 0, 16, 16);
        canvas.drawBitmap(bitmap, null, rect, null);


        // construct palette with unique colors
        HashSet<Integer> palette = new HashSet<>();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pixel = bmpResized.getPixel(x, y);
                palette.add(pixel);
            }
        }
        // convert to lookup of index
        HashMap<Integer, Integer> paletteLookup = new HashMap<>();
        int index = 0;
        for (int color : palette) {
            paletteLookup.put(color, index++);
        }

        int paletteMaxIndex = palette.size() - 1;
        int bpp = 1;
        while ((paletteMaxIndex >>= 1) != 0) {
            bpp++;
        }
        LOG.info("got palette of {} colors, will need {}bpp", palette.size(), bpp);
        byte[] header = new byte[]{0x44, 0x00, 0x0a, 0x0a, 0x04, (byte) 0xaa, 0x53, 0x00, (byte) 0xf4, 0x01, 0x00, (byte) palette.size()};
        int pixels_size = (bpp * 16 * 16) / 8;
        byte[] pixels = new byte[pixels_size];
        ByteBuffer buf = ByteBuffer.allocate(palette.size() * 3 + header.length + pixels_size);
        buf.put(header);
        for (int color : palette) {
            buf.put((byte) ((color >> 16) & 0xff));
            buf.put((byte) ((color >> 8) & 0xff));
            buf.put((byte) (color & 0xff));
        }
        int bitposition = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pixel = bmpResized.getPixel(x, y);
                index = paletteLookup.get(pixel);
                int pos = bitposition / 8;
                int shift = bitposition % 8;
                pixels[pos] = (byte) (pixels[pos] | (byte) (index << shift));
                if (shift + bpp > 8) {
                    shift = -shift + 8;
                    pos++;
                    pixels[pos] = (byte) (pixels[pos] | (byte) (index >> shift));
                }
                bitposition += bpp;
            }
        }
        buf.put(pixels);

        return encodeProtocol(buf.array());
    }

    private byte[] encodeClockModeCommand(int clockMode, boolean showTime, boolean showWeather, boolean showTemperature, boolean showDate,
                                          int r, int g, int b) {

        r = Math.min(r, 127);
        g = Math.min(g, 127);
        b = Math.min(b, 127);

        return encodeProtocol(new byte[]{
                0x45,
                0x00,
                0x01, // unknown, can be 0 or 1
                (byte) clockMode,
                (byte) (showTime ? 0x01 : 0x00), // ignored it seems
                (byte) (showWeather ? 0x01 : 0x00),
                (byte) (showTemperature ? 0x01 : 0x00),
                (byte) (showDate ? 0x01 : 0x00),
                (byte) r, (byte) g, (byte) b
        });

    }

    private byte[] encodeAudioVisualisationModeCommand(int visualisationMode) {
        return encodeProtocol(new byte[]{
                0x45,
                0x04,
                (byte) visualisationMode,
        });
    }

    private byte[] encodeEffectModeCommand(int effectMode) {
        return encodeProtocol(new byte[]{
                0x45,
                0x03,
                (byte) effectMode,
        });
    }

    public byte[] encodeReqestAlarms() {
        return encodeProtocol(new byte[]{0x42});
    }

    @Override
    public byte[] encodeTestNewFunction() {
        //return encodeAudioModeCommand(1); // works
        //return encodeEffectModeCommand(5); // does nothing
        //return encodeClockModeCommand(0, true, true, false, true, 127, 127, 127); // works r,g,b up to 127
        return encodeDrawIcon("nodomain.freeyourgadget.gadgetbridge");
        //return encodeDrawIcon("com.benny.openlauncher");
    }

    byte[] encodeProtocol(byte[] payload) {

        ByteBuffer msgBuf = ByteBuffer.allocate(6 + payload.length);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put((byte) 0x01);
        msgBuf.putShort((short) (payload.length + 2));
        msgBuf.put(payload);
        short crc = (short) (((payload.length + 2) & 0xff) + ((payload.length + 2) >> 8));
        for (byte b : payload) {
            crc += (b & 0xff);
        }
        msgBuf.putShort(crc);
        msgBuf.put((byte) 0x02);
        return msgBuf.array();
    }

}

