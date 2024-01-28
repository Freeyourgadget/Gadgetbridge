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

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class CmfFwHelper {
    private static final Logger LOG = LoggerFactory.getLogger(CmfFwHelper.class);

    private static final byte[] HEADER_WATCHFACE = new byte[]{0x01, 0x00, 0x00, 0x02};
    private static final byte[] HEADER_FIRMWARE = new byte[]{'A', 'O', 'T', 'A'};
    private static final byte[] HEADER_AGPS = new byte[]{0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x30, 0x30, 0x30};

    private final Uri uri;
    private byte[] fw;
    private boolean typeFirmware;
    private boolean typeWatchface;
    private boolean typeAgps;

    private String name;
    private String version;

    public CmfFwHelper(final Uri uri, final Context context) {
        this.uri = uri;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri helper for {}", uri, e);
            return;
        }

        final int maxExpectedFileSize = 1024 * 1024 * 32; // 32MB

        if (uriHelper.getFileSize() > maxExpectedFileSize) {
            LOG.warn("File size is larger than the maximum expected file size of {}", maxExpectedFileSize);
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

    public Uri getUri() {
        return uri;
    }

    public boolean isValid() {
        return isWatchface() || isFirmware() || isAgps();
    }

    public boolean isWatchface() {
        return typeWatchface;
    }

    public boolean isFirmware() {
        return typeFirmware;
    }

    public boolean isAgps() {
        return typeAgps;
    }

    public String getDetails() {
        return name != null ? name : (version != null ? version : "UNKNOWN");
    }

    public byte[] getBytes() {
        return fw;
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
            assert name != null;
            typeWatchface = true;
        } else if (parseAsFirmware()) {
            assert version != null;
            typeFirmware = true;
        } else if (parseAsAgps()) {
            typeAgps = true;
        }
    }

    private boolean parseAsWatchface() {
        if (!ArrayUtils.equals(fw, HEADER_WATCHFACE, 4)) {
            LOG.warn("File header not a watchface");
            return false;
        }

        final String nameHeader = StringUtils.untilNullTerminator(fw, 8);
        if (nameHeader == null) {
            LOG.warn("watchface name not found in {}", uri);
            return false;
        }

        // Confirm it's a watchface by finding the same name at the end
        final String nameTrailer = StringUtils.untilNullTerminator(fw, fw.length - 28);
        if (nameTrailer == null) {
            LOG.warn("watchface name not found at the end of {}", uri);
            return false;
        }

        if (!nameHeader.equals(nameTrailer)) {
            LOG.warn("Names in header and trailer do not match");
            return false;
        }

        name = nameHeader;

        return true;
    }

    private boolean parseAsFirmware() {
        if (!ArrayUtils.equals(fw, HEADER_FIRMWARE, 0)) {
            LOG.warn("File header not a firmware");
            return false;
        }

        // FIXME: This is not really the version, but build number?
        final String versionHeader = StringUtils.untilNullTerminator(fw, 64);
        if (versionHeader == null) {
            LOG.warn("firmware version not found in {}", uri);
            return false;
        }

        version = versionHeader;

        return true;
    }

    private boolean parseAsAgps() {
        if (!ArrayUtils.equals(fw, HEADER_AGPS, 0)) {
            LOG.warn("File header not agps");
            return false;
        }

        // TODO parse? and set something

        return true;
    }
}
