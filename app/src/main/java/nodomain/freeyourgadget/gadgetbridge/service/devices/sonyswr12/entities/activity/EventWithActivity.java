/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.SonySWR12Util;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.IntFormat;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayReader;

public class EventWithActivity extends EventBase {
    public final long timeStampSec;
    public final List<ActivityBase> activityList;

    private EventWithActivity(long timeStampSec, List<ActivityBase> activityList) {
        super(EventCode.ACTIVITY_DATA);
        this.timeStampSec = timeStampSec;
        this.activityList = activityList;
    }

    public static EventWithActivity fromByteArray(ByteArrayReader byteArrayReader) {
        long timeOffset = byteArrayReader.readInt(IntFormat.UINT32);
        long timeStampSec = SonySWR12Util.secSince2013() + timeOffset;
        ArrayList<ActivityBase> activities = new ArrayList<>();
        while (byteArrayReader.getBytesLeft() > 0) {
            UIntBitReader uIntBitReader = new UIntBitReader(byteArrayReader.readInt(IntFormat.UINT32), 32);
            ActivityType activityType = ActivityType.fromInt(uIntBitReader.read(4));
            int offsetMin = uIntBitReader.read(12);
            ActivityBase activityPayload;
            switch (activityType) {
                case SLEEP: {
                    int duration = uIntBitReader.read(14);
                    SleepLevel sleepLevel = SleepLevel.fromInt(uIntBitReader.read(2));
                    activityPayload = new ActivitySleep(offsetMin, duration, sleepLevel, timeStampSec);
                    break;
                }
                case HEART_RATE: {
                    int bpm = uIntBitReader.read(16);
                    activityPayload = new ActivityHeartRate(offsetMin, bpm, timeStampSec);
                    break;
                }
                default: {
                    int data = uIntBitReader.read(16);
                    activityPayload = new ActivityWithData(activityType, offsetMin, data, timeStampSec);
                    break;
                }
            }
            activities.add(activityPayload);
        }
        return new EventWithActivity(timeStampSec, activities);
    }
}
