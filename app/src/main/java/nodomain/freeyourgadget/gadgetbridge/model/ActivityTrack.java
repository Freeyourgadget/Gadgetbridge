/*  Copyright (C) 2017-2018 Carsten Pfeiffer

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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;

public class ActivityTrack {
    private Date baseTime;
    private Device device;
    private User user;
    private String name;


    public void setBaseTime(Date baseTime) {
        this.baseTime = baseTime;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setTrackPoints(List<ActivityPoint> trackPoints) {
        this.trackPoints = trackPoints;
    }

    private List<ActivityPoint> trackPoints = new ArrayList<>();

    public void addTrackPoint(ActivityPoint point) {
        trackPoints.add(point);
    }

    public List<ActivityPoint> getTrackPoints() {
        return trackPoints;
    }

    public Date getBaseTime() {
        return baseTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
