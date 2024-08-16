/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx.model;

import java.util.ArrayList;
import java.util.List;

public class GpxTrack {
    private final String name;

    private final String type;
    private final List<GpxTrackSegment> trackSegments;

    public GpxTrack(String name, String type, final List<GpxTrackSegment> trackSegments) {
        this.name = name;
        this.type = type;
        this.trackSegments = trackSegments;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<GpxTrackSegment> getTrackSegments() {
        return trackSegments;
    }

    public boolean isEmpty() {
        for (final GpxTrackSegment trackSegment : trackSegments) {
            if (!trackSegment.getTrackPoints().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public static class Builder {
        private String name;
        private String type;
        private final List<GpxTrackSegment> trackSegments = new ArrayList<>();

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Builder withTrackSegment(final GpxTrackSegment trackSegment) {
            trackSegments.add(trackSegment);
            return this;
        }

        public GpxTrack build() {
            return new GpxTrack(name, type, trackSegments);
        }
    }
}
