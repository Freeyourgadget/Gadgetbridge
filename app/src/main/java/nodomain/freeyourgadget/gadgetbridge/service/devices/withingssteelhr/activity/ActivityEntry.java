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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.activity;

public class ActivityEntry {
    private int timestamp;
    private int duration;
    private int rawKind = -1;
    private int heartrate;
    private int steps;
    private int calories;
    private int distance;
    private int rawIntensity;
    private boolean isHeartrate;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getHeartrate() {
        return heartrate;
    }

    public void setIsHeartrate(int heartrate) {
        this.heartrate = heartrate;
    }

    public int getRawKind() {
        return rawKind;
    }

    public void setRawKind(int rawKind) {
        this.rawKind = rawKind;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getRawIntensity() {
        return rawIntensity;
    }

    public void setRawIntensity(int rawIntensity) {
        this.rawIntensity = rawIntensity;
    }

    public boolean isHeartrate() {
        return isHeartrate;
    }

    public void setIsHeartrate(boolean heartrate) {
        isHeartrate = heartrate;
    }
}
