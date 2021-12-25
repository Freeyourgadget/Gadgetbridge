/*  Copyright (C) 2017-2020 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.model;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;

/**
 * Summarized information about a temporal activity.
 *
 * // TODO: split into separate entities?
 */
public interface ActivitySummary extends Serializable {
    Long getId();
    String getName();
    Date getStartTime();
    Date getEndTime();

    int getActivityKind();
    String getGpxTrack();

    long getDeviceId();

    long getUserId();
    String getSummaryData();
    //    long getSteps();
//    float getDistanceMeters();
//    float getAscentMeters();
//    float getDescentMeters();
//    float getMinAltitude();
//    float getMaxAltitude();
//    float getCalories();
//
//    float getMaxSpeed();
//    float getMinSpeed();
//    float getAverageSpeed();
}
