/*  Copyright (C) 2020-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.async;

import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class ConfirmAppStatusRequest extends JsonPutRequest {
    public ConfirmAppStatusRequest(int requestId, FossilHRWatchAdapter adapter) {
        super(createResponseObject(requestId), adapter);
    }

    private static JSONObject createResponseObject(int requestId){
        try {
            return new JSONObject()
                    .put("res",
                            new JSONObject()
                            .put("id", requestId)
                            .put("set", new JSONObject()
                                    .put("master._.config.app_status", new JSONObject()
                                            .put("message", "")
                                            .put("type", "success")
                                    )
                            )
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
