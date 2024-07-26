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
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.workout;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro.CmfActivityType;

public class CmfWorkoutSummaryParser implements ActivitySummaryParser {
    private final GBDevice gbDevice;

    public CmfWorkoutSummaryParser(final GBDevice device) {
        this.gbDevice = device;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary) {
        final ActivitySummaryData summaryData = new ActivitySummaryData();

        final ByteBuffer buf = ByteBuffer.wrap(summary.getRawSummaryData()).order(ByteOrder.LITTLE_ENDIAN);

        final int startTime = buf.getInt();
        final int duration = buf.getShort();
        final byte workoutType = buf.get();

        buf.get(new byte[19]); // ?
        final int endTime = buf.getInt();
        final boolean gps = buf.get() == 1;
        buf.get(); // ?

        summary.setStartTime(new Date(startTime * 1000L));
        summary.setEndTime(new Date(endTime * 1000L));

        final CmfActivityType cmfActivityType = CmfActivityType.fromCode(workoutType);
        if (cmfActivityType != null) {
            summary.setActivityKind(cmfActivityType.getActivityKind());
        } else {
            summary.setActivityKind(ActivityKind.TYPE_UNKNOWN);
        }

        summaryData.add(ACTIVE_SECONDS, duration, UNIT_SECONDS);

        return summary;
    }
}
