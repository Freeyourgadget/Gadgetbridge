/*  Copyright (C) 2017-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public enum HuamiFirmwareType {
    FIRMWARE((byte) 0, R.string.kind_firmware),
    CHANGELOG_TXT((byte) 16, R.string.action_changelog),
    // MB7 firmwares are sent as UIHH packing FIRMWARE (zip) + CHANGELOG_TXT, type 0xfd
    FIRMWARE_UIHH_2021_ZIP_WITH_CHANGELOG((byte) -3, R.string.kind_firmware),
    FONT((byte) 1, R.string.kind_font),
    RES((byte) 2, R.string.kind_resources),
    RES_COMPRESSED((byte) 130, R.string.kind_resources),
    GPS((byte) 3, R.string.kind_gps),
    GPS_CEP((byte) 4, R.string.kind_gps_cep),
    AGPS_UIHH((byte) -4, R.string.kind_agps_bundle),
    GPS_ALMANAC((byte) 5, R.string.kind_gps_almanac),
    WATCHFACE((byte) 8, R.string.kind_watchface),
    APP((byte) 8, R.string.kind_app),
    FONT_LATIN((byte) 11, R.string.kind_font),
    ZEPPOS_UNKNOWN_0X13((byte) 0x13, R.string.unknown),
    ZEPPOS_APP((byte) 0xa0, R.string.kind_app),
    INVALID(Byte.MIN_VALUE, R.string.kind_invalid),
    ;

    private final byte value;
    private final int nameResId;

    HuamiFirmwareType(byte value, int nameResId) {
        this.value = value;
        this.nameResId = nameResId;
    }

    public byte getValue() {
        return value;
    }

    @StringRes
    public int getNameResId() {
        return nameResId;
    }

    public boolean isApp() {
        return this == APP || this == ZEPPOS_APP;
    }

    public boolean isWatchface() {
        return this == WATCHFACE;
    }
}
