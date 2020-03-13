/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.util.Log;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class NotificationConfiguration implements Serializable {
    private short min, hour, subEye = -1;
    private String packageName, appName;
    private PlayNotificationRequest.VibrationType vibration;
    private boolean respectSilentMode;
    private long id = -1;

    NotificationConfiguration(short min, short hour, String packageName, String appName, boolean respectSilentMode, PlayNotificationRequest.VibrationType vibration) {
        this.min = min;
        this.hour = hour;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = respectSilentMode;
        this.vibration = vibration;
    }

    public NotificationConfiguration(short min, short hour, short subEye, PlayNotificationRequest.VibrationType vibration) {
        this.min = min;
        this.hour = hour;
        this.subEye = subEye;
        this.vibration = vibration;
    }

    public NotificationConfiguration(short min, short hour, String packageName, String appName, boolean respectSilentMode, PlayNotificationRequest.VibrationType vibration, long id) {
        this.min = min;
        this.hour = hour;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = respectSilentMode;
        this.vibration = vibration;
        this.id = id;
    }
    NotificationConfiguration(String packageName, String appName) {
        this.min = -1;
        this.hour = -1;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = false;
        this.vibration = PlayNotificationRequest.VibrationType.SINGLE_NORMAL;
        this.id = -1;
    }

    public PlayNotificationRequest.VibrationType getVibration() {
        return vibration;
    }

    public void setVibration(PlayNotificationRequest.VibrationType vibration) {
        this.vibration = vibration;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean getRespectSilentMode() {
        Log.d("Config", "respect: " + respectSilentMode);
        return respectSilentMode;
    }

    public void setRespectSilentMode(boolean respectSilentMode) {
        this.respectSilentMode = respectSilentMode;
    }

    public void setMin(short min) {
        this.min = min;
    }

    public void setHour(short hour) {
        this.hour = hour;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public short getMin() {
        return min;
    }

    public short getHour() {
        return hour;
    }

    public short getSubEye() {
        return subEye;
    }

    public void setSubEye(short subEye) {
        this.subEye = subEye;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }
}
