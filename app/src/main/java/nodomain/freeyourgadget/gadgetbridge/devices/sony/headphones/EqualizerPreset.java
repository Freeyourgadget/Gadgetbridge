/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

public enum EqualizerPreset {
    OFF(new byte[] {(byte) 0x00, (byte) 0x00}),
    BRIGHT(new byte[] {(byte) 0x10, (byte) 0x00}),
    EXCITED(new byte[] {(byte) 0x11, (byte) 0x00}),
    MELLOW(new byte[] {(byte) 0x12, (byte) 0x00}),
    RELAXED(new byte[] {(byte) 0x13, (byte) 0x00}),
    VOCAL(new byte[] {(byte) 0x14, (byte) 0x00}),
    TREBLE_BOOST(new byte[] {(byte) 0x15, (byte) 0x00}),
    BASS_BOOST(new byte[] {(byte) 0x16, (byte) 0x00}),
    SPEECH(new byte[] {(byte) 0x17, (byte) 0x00}),
    MANUAL(new byte[] {(byte) 0xa0, (byte) 0x00}),
    CUSTOM_1(new byte[] {(byte) 0xa1, (byte) 0x00}),
    CUSTOM_2(new byte[] {(byte) 0xa2, (byte) 0x00});

    public final byte[] code;

    EqualizerPreset(final byte[] code) {
        this.code = code;
    }
}
