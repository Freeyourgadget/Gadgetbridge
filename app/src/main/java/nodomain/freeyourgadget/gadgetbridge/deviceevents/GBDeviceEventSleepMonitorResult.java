/*  Copyright (C) 2015-2018 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventSleepMonitorResult extends GBDeviceEvent {
    // FIXME: this is just the low-level data from Morpheuz, we need something generic
    public int smartalarm_from = -1; // time in minutes relative from 0:00 for smart alarm (earliest)
    public int smartalarm_to = -1;// time in minutes relative from 0:00 for smart alarm (latest)
    public int recording_base_timestamp = -1; // timestamp for the first "point", all folowing are +10 minutes offset each
    public int alarm_gone_off = -1; // time in minutes relative from 0:00 when alarm gone off
}
