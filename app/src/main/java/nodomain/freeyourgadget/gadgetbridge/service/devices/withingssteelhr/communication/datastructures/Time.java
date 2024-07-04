/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.TimeZone;

public class Time extends WithingsStructure {

    private Instant now;
    private int timeOffsetInSeconds;
    private Instant nextDaylightSavingTransition;
    private int nextDaylightSavingTransitionOffsetInSeconds;

    public Time() {
        now = Instant.now();
        final TimeZone timezone = TimeZone.getDefault();
        timeOffsetInSeconds = timezone.getOffset(now.toEpochMilli()) / 1000;
        final ZoneId zoneId = ZoneId.systemDefault();
        final ZoneRules zoneRules = zoneId.getRules();
        final ZoneOffsetTransition nextTransition = zoneRules.nextTransition(Instant.now());
        long nextTransitionTs = 0;
        if (nextTransition != null) {
            nextTransitionTs = nextTransition.getDateTimeBefore().atZone(zoneId).toEpochSecond();
            nextDaylightSavingTransitionOffsetInSeconds = nextTransition.getOffsetAfter().getTotalSeconds();
        }

        nextDaylightSavingTransition = Instant.ofEpochSecond(nextTransitionTs);
    }

    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }

    public int getTimeOffsetInSeconds() {
        return timeOffsetInSeconds;
    }

    public void setTimeOffsetInSeconds(int TimeOffsetInSeconds) {
        this.timeOffsetInSeconds = TimeOffsetInSeconds;
    }

    public Instant getNextDaylightSavingTransition() {
        return nextDaylightSavingTransition;
    }

    public void setNextDaylightSavingTransition(Instant nextDaylightSavingTransition) {
        this.nextDaylightSavingTransition = nextDaylightSavingTransition;
    }

    public int getNextDaylightSavingTransitionOffsetInSeconds() {
        return nextDaylightSavingTransitionOffsetInSeconds;
    }

    public void setNextDaylightSavingTransitionOffsetInSeconds(int nextDaylightSavingTransitionOffsetInSeconds) {
        this.nextDaylightSavingTransitionOffsetInSeconds = nextDaylightSavingTransitionOffsetInSeconds;
    }

    @Override
    public short getType() {
        return WithingsStructureType.TIME;
    }

    @Override
    public short getLength() {
        return 20;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer rawDataBuffer) {
        rawDataBuffer.putInt((int)now.getEpochSecond());
        rawDataBuffer.putInt(timeOffsetInSeconds);
        rawDataBuffer.putInt((int)nextDaylightSavingTransition.getEpochSecond());
        rawDataBuffer.putInt(nextDaylightSavingTransitionOffsetInSeconds);
    }
}
