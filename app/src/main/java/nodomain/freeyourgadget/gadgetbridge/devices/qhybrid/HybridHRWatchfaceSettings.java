/*  Copyright (C) 2021-2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import java.io.Serializable;

public class HybridHRWatchfaceSettings implements Serializable {
    private int displayTimeoutFull = 60;
    private int displayTimeoutPartial = 15;
    private boolean wristFlickHandsMoveRelative = true;
    private int wristFlickDuration = 2200;
    private int wristFlickMoveHour = 360;
    private int wristFlickMoveMinute = -360;
    private String toggleWidgetsEvent = "";
    private String toggleBacklightEvent = "";
    private String moveHandsEvent = "flick_away";
    private boolean powersaveDisplay = false;
    private boolean powersaveHands = false;
    private boolean lightUpOnNotification = false;

    public HybridHRWatchfaceSettings() {
    }

    public int getDisplayTimeoutFull() {
        return displayTimeoutFull;
    }

    public void setDisplayTimeoutFull(int displayTimeoutFull) {
        this.displayTimeoutFull = displayTimeoutFull;
    }

    public int getDisplayTimeoutPartial() {
        return displayTimeoutPartial;
    }

    public void setDisplayTimeoutPartial(int displayTimeoutPartial) {
        this.displayTimeoutPartial = displayTimeoutPartial;
    }

    public boolean isWristFlickHandsMoveRelative() {
        return wristFlickHandsMoveRelative;
    }

    public void setWristFlickHandsMoveRelative(boolean wristFlickHandsMoveRelative) {
        this.wristFlickHandsMoveRelative = wristFlickHandsMoveRelative;
    }

    public int getWristFlickDuration() {
        return wristFlickDuration;
    }

    public void setWristFlickDuration(int wristFlickDuration) {
        this.wristFlickDuration = wristFlickDuration;
    }

    public boolean getLightUpOnNotification() {
        return lightUpOnNotification;
    }

    public void setLightUpOnNotification(boolean lightUpOnNotification) {
        this.lightUpOnNotification = lightUpOnNotification;
    }

    public int getWristFlickMoveHour() {
        return wristFlickMoveHour;
    }

    public void setWristFlickMoveHour(int wristFlickMoveHour) {
        if (wristFlickMoveHour < -360) {
            this.wristFlickMoveHour = -360;
        } else if (wristFlickMoveHour > 360) {
            this.wristFlickMoveHour = 360;
        } else {
            this.wristFlickMoveHour = wristFlickMoveHour;
        }
    }

    public int getWristFlickMoveMinute() {
        return wristFlickMoveMinute;
    }

    public void setWristFlickMoveMinute(int wristFlickMoveMinute) {
        if (wristFlickMoveMinute < -360) {
            this.wristFlickMoveMinute = -360;
        } else if (wristFlickMoveMinute > 360) {
            this.wristFlickMoveMinute = 360;
        } else {
            this.wristFlickMoveMinute = wristFlickMoveMinute;
        }
    }

    public String getToggleWidgetsEvent() {
        return toggleWidgetsEvent;
    }

    public void setToggleWidgetsEvent(String event) {
        this.toggleWidgetsEvent = event;
    }

    public String getToggleBacklightEvent() {
        return toggleBacklightEvent;
    }

    public void setToggleBacklightEvent(String event) {
        this.toggleBacklightEvent = event;
    }

    public String getMoveHandsEvent() {
        return moveHandsEvent;
    }

    public void setMoveHandsEvent(String event) {
        this.moveHandsEvent = event;
    }

    public boolean getPowersaveDisplay() {
        return powersaveDisplay;
    }

    public void setPowersaveDisplay(boolean powersaveDisplay) {
        this.powersaveDisplay = powersaveDisplay;
    }

    public boolean getPowersaveHands() {
        return powersaveHands;
    }

    public void setPowersaveHands(boolean powersaveHands) {
        this.powersaveHands = powersaveHands;
    }
}
