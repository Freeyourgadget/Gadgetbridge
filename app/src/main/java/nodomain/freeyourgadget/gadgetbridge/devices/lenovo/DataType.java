/*  Copyright (C) 2019-2021 mamucho, mkusnierz

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
package nodomain.freeyourgadget.gadgetbridge.devices.lenovo;

public enum DataType {
    STEPS(new byte[]{0x00, 0x00}),
    SLEEP(new byte[]{0x00, 0x01}),
    HEART_RATE(new byte[]{0x00, 0x02}),
    BLOOD_PRESSURE(new byte[]{0x00, 0x06}),
    INFRARED_TEMPERATURE(new byte[]{0x00, 0x08}),
    ENVIRONMENT_TEMPERATURE(new byte[]{0x00, 0x09}),
    AIR_PRESSURE(new byte[]{0x00, 0x0A});

    private final byte[] value;

    DataType(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public static DataType getType(int value) {
        for(DataType type : values()) {
            int intVal = (type.getValue()[1] & 0xff) | ((type.getValue()[0] & 0xff) << 8);
            if(intVal == value) {
                return type;
            }
        }
        throw new RuntimeException("No value defined for " + value);
    }
}
