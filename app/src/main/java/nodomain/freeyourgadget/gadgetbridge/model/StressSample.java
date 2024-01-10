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
package nodomain.freeyourgadget.gadgetbridge.model;

public interface StressSample extends TimeSample {
    enum Type {
        MANUAL(0),
        AUTOMATIC(1),
        UNKNOWN(2),
        ;

        private final int num;

        Type(final int num) {
            this.num = num;
        }

        public int getNum() {
            return num;
        }

        public static Type fromNum(final int num) {
            for (Type value : Type.values()) {
                if (value.getNum() == num) {
                    return value;
                }
            }

            throw new IllegalArgumentException("Unknown num " + num);
        }
    }

    /**
     * Returns the measurement type for this stress value.
     */
    Type getType();

    /**
     * Returns the stress value between 0 and 100.
     */
    int getStress();
}
