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
