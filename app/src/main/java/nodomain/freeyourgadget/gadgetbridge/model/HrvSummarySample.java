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
package nodomain.freeyourgadget.gadgetbridge.model;

public interface HrvSummarySample extends TimeSample {
    enum Status {
        NONE(0),
        POOR(1),
        LOW(2),
        UNBALANCED(3),
        BALANCED(4),
        ;

        private final int num;

        Status(final int num) {
            this.num = num;
        }

        public int getNum() {
            return num;
        }

        public static Status fromNum(final int num) {
            for (Status value : Status.values()) {
                if (value.getNum() == num) {
                    return value;
                }
            }

            throw new IllegalArgumentException("Unknown hrv status num " + num);
        }
    }

    /**
     * Weekly average, in milliseconds.
     */
    Integer getWeeklyAverage();

    /**
     * Last night average, in milliseconds.
     */
    Integer getLastNightAverage();

    /**
     * Last night 5-min high, in milliseconds.
     */
    Integer getLastNight5MinHigh();

    /**
     * Baseline low upper, in milliseconds.
     */
    Integer getBaselineLowUpper();

    /**
     * Baseline balanced lower, in milliseconds.
     */
    Integer getBaselineBalancedLower();

    /**
     * Baseline balanced upper, in milliseconds.
     */
    Integer getBaselineBalancedUpper();

    Status getStatus();
}
