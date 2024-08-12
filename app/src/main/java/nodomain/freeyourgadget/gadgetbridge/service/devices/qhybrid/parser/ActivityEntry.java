/*  Copyright (C) 2020-2024 Andreas Shimokawa, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser;

import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class ActivityEntry {
    public int id;
    public int heartRate;
    public int variability, maxVariability;
    public int calories;
    public int stepCount;
    public boolean isActive;

    public int timestamp;

    public int heartRateQuality;

    public WEARING_STATE wearingState;

    public HybridHRActivitySample toDAOActivitySample(long userId, long deviceId) {
        return new HybridHRActivitySample(
                timestamp,
                deviceId,
                userId,
                stepCount,
                calories,
                variability,
                maxVariability,
                heartRateQuality,
                isActive,
                wearingState.value,
                heartRate
        );
    }

    public enum WEARING_STATE{
        WEARING((byte) 0, ActivityKind.NOT_MEASURED),
        NOT_WEARING((byte) 1, ActivityKind.NOT_WORN),
        UNKNOWN((byte) 2, ActivityKind.UNKNOWN);

        final byte value;
        final ActivityKind activityKind;

        WEARING_STATE(byte value, ActivityKind activityKind){
            this.value = value;
            this.activityKind = activityKind;
        }

        public ActivityKind getActivityKind() {
            return activityKind;
        }

        static public WEARING_STATE fromValue(byte value){
            switch (value){
                case 0: return WEARING_STATE.WEARING;
                case 1: return WEARING_STATE.NOT_WEARING;
                case 2: return WEARING_STATE.UNKNOWN;
                default: throw new RuntimeException("value " + value + " not valid state value");
            }
        }
    }
}
