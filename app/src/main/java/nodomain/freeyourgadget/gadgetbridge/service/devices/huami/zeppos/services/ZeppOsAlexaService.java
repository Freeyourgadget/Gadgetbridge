/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WATCHFACE;

import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsAlexaService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAlexaService.class);

    private static final short ENDPOINT = 0x0011;

    private static final byte CMD_START = 0x01;
    private static final byte CMD_END = 0x02;
    private static final byte CMD_START_ACK = 0x03;
    private static final byte CMD_VOICE_DATA = 0x05;
    private static final byte CMD_TRIGGERED = 0x06;
    private static final byte CMD_REPLY_COMPLEX = 0x08;
    private static final byte CMD_REPLY_SIMPLE = 0x09;
    private static final byte CMD_REPLY_VOICE = 0x0a;
    private static final byte CMD_REPLY_VOICE_MORE = 0x0b;
    private static final byte CMD_ERROR = 0x0f;
    private static final byte CMD_LANGUAGES_REQUEST = 0x10;
    private static final byte CMD_LANGUAGES_RESPONSE = 0x11;
    private static final byte CMD_SET_LANGUAGE = 0x12;
    private static final byte CMD_SET_LANGUAGE_ACK = 0x13;
    private static final byte CMD_CAPABILITIES_REQUEST = 0x20;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x21;

    private static final byte COMPLEX_REPLY_WEATHER = 0x01;
    private static final byte COMPLEX_REPLY_REMINDER = 0x02;
    private static final byte COMPLEX_REPLY_RICH_TEXT = 0x06;

    private static final byte ERROR_NO_INTERNET = 0x03;
    private static final byte ERROR_UNAUTHORIZED = 0x06;

    public static final String PREF_VERSION = "zepp_os_alexa_version";

    private final Handler handler = new Handler();

    final ByteArrayOutputStream voiceBuffer = new ByteArrayOutputStream();

    public ZeppOsAlexaService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_START:
                handleStart(payload);
                break;
            case CMD_END:
                handleEnd(payload);
                break;
            case CMD_VOICE_DATA:
                handleVoiceData(payload);
                break;
            case CMD_LANGUAGES_RESPONSE:
                handleLanguagesResponse(payload);
                break;
            case CMD_SET_LANGUAGE_ACK:
                LOG.info("Alexa set language ack, status = {}", payload[1]);
                break;
            case CMD_CAPABILITIES_RESPONSE:
                handleCapabilitiesResponse(payload);
                break;
            default:
                LOG.warn("Unexpected alexa byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE:
                final String alexaLanguage = prefs.getString(DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE, null);
                LOG.info("Setting alexa language to {}", alexaLanguage);
                setLanguage(alexaLanguage);
                return true;
            case "zepp_os_alexa_btn_trigger":
                GB.toast("Alexa cmd trigger", Toast.LENGTH_SHORT, GB.INFO);
                sendCmdTriggered();
                return true;
            case "zepp_os_alexa_btn_send_simple":
                GB.toast("Alexa simple reply", Toast.LENGTH_SHORT, GB.INFO);
                final String simpleText = prefs.getString("zepp_os_alexa_reply_text", null);
                sendReply(simpleText);
                return true;
            case "zepp_os_alexa_btn_send_complex":
                GB.toast("Alexa complex reply", Toast.LENGTH_SHORT, GB.INFO);
                final String title = prefs.getString("zepp_os_alexa_reply_title", null);
                final String subtitle = prefs.getString("zepp_os_alexa_reply_subtitle", null);
                final String text = prefs.getString("zepp_os_alexa_reply_text", null);
                sendReply(title, subtitle, text);
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        if (Huami2021Coordinator.experimentalFeatures(getSupport().getDevice())) {
            requestCapabilities(builder);
            requestLanguages(builder);
        }
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void requestLanguages(final TransactionBuilder builder) {
        write(builder, CMD_LANGUAGES_REQUEST);
    }

    public void sendReply(final String text) {
        LOG.debug("Sending alexa simple text reply '{}'", text);

        final byte[] textBytes = StringUtils.ensureNotNull(text).getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(textBytes.length + 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_REPLY_SIMPLE);
        buf.put(textBytes);
        buf.put((byte) 0);

        write("send simple text reply", buf.array());
    }

    public void sendReply(final String title, final String subtitle, final String text) {
        LOG.debug("Sending alexa complex text reply '{}', '{}', '{}'", title, subtitle, text);

        final byte[] titleBytes = StringUtils.ensureNotNull(title).getBytes(StandardCharsets.UTF_8);
        final byte[] subtitleBytes = StringUtils.ensureNotNull(subtitle).getBytes(StandardCharsets.UTF_8);
        final byte[] textBytes = StringUtils.ensureNotNull(text).getBytes(StandardCharsets.UTF_8);

        final int messageLength = titleBytes.length + subtitleBytes.length + textBytes.length + 3;

        final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 4 + messageLength)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_REPLY_COMPLEX);
        buf.putShort(COMPLEX_REPLY_RICH_TEXT);
        buf.putInt(messageLength);
        buf.put(titleBytes);
        buf.put((byte) 0);
        buf.put(subtitleBytes);
        buf.put((byte) 0);
        buf.put(textBytes);
        buf.put((byte) 0);

        write("send complex text reply", buf.array());
    }

    public void sendReply(final WeatherSpec weather) {
        // TODO finish this
        if (true) {
            LOG.warn("Reply with weather not fully implemented");
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(0xfd); // ?
            baos.write(0x03); // ?
            baos.write(0x00); // ?
            baos.write(0x00); // ?

            baos.write(BLETypeConversions.fromUint32(weather.timestamp));

            baos.write(StringUtils.ensureNotNull(weather.location).getBytes(StandardCharsets.UTF_8));
            baos.write(0);

            // FIXME long date string
            baos.write(0);

            baos.write(StringUtils.ensureNotNull(weather.currentCondition).getBytes(StandardCharsets.UTF_8));
            baos.write(0);

            // FIXME Second line for the condition
            baos.write(0);

            // FIXME

            baos.write(weather.forecasts.size());
            for (final WeatherSpec.Daily forecast : weather.forecasts) {
                // FIXME
            }
        } catch (final IOException e) {
            LOG.error("Failed to encode weather payload", e);
            return;
        }

        final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 4 + baos.size())
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_REPLY_COMPLEX);
        buf.putShort(COMPLEX_REPLY_WEATHER);
        buf.putInt(baos.size());
        buf.put(baos.toByteArray());

        write("send weather reply", buf.array());
    }

    public void sendReplyReminder() {
        // TODO implement
    }

    public void sendReplyAlarm() {
        // TODO implement
    }

    public void sendVoiceReply(final List<byte[]> voiceFrames) {
        try {
            final TransactionBuilder builder = getSupport().performInitialized("send voice reply");

            for (final byte[] voiceFrame : voiceFrames) {
                // TODO encode
            }

            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to send voice reply", e);
        }
    }

    public void setLanguage(final String language) {
        if (language == null) {
            LOG.warn("Alexa language is null");
            return;
        }

        final byte[] languageBytes = language.replace("_", "-").getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(languageBytes.length + 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_SET_LANGUAGE);
        buf.put(languageBytes);
        buf.put((byte) 0);

        write("set alexa language", buf.array());
    }

    public void sendError(final byte errorCode, final String errorMessage) {
        final byte[] messageBytes = StringUtils.ensureNotNull(errorMessage).getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(messageBytes.length + 3)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_ERROR);
        buf.put(errorCode);
        buf.put(messageBytes);
        buf.put((byte) 0);

        write("send alexa error", buf.array());
    }

    public void sendStartAck() {
        write("send alexa start ack", new byte[]{CMD_START_ACK, 0x00});
    }

    public void sendCmdTriggered() {
        write("alexa cmd triggered", CMD_TRIGGERED);
    }

    public void sendVoiceMore() {
        write("alexa request more voice", CMD_REPLY_VOICE_MORE);
    }

    private void handleCapabilitiesResponse(final byte[] payload) {
        final int version = payload[1] & 0xFF;
        if (version != 3) {
            LOG.warn("Unsupported alexa service version {}", version);
            return;
        }
        final byte var1 = payload[2];
        if (var1 != 1) {
            LOG.warn("Unexpected value for var1 '{}'", var1);
        }
        final byte var2 = payload[3];
        if (var1 != 1) {
            LOG.warn("Unexpected value for var2 '{}'", var2);
        }

        getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, version));

        LOG.info("Alexa version={}, var1={}, var2={}", version, var1, var2);
    }

    private void handleStart(final byte[] payload) {
        final byte var1 = payload[1];
        final byte var2 = payload[2];
        final byte var3 = payload[3];
        final byte var4 = payload[4];
        final String params = StringUtils.untilNullTerminator(payload, 5);

        LOG.info("Alexa starting: var1={}, var2={}, var3={}, var4={}, params={}", var1, var2, var3, var4, params);

        // Send the start ack with a slight delay, to give enough time for the connection to switch to fast mode
        // I can't seem to get the callback for onConnectionUpdated working, and if we reply too soon the watch
        // will just stay stuck "Connecting...". It seems like it takes ~350ms to switch to fast connection.
        handler.postDelayed(this::sendStartAck, 700);
    }

    private void handleEnd(final byte[] payload) {
        voiceBuffer.reset();
        // TODO do something else?
    }

    private void handleVoiceData(final byte[] payload) {
        LOG.info("Got {} bytes of voice data", payload.length);
        // TODO
    }

    private void handleLanguagesResponse(final byte[] payload) {
        int pos = 2;
        final String currentLanguage = StringUtils.untilNullTerminator(payload, pos);
        pos = pos + currentLanguage.length() + 1;

        final int numLanguages = payload[pos++] & 0xFF;
        final List<String> allLanguages = new ArrayList<>();

        for (int i = 0; i < numLanguages; i++) {
            final String language = StringUtils.untilNullTerminator(payload, pos);
            allLanguages.add(language);
            pos = pos + language.length() + 1;
        }

        LOG.info("Got alexa language = {}, supported languages = {}", currentLanguage, allLanguages);

        final GBDeviceEventUpdatePreferences evt = new GBDeviceEventUpdatePreferences()
                .withPreference(PREF_VOICE_SERVICE_LANGUAGE, currentLanguage.replace("-", "_"))
                .withPreference(DeviceSettingsUtils.getPrefPossibleValuesKey(PREF_VOICE_SERVICE_LANGUAGE), TextUtils.join(",", allLanguages).replace("-", "_"));
        getSupport().evaluateGBDeviceEvent(evt);
    }

    public static boolean isSupported(final Prefs devicePrefs) {
        return devicePrefs.getInt(PREF_VERSION, 0) == 3;
    }
}
