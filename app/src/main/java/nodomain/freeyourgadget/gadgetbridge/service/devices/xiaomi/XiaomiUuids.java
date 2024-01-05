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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class XiaomiUuids {
    public static final UUID UUID_SERVICE_SERIAL_PORT_PROFILE = UUID.fromString(String.format(BASE_UUID, "1101"));
    public static final Map<UUID, XiaomiBleUuidSet> BLE_UUIDS = new LinkedHashMap<UUID, XiaomiBleUuidSet>() {{
        // all encrypted devices seem to share the same characteristics
        // Mi Band 8
        // Redmi Watch 3 Active
        // Xiaomi Watch S1 (Active)
        // Redmi Smart Band 2
        // Redmi Watch 2 Lite
        put(UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb"), new XiaomiBleUuidSet(
                true,
                UUID.fromString("00000051-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("00000052-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("00000053-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("00000055-0000-1000-8000-00805f9b34fb")
        ));

        // Mi Watch Lite
        // Redmi Watch
        put(UUID.fromString("16186f00-0000-1000-8000-00807f9b34fb"), new XiaomiBleUuidSet(
                false,
                UUID.fromString("16186f01-0000-1000-8000-00807f9b34fb"),
                UUID.fromString("16186f02-0000-1000-8000-00807f9b34fb"),
                UUID.fromString("16186f03-0000-1000-8000-00807f9b34fb"),
                UUID.fromString("16186f04-0000-1000-8000-00807f9b34fb")
        ));

        // Mi Smart Watch 4C
        // Redmi Band
        put(UUID.fromString("16187f00-0000-1000-8000-00807f9b34fb"), new XiaomiBleUuidSet(
                false, // FIXME check
                UUID.fromString("16187f02-0000-1000-8000-00807f9b34fb"),
                UUID.fromString("16187f01-0000-1000-8000-00807f9b34fb"),
                UUID.fromString("16187f03-0000-1000-8000-00807f9b34fb"),
                UUID.fromString("16187f04-0000-1000-8000-00807f9b34fb")
        ));

        // Mi Watch (Color (Sport))
        put(UUID.fromString("1314f000-1000-9000-7000-301291e21220"), new XiaomiBleUuidSet(
                false,
                UUID.fromString("1314f005-1000-9000-7000-301291e21220"),
                UUID.fromString("1314f001-1000-9000-7000-301291e21220"),
                UUID.fromString("1314f002-1000-9000-7000-301291e21220"),
                UUID.fromString("1314f007-1000-9000-7000-301291e21220")
        ));

        // Mi Watch CN
        put(UUID.fromString("7495fe00-a7f3-424b-92dd-4a006a3aef56"), new XiaomiBleUuidSet(
                false, // FIXME check
                UUID.fromString("74950002-a7f3-424b-92dd-4a006a3aef56"),
                UUID.fromString("74950001-a7f3-424b-92dd-4a006a3aef56"),
                UUID.fromString("74950003-a7f3-424b-92dd-4a006a3aef56"),
                null
        ));
    }};

    public static class XiaomiBleUuidSet {
        private final boolean encrypted;
        private final UUID characteristicCommandRead;
        private final UUID characteristicCommandWrite;
        private final UUID characteristicActivityData;
        private final UUID characteristicDataUpload;

        public XiaomiBleUuidSet(final boolean encrypted,
                                final UUID characteristicCommandRead,
                                final UUID characteristicCommandWrite,
                                final UUID characteristicActivityData,
                                final UUID characteristicDataUpload) {

            this.encrypted = encrypted;
            this.characteristicCommandRead = characteristicCommandRead;
            this.characteristicCommandWrite = characteristicCommandWrite;
            this.characteristicActivityData = characteristicActivityData;
            this.characteristicDataUpload = characteristicDataUpload;
        }

        protected boolean isEncrypted() {
            return encrypted;
        }

        protected UUID getCharacteristicCommandRead() {
            return characteristicCommandRead;
        }

        protected UUID getCharacteristicCommandWrite() {
            return characteristicCommandWrite;
        }

        protected UUID getCharacteristicActivityData() {
            return characteristicActivityData;
        }

        protected UUID getCharacteristicDataUpload() {
            return characteristicDataUpload;
        }
    }
}
