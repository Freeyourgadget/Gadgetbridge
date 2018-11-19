/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, João Paulo Barraca, José Rebelo, Kranz, ladbsoft, maxirnilian,
    protomors, Quallenauge, Sami Alaoui, tiparega, Vadim Kaushan

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
package nodomain.freeyourgadget.gadgetbridge.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * For every supported device, a device type constant must exist.
 *
 * Note: they key of every constant is stored in the DB, so it is fixed forever,
 * and may not be changed.
 */
public enum DeviceType {
    UNKNOWN(-1, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_unknown),
    PEBBLE(1, R.drawable.ic_device_pebble, R.drawable.ic_device_pebble_disabled, R.string.devicetype_pebble),
    MIBAND(10, R.drawable.ic_device_miband, R.drawable.ic_device_miband_disabled, R.string.devicetype_miband),
    MIBAND2(11, R.drawable.ic_device_miband2, R.drawable.ic_device_miband2_disabled, R.string.devicetype_miband2),
    AMAZFITBIP(12, R.drawable.ic_device_hplus, R.drawable.ic_device_hplus_disabled, R.string.devicetype_amazfit_bip),
    AMAZFITCOR(13, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_amazfit_cor),
    MIBAND3(14, R.drawable.ic_device_miband2, R.drawable.ic_device_miband2_disabled, R.string.devicetype_miband3),
    VIBRATISSIMO(20, R.drawable.ic_device_lovetoy, R.drawable.ic_device_lovetoy_disabled, R.string.devicetype_vibratissimo),
    LIVEVIEW(30, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_liveview),
    HPLUS(40, R.drawable.ic_device_hplus, R.drawable.ic_device_hplus_disabled, R.string.devicetype_hplus),
    MAKIBESF68(41, R.drawable.ic_device_hplus, R.drawable.ic_device_hplus_disabled, R.string.devicetype_makibes_f68),
    EXRIZUK8(42, R.drawable.ic_device_hplus, R.drawable.ic_device_hplus_disabled, R.string.devicetype_exrizu_k8),
    Q8(43, R.drawable.ic_device_hplus, R.drawable.ic_device_hplus_disabled, R.string.devicetype_q8),
    NO1F1(50, R.drawable.ic_device_hplus, R.drawable.ic_device_hplus_disabled, R.string.devicetype_no1_f1),
    TECLASTH30(60, R.drawable.ic_device_h30_h10, R.drawable.ic_device_h30_h10_disabled, R.string.devicetype_teclast_h30),
    XWATCH(70, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_xwatch),
    FOSSILQHYBRID(80, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_qhybrid),
    ZETIME(80, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_mykronoz_zetime),
    ID115(90, R.drawable.ic_device_h30_h10, R.drawable.ic_device_h30_h10_disabled, R.string.devicetype_id115),
    WATCH9(100, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_watch9),
    ROIDMI(110, R.drawable.ic_device_roidmi, R.drawable.ic_device_roidmi_disabled, R.string.devicetype_roidmi),
    ROIDMI3(112, R.drawable.ic_device_roidmi, R.drawable.ic_device_roidmi_disabled, R.string.devicetype_roidmi3),
    TEST(1000, R.drawable.ic_device_default, R.drawable.ic_device_default_disabled, R.string.devicetype_test);

    private final int key;
    @DrawableRes
    private final int defaultIcon;
    @DrawableRes
    private final int disabledIcon;
    @StringRes
    private final int name;

    DeviceType(int key, int defaultIcon, int disabledIcon, int name) {
        this.key = key;
        this.defaultIcon = defaultIcon;
        this.disabledIcon = disabledIcon;
        this.name = name;
    }

    public int getKey() {
        return key;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    public static DeviceType fromKey(int key) {
        for (DeviceType type : values()) {
            if (type.key == key) {
                return type;
            }
        }
        return DeviceType.UNKNOWN;
    }

    @StringRes
    public int getName() {
        return name;
    }

    @DrawableRes
    public int getIcon() {
        return defaultIcon;
    }

    @DrawableRes
    public int getDisabledIcon() {
        return disabledIcon;
    }
}
