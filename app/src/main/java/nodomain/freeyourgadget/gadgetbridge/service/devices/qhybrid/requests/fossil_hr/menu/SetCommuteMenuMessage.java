/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.menu;

import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class SetCommuteMenuMessage extends JsonPutRequest {
    public SetCommuteMenuMessage(String message, boolean finished, FossilHRWatchAdapter adapter) {
        super(createObject(message, finished), adapter);
    }

    private static JSONObject createObject(String message, boolean finished) {
        try {
            return new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("commuteApp._.config.commute_info", new JSONObject()
                                            .put("message", message)
                                            .put("type", finished ? "end" : "in_progress")
                                    )
                            )
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
