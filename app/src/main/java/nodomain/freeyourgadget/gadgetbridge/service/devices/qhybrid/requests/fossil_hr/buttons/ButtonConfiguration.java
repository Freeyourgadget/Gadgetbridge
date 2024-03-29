/*  Copyright (C) 2020-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons;

import org.json.JSONException;
import org.json.JSONObject;

public class ButtonConfiguration {
    private String triggerEvent;
    private String action;

    public ButtonConfiguration(String triggerEvent, String action) {
        this.triggerEvent = triggerEvent;
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public JSONObject toJsonObject(){
        try {
            return new JSONObject()
                    .put("button_evt", triggerEvent)
                    .put("name", action);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
