/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings;

import android.content.pm.PackageManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;

public class CameraAppTypeSetting implements Wena3Packetable {
    private static final String PHOTOPRO_APP_ID = "com.sonymobile.photopro";
    public final boolean hasXperiaApp;

    public CameraAppTypeSetting(boolean isXperia) {
        this.hasXperiaApp = isXperia;
    }

    public static CameraAppTypeSetting findOut(PackageManager pm) {
        try {
            pm.getPackageInfo(PHOTOPRO_APP_ID, 0);
            return new CameraAppTypeSetting(true);
        } catch (PackageManager.NameNotFoundException e) {
            return new CameraAppTypeSetting(false);
        }
    }

    @Override
    public byte[] toByteArray() {
        return ByteBuffer
                .allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte)0x21)
                .put((byte) (hasXperiaApp ? 0x1 : 0x0))
                .array();
    }
}
