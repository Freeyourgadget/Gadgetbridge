/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiBitmapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class XiaomiFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiFWHelper.class);
    // TODO use to determine translatable face name displayed in UI
    private static final String[] FACE_LOCALE_BITMAP_LIST = new String[]{
            "en_US",
            "zh_CN",
            "zh_TW",
            "ja_JP",
            "es_ES",
            "fr_FR",
            "de_DE",
            "ru_RU",
            "pt_BR",
            "pt_PT",
            "it_IT",
            "ko_KR",
            "tr_TR",
            "nl_NL",
            "th_TH",
            "sv_SE",
            "da_DK",
            "vi_VN",
            "nb_NO",
            "pl_PL",
            "fi_FI",
            "in_ID", // old ISO-639
            "el_GR",
            "ro_RO",
            "cs_CZ",
            "uk_UA",
            "hu_HU",
            "sk_SK",
            "ar_EG",
            "iw_IL", // old ISO-639
            "zh_HK",
    };

    private final Uri uri;
    private byte[] fw;
    private boolean valid;
    private boolean typeFirmware;
    private boolean typeWatchface;

    private String id;
    private String name;
    private String version;

    public XiaomiFWHelper(final Uri uri, final Context context) {
        this.uri = uri;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri helper for {}", uri, e);
            return;
        }

        final int maxExpectedFileSize = 1024 * 1024 * 128; // 64MB

        if (uriHelper.getFileSize() > maxExpectedFileSize) {
            LOG.warn("Firmware size is larger than the maximum expected file size of {}", maxExpectedFileSize);
            return;
        }

        try (final InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            this.fw = FileUtils.readAll(in, maxExpectedFileSize);
        } catch (final IOException e) {
            LOG.error("Failed to read bytes from {}", uri, e);
            return;
        }

        parseBytes();
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isWatchface() {
        return typeWatchface;
    }

    public boolean isFirmware() {
        return typeFirmware;
    }

    public String getDetails() {
        return name != null ? name : (version != null ? version : "UNKNOWN");
    }

    public byte[] getBytes() {
        return fw;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void unsetFwBytes() {
        this.fw = null;
    }

    private void parseBytes() {
        if (parseAsWatchface()) {
            assert id != null;
            valid = true;
            typeWatchface = true;
        } else if (parseAsFirmware()) {
            assert version != null;
            valid = true;
            typeFirmware = true;
        } else {
            valid = false;
        }
    }

    private int findBestI18n(final long availableLocales) {
        // try to find a locale matching the user's preferences amongst locales available
        final Locale userLocale = GBApplication.getLanguage();
        int found = -1, fallbackLocale = -1;
        for (int i = 0; i < FACE_LOCALE_BITMAP_LIST.length; i++) {
            if ((availableLocales & (1L << i)) != 0) {
                final String[] localeParts = FACE_LOCALE_BITMAP_LIST[i].split("_");
                final Locale locale = new Locale(localeParts[0], localeParts[1]);

                if (locale.getLanguage().equals(userLocale.getLanguage())) {
                    // return for this entry if locale matches on language and country
                    if (locale.getCountry().equals(userLocale.getCountry())) {
                        LOG.debug("Found locale match: {}", FACE_LOCALE_BITMAP_LIST[i]);
                        return i;
                    }

                    // keep scanning as a better locale might exist (Portuguese)
                    LOG.debug("Found language match for {}_{}: {}",
                            userLocale.getLanguage(),
                            userLocale.getCountry(),
                            FACE_LOCALE_BITMAP_LIST[i]);
                    found = i;
                }

                // keep first found available locale as fallback
                if (fallbackLocale == -1) {
                    fallbackLocale = i;
                }
            }
        }

        if (found != -1) {
            return found;
        }

        if (fallbackLocale != -1) {
            LOG.debug("Using fallback locale {}", FACE_LOCALE_BITMAP_LIST[fallbackLocale]);
            return fallbackLocale;
        }

        LOG.debug("No translation available");
        return -1;
    }

    private String extractTranslatableString(final int tableOffset, final int tableSize) {
        if (tableOffset < 0 || tableSize < 0 || tableOffset + tableSize > fw.length) {
            LOG.error("i18n table out-of-bounds");
            return null;
        }

        final ByteBuffer bb = ByteBuffer.wrap(fw, tableOffset, tableSize).order(ByteOrder.LITTLE_ENDIAN);
        long availableLocalizations = bb.getLong();
        final int localizationsCount = Long.bitCount(availableLocalizations);

        if (tableSize < localizationsCount * 4 + 8) {
            LOG.error("cannot decode i18n table (at least {} bytes required for length table, but localization block is only {} bytes)",
                    localizationsCount * 4 + 8,
                    tableSize);
            return null;
        }

        final int targetLocale = findBestI18n(availableLocalizations);
        if (targetLocale == -1) {
            return null;
        }

        int targetOffset = 0, targetSize = 0;
        for (int i = 0; availableLocalizations != 0; i++) {
            if ((availableLocalizations & 1L) != 0) {
                final int size = bb.getInt();

                if (i < targetLocale) {
                    targetOffset += size;
                }

                if (i == targetLocale) {
                    targetSize = size;
                }
            }
            availableLocalizations >>= 1;
        }

        if (bb.remaining() < targetOffset + targetSize) {
            LOG.error("cannot extract localization (at least {} bytes required, but only {} bytes remaining)",
                    targetOffset + targetSize,
                    bb.remaining());
            return null;
        }

        bb.position(bb.position() + targetOffset);
        final byte[] localizationBytes = new byte[targetSize];
        bb.get(localizationBytes);
        return new String(localizationBytes, StandardCharsets.UTF_8);
    }

    public Bitmap getWatchfacePreview() {
        if (!isWatchface() || fw == null) {
            return null;
        }

        final ByteBuffer bb = ByteBuffer.wrap(fw).order(ByteOrder.LITTLE_ENDIAN);
        final int previewOffset = bb.getInt(0x20);
        if (previewOffset == 0) {
            LOG.debug("No preview available (at offset 0)");
            return null;
        }

        if (previewOffset + 12 > fw.length) {
            LOG.debug("No preview available (header out-of-bounds)");
            return null;
        }

        bb.position(previewOffset);
        final int bitmapType = bb.get() & 0xff;
        final int compressionType = bb.get() & 0xff;
        bb.getShort(); // ignore
        final int width = bb.getShort() & 0xffff;
        final int height = bb.getShort() & 0xffff;
        final int bitmapSize = bb.getInt();

        byte[] bitmapData = new byte[bitmapSize];
        bb.get(bitmapData);

        if (compressionType != 0) {
            LOG.debug("Preview image compression type: {}", compressionType);
            switch (compressionType) {
                case 4:
                    bitmapData = XiaomiBitmapUtils.decompressLvglRleV2(bitmapData);
                    break;
                case 8:
                    bitmapData = XiaomiBitmapUtils.decompressLvglRleV1(bitmapData);
                    break;
                default:
                    LOG.error("unknown compression type {}", compressionType);
                    return null;
            }

            if (bitmapData == null) {
                LOG.error("decompression returned null");
                return null;
            }
        }

        return XiaomiBitmapUtils.decodeWatchfaceImage(
                bitmapData,
                bitmapType,
                compressionType == 8,
                width,
                height
        );
    }

    private boolean parseAsWatchface() {
        if (fw[0] != (byte) 0x5A || fw[1] != (byte) 0xA5) {
            LOG.warn("File header not a watchface");
            return false;
        }

        id = StringUtils.untilNullTerminator(fw, 0x28);

        if (id == null) {
            LOG.warn("id not found in {}", uri);
            return false;
        }

        if (!Pattern.matches("^\\d+$", id)) {
            LOG.warn("Id {} not a number", id);
            return false;
        }

        if (ArrayUtils.equals(fw, new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff}, 0x68)) {
            // name is localized
            LOG.debug("detected translatable face name");
            name = ""; // default value in case localization fails
            final int tableOffset = BLETypeConversions.toUint32(fw, 0x74);
            final int tableSize = BLETypeConversions.toUint32(fw, 0x78);

            if (tableSize > 8) {
                final String localized = extractTranslatableString(tableOffset, tableSize);

                if (localized != null) {
                    name = localized;
                }
            }
        } else {
            name = StringUtils.untilNullTerminator(fw, 0x68);

            if (name == null) {
                LOG.warn("name not found in {}", uri);
                return false;
            }
        }

        return true;
    }

    private boolean parseAsFirmware() {
        // TODO parse and set version
        return false;
    }
}
