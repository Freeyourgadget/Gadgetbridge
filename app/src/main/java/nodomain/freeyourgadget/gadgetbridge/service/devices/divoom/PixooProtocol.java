/*  Copyright (C) 2023 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.divoom;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

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

        return devEvts.toArray(new GBDeviceEvent[0]);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS:
                byte brightness = (byte) prefs.getInt(DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS, 50);
                LOG.debug("setting brightness to " + brightness);
                return encodeProtocol(new byte[]{
                        0x74,
                        brightness
                });
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                final GBPrefs gbPrefs = new GBPrefs(new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress())));
                final String timeFormat = gbPrefs.getTimeFormat();
                final boolean is24hour = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H.equals(timeFormat);

                return encodeProtocol(new byte[]{
                        0x2d,
                        (byte) (is24hour ? 1 : 0),
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

    byte[] encodeProtocol(byte[] payload) {

        ByteBuffer msgBuf = ByteBuffer.allocate(6 + payload.length);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put((byte) 0x01);
        msgBuf.putShort((short) (payload.length + 2));
        msgBuf.put(payload);
        short crc = (short) (((payload.length + 2) & 0xff) + ((payload.length + 2) >> 8));
        for (byte b : payload) {
            crc += b;
        }
        msgBuf.putShort(crc);
        msgBuf.put((byte) 0x02);
        return msgBuf.array();
    }

}
