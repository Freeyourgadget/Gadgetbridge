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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.BarcodeFormat;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeppOsLoyaltyCardService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsLoyaltyCardService.class);

    private static final short ENDPOINT = 0x003c;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_REQUEST = 0x05;
    private static final byte CMD_RESPONSE = 0x06;
    private static final byte CMD_SET = 0x03;
    private static final byte CMD_SET_ACK = 0x04;
    private static final byte CMD_UPDATE = 0x07;
    private static final byte CMD_UPDATE_ACK = 0x08;
    private static final byte CMD_ADD = 0x09;
    private static final byte CMD_ADD_ACK = 0x0a;

    private final List<BarcodeFormat> supportedFormats = new ArrayList<>();
    private final List<Integer> supportedColors = new ArrayList<>();

    public static final String PREF_VERSION = "zepp_os_loyalty_cards_version";

    public ZeppOsLoyaltyCardService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                LOG.info("Loyalty cards capabilities, version1={}, version2={}", payload[1], payload[2]);

                supportedFormats.clear();
                supportedColors.clear();
                int version = payload[1];

                if (version != 1 || payload[2] != 1) {
                    LOG.warn("Unexpected loyalty cards service version");
                    return;
                }

                int pos = 3;

                final byte numSupportedCardTypes = payload[pos++];
                final Map<Byte, BarcodeFormat> barcodeFormatCodes = MapUtils.reverse(BARCODE_FORMAT_CODES);
                for (int i = 0; i < numSupportedCardTypes; i++, pos++) {
                    final BarcodeFormat barcodeFormat = barcodeFormatCodes.get(payload[pos]);
                    if (barcodeFormat == null) {
                        LOG.warn("Unknown barcode format {}", String.format("0x%02x", payload[pos]));
                        continue;
                    }
                    supportedFormats.add(barcodeFormat);
                }

                final byte numSupportedColors = payload[pos++];
                final Map<Byte, Integer> colorCodes = MapUtils.reverse(COLOR_CODES);
                for (int i = 0; i < numSupportedColors; i++) {
                    final Integer color = colorCodes.get(payload[pos]);
                    if (color == null) {
                        LOG.warn("Unknown color {}", String.format("0x%02x", payload[pos]));
                        continue;
                    }
                    supportedColors.add(color);
                }

                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, version));
                return;
            case CMD_SET_ACK:
                LOG.info("Loyalty cards set ACK, status = {}", payload[1]);
                return;
        }

        LOG.warn("Unexpected loyalty cards byte {}", String.format("0x%02x", payload[0]));
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
    }

    public boolean isSupported() {
        return !supportedFormats.isEmpty() && !supportedColors.isEmpty();
    }

    public List<BarcodeFormat> getSupportedFormats() {
        return supportedFormats;
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void setCards(final List<LoyaltyCard> cards) {
        LOG.info("Setting {} loyalty cards", cards.size());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final List<LoyaltyCard> supportedCards = filterSupportedCards(cards);

        baos.write(CMD_SET);
        baos.write(supportedCards.size());

        for (final LoyaltyCard card : supportedCards) {
            try {
                baos.write(encodeCard(card));
            } catch (final Exception e) {
                LOG.error("Failed to encode card", e);
                return;
            }
        }

        write("set loyalty cards", baos.toByteArray());
    }

    private List<LoyaltyCard> filterSupportedCards(final List<LoyaltyCard> cards) {
        final List<LoyaltyCard> ret = new ArrayList<>();

        for (final LoyaltyCard card : cards) {
            if (supportedFormats.contains(card.getBarcodeFormat())) {
                ret.add(card);
            }
        }

        return ret;
    }

    private byte[] encodeCard(final LoyaltyCard card) {
        final Byte barcodeFormatCode = BARCODE_FORMAT_CODES.get(card.getBarcodeFormat());
        if (barcodeFormatCode == null) {
            LOG.error("Unsupported barcode format {}", card.getBarcodeFormat());
            return null;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(card.getName().getBytes(StandardCharsets.UTF_8));
            baos.write(0);

            // This is optional
            baos.write(card.getCardId().getBytes(StandardCharsets.UTF_8));
            baos.write(0);

            if (card.getBarcodeId() != null) {
                baos.write(card.getBarcodeId().getBytes(StandardCharsets.UTF_8));
            } else {
                baos.write(card.getCardId().getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            baos.write(barcodeFormatCode);
            if (card.getColor() != null) {
                baos.write(findNearestColorCode(card.getColor()));
            } else {
                baos.write(0x00);
            }
        } catch (final Exception e) {
            LOG.error("Failed to encode card", e);
            return null;
        }

        return baos.toByteArray();
    }

    private byte findNearestColorCode(final int color) {
        final double r = ((color >> 16) & 0xff) / 255f;
        final double g = ((color >> 8) & 0xff) / 255f;
        final double b = (color & 0xff) / 255f;

        int nearestColor = 0x66c6ea;
        double minDistance = Float.MAX_VALUE;

        // TODO better color distance algorithm?
        for (final Integer colorPreset : COLOR_CODES.keySet()) {
            final double rPreset = ((colorPreset >> 16) & 0xff) / 255f;
            final double gPreset = ((colorPreset >> 8) & 0xff) / 255f;
            final double bPreset = (colorPreset & 0xff) / 255f;

            final double distance = Math.sqrt(Math.pow(rPreset - r, 2) + Math.pow(gPreset - g, 2) + Math.pow(bPreset - b, 2));
            if (distance < minDistance) {
                nearestColor = colorPreset;
                minDistance = distance;
            }
        }

        return Objects.requireNonNull(COLOR_CODES.get(nearestColor));
    }

    private static final Map<BarcodeFormat, Byte> BARCODE_FORMAT_CODES = new HashMap<BarcodeFormat, Byte>() {{
        put(BarcodeFormat.CODE_128, (byte) 0x00);
        put(BarcodeFormat.CODE_39, (byte) 0x01);
        put(BarcodeFormat.ITF, (byte) 0x02);
        put(BarcodeFormat.QR_CODE, (byte) 0x03);
        put(BarcodeFormat.PDF_417, (byte) 0x04);
        put(BarcodeFormat.DATA_MATRIX, (byte) 0x05);
        put(BarcodeFormat.UPC_A, (byte) 0x06);
        put(BarcodeFormat.EAN_13, (byte) 0x07);
        put(BarcodeFormat.EAN_8, (byte) 0x08);
    }};

    /**
     * Map or RGB color to color byte - the watches only support color presets.
     */
    private static final Map<Integer, Byte> COLOR_CODES = new HashMap<Integer, Byte>() {{
        put(0x66c6ea, (byte) 0x00); // Light blue
        put(0x008fc5, (byte) 0x01); // Blue
        put(0xc19ffd, (byte) 0x02); // Light purple
        put(0x8855e2, (byte) 0x03); // Purple
        put(0xfb8e89, (byte) 0x04); // Light red
        put(0xdf3b34, (byte) 0x05); // Red
        put(0xffab03, (byte) 0x06); // Orange
        put(0xffaa77, (byte) 0x07); // Light Orange
        put(0xe75800, (byte) 0x08); // Dark Orange
        put(0x66d0b8, (byte) 0x09); // Light green
        put(0x009e7a, (byte) 0x0a); // Green
        put(0xffcd68, (byte) 0x0b); // Yellow-ish
    }};

    public static boolean isSupported(final Prefs devicePrefs) {
        return devicePrefs.getInt(PREF_VERSION, 0) == 1;
    }
}
