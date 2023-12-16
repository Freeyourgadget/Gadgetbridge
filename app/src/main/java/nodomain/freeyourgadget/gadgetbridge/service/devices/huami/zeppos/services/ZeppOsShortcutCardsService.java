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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.SHORTCUT_CARDS_SORTABLE;

import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsShortcutCardsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsShortcutCardsService.class);

    private static final short ENDPOINT = 0x0009;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_LIST_GET = 0x07;
    public static final byte CMD_LIST_RET = 0x08;
    public static final byte CMD_ENABLED_SET = 0x09;
    public static final byte CMD_ENABLED_SET_ACK = 0x0a;

    public static final String PREF_VERSION = "zepp_os_shortcut_cards_version";

    public enum ShortcutCard {
        WEATHER("2", "1"),
        AQI("2", "2"),
        FORECAST("2", "3"),
        PAI("3", "1"),
        ALARM("4", "1"),
        SLEEP("5", "1"),
        HEARTRATE("6", "1"),
        ACTIVITY("7", "1"),
        SPO2("8", "1"),
        PHONE("9", "1"),
        EVENTS("10", "1"),
        STRESS("11", "1"),
        THERMOMETER("12", "1"),
        WORLDCLOCK("13", "1"),
        TODO("17", "1"),
        COUNTDOWN("18", "1"),
        LAST_WORKOUT("19", "1"),
        TOTAL_WORKOUT("19", "2"),
        WORKOUT_STATUS("19", "3"),
        VO2_MAX("19", "4"),
        MUSIC("20", "1"),
        CYCLE_TRACKING("21", "1"),
        ONE_TAP_MEASURING("22", "1"),
        BREATHING("24", "1"),
        STOPWATCH("25", "1"),
        ZEPP_COACH("27", "1"),
        RECOMMENDATION("28", "1"),
        BODY_COMPOSITION("33", "1"),
        READINESS("34", "1"),
        ALEXA("35", "1"),
        ZEPP_PAY("37", "1"),
        ;

        private final String appNum;
        private final String cardNum;

        ShortcutCard(final String appNum, final String cardNum) {
            this.appNum = appNum;
            this.cardNum = cardNum;
        }

        public String getAppNum() {
            return appNum;
        }

        public String getCardNum() {
            return cardNum;
        }

        public static ShortcutCard fromCodes(final String appNum, final String cardNum) {
            for (ShortcutCard value : ShortcutCard.values()) {
                if (value.getAppNum().equals(appNum) && value.getCardNum().equals(cardNum)) {
                    return value;
                }
            }

            return null;
        }
    }

    private int version = 0;
    private int maxCards = 0;

    public ZeppOsShortcutCardsService(final Huami2021Support support) {
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
            case CMD_CAPABILITIES_RESPONSE:
                version = payload[1];
                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, version));
                if (version != 3 && version != 4) {
                    LOG.warn("Unsupported shortcut cards service version {}", version);
                    return;
                }
                maxCards = payload[2] & 0xFF;
                LOG.info("Shortcut cards version={}, maxCards={}", version, maxCards);
                break;
            case CMD_LIST_RET:
                LOG.info("Got shortcut cards list");
                parseShortcutCards(payload);
                break;
            case CMD_ENABLED_SET_ACK:
                LOG.info("Got enabled shortcut cards ack, status = {}", payload[1]);
                break;
            default:
                LOG.warn("Unexpected shortcut cards byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.SHORTCUT_CARDS_SORTABLE:
                final List<String> shortcutCards = prefs.getList(SHORTCUT_CARDS_SORTABLE, Collections.emptyList());
                LOG.info("Setting shortcut cards to {}", shortcutCards);
                setShortcutCards(shortcutCards);
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
        requestShortcutCards(builder);
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void requestShortcutCards(final TransactionBuilder builder) {
        write(builder, CMD_LIST_GET);
    }

    public void parseShortcutCards(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard the command byte
        final int numCards = buf.get() & 0xFF;
        final List<String> allCards = new ArrayList<>();
        final List<String> enabledCards = new ArrayList<>();

        for (int i = 0; i < numCards; i++) {
            final String appNum = StringUtils.untilNullTerminator(buf);
            final String cardNum = StringUtils.untilNullTerminator(buf);
            final boolean enabled = buf.get() == 0x01;
            final byte b = buf.get();
            if (b != 0) {
                LOG.warn("Unexpected byte {} at pos {}", b, buf.position() - 1);
                return;
            }
            final ShortcutCard card = ShortcutCard.fromCodes(appNum, cardNum);
            final String cardPrefValue;
            if (card != null) {
                cardPrefValue = card.name().toLowerCase(Locale.ROOT);
            } else {
                LOG.warn("Unknown shortcut card [{}, {}]", appNum, cardNum);
                cardPrefValue = appNum + "/" + cardNum;
            }
            allCards.add(cardPrefValue);
            if (enabled) {
                enabledCards.add(cardPrefValue);
            }
        }

        final GBDeviceEventUpdatePreferences evt = new GBDeviceEventUpdatePreferences()
                .withPreference(SHORTCUT_CARDS_SORTABLE, TextUtils.join(",", enabledCards))
                .withPreference(DeviceSettingsUtils.getPrefPossibleValuesKey(SHORTCUT_CARDS_SORTABLE), TextUtils.join(",", allCards));
        getSupport().evaluateGBDeviceEvent(evt);
    }

    public void setShortcutCards(final List<String> cardsPrefValue) {
        if (maxCards == 0) {
            LOG.warn("maxCards == 0, refusing");
            return;
        }

        final List<String> cards = new ArrayList<>(cardsPrefValue);
        if (cards.size() > maxCards) {
            LOG.warn("Number of cards {} > maxCards {}, truncating", cards.size(), maxCards);
            cards.subList(maxCards, cards.size()).clear();
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(CMD_ENABLED_SET);
        baos.write(cards.size());

        for (final String cardPrefValue : cards) {
            String appNum;
            String cardNum;
            try {
                final ShortcutCard card = ShortcutCard.valueOf(cardPrefValue.toUpperCase(Locale.ROOT));
                appNum = card.getAppNum();
                cardNum = card.getCardNum();
            } catch (final IllegalArgumentException e) {
                // attempt to parse as appNum/cardNum
                final Matcher matcher = Pattern.compile("^([0-9a-fA-F]+)/([0-9a-fA-F]+)$").matcher(cardPrefValue);
                if (matcher.find()) {
                    appNum = matcher.group(1);
                    cardNum = matcher.group(2);
                } else {
                    LOG.warn("Unexpected format for shortcut cards pref value {}", cardPrefValue);
                    return;
                }
            }

            try {
                baos.write(appNum.getBytes(StandardCharsets.UTF_8));
                baos.write(0);
                baos.write(cardNum.getBytes(StandardCharsets.UTF_8));
                baos.write(0);
                baos.write(1); // enabled
                baos.write(0); // ?
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        write("set enabled shortcut cards", baos.toByteArray());
    }

    public static boolean isSupported(final Prefs devicePrefs) {
        final int version = devicePrefs.getInt(PREF_VERSION, 0);
        return version == 3 || version == 4;
    }
}
