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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;

public abstract class XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiActivityParser.class);

    private final XiaomiSupport mSupport;

    public XiaomiActivityParser(final XiaomiSupport support) {
        this.mSupport = support;
    }

    public abstract boolean parse(final XiaomiActivityFileId fileId, final byte[] bytes);

    public XiaomiSupport getSupport() {
        return mSupport;
    }

    @Nullable
    public static XiaomiActivityParser create(final XiaomiActivityFileId fileId) {
        switch (fileId.getType()) {
            case XiaomiActivityFileId.TYPE_ACTIVITY:
                return createForActivity(fileId);
            case XiaomiActivityFileId.TYPE_SPORTS:
                return createForSports(fileId);
        }

        LOG.warn("Unknown file type for {}", fileId);
        return null;
    }

    public static XiaomiActivityParser createForActivity(final XiaomiActivityFileId fileId) {
        assert fileId.getType() == XiaomiActivityFileId.TYPE_ACTIVITY;

        return null;
    }

    public static XiaomiActivityParser createForSports(final XiaomiActivityFileId fileId) {
        assert fileId.getType() == XiaomiActivityFileId.TYPE_SPORTS;

        return null;
    }
}
