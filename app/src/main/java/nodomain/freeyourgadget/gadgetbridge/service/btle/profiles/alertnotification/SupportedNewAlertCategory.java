/*  Copyright (C) 2016-2019 Carsten Pfeiffer, Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id.xml
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id_bit_mask.xml
 */
public class SupportedNewAlertCategory {
    private final int id;
//
//    public static Ca(byte[] categoryBytes) {
//
//    }

    public SupportedNewAlertCategory(int id) {
        this.id = id;
    }

    /**
     * Returns the numerical ID value of this category
     * To be used as uint8 value
     * @return the uint8 value for this category
     */
    public int getId() {
        return id;
    }

    private int realBitNumber() {
        // the ID corresponds to the bit for the bitset
        return id;
    }

    private int bitNumberPerByte() {
        // the ID corresponds to the bit for the bitset (per byte)
        return realBitNumber() % 8;
    }

    private int asBit() {
        return 1 << bitNumberPerByte();
    }

    private int byteNumber() {
        return id <= 7 ? 0 : 1;
    }

    /**
     * Converts the given categories to an array of bytes.
     * @param categories
     * @return
     */
    public static byte[] toBitmask(SupportedNewAlertCategory... categories) {
        byte[] result = new byte[2];

        for (SupportedNewAlertCategory category : categories) {
            result[category.byteNumber()] |= category.asBit();
        }
        return result;
    }
}
