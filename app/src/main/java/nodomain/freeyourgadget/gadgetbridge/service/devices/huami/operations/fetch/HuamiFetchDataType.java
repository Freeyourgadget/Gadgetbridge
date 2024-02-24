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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

public enum HuamiFetchDataType {
    ACTIVITY(0x01),
    MANUAL_HEART_RATE(0x02),
    SPORTS_SUMMARIES(0x05),
    SPORTS_DETAILS(0x06),
    DEBUG_LOGS(0x07),
    PAI(0x0d),
    STRESS_MANUAL(0x12),
    STRESS_AUTOMATIC(0x13),
    SPO2_NORMAL(0x25),
    SPO2_SLEEP(0x26),
    STATISTICS(0x2c),
    TEMPERATURE(0x2e),
    SLEEP_RESPIRATORY_RATE(0x38),
    RESTING_HEART_RATE(0x3a),
    MAX_HEART_RATE(0x3d),
    ;

    private final byte code;

    HuamiFetchDataType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
