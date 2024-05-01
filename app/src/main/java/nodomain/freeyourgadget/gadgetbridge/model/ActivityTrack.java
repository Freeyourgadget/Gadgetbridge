/*  Copyright (C) 2017-2024 Carsten Pfeiffer, José Rebelo

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;

public class ActivityTrack {
    private Date baseTime;
    private Device device;
    private User user;
    private String name;
    private List<ActivityPoint> currentSegment = new ArrayList<>();
    private List<List<ActivityPoint>> segments = new ArrayList<List<ActivityPoint>>() {{
        add(currentSegment);
    }};

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

    /**
     * Add a track point to the current segment.
     */
    public void addTrackPoint(final ActivityPoint point) {
        currentSegment.add(point);
    }

    public void addTrackPoints(final Collection<ActivityPoint> points) {
        currentSegment.addAll(points);
    }

    public void startNewSegment() {
        // Only really start a new segment if the current one is not empty
        if (!currentSegment.isEmpty()) {
            currentSegment = new ArrayList<>();
            segments.add(currentSegment);
        }
    }

    public List<List<ActivityPoint>> getSegments() {
        return segments;
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
