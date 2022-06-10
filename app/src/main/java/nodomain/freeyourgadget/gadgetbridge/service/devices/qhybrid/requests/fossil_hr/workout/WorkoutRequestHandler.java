/*  Copyright (C) 2022 Arjan Schrijver

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.workout;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;

public class WorkoutRequestHandler {
    public static void addStateResponse(JSONObject workoutResponse, String type, String msg) throws JSONException {
        workoutResponse.put("workoutApp._.config.response", new JSONObject()
                .put("message", msg)
                .put("type", type)
        );
    }

    public static JSONObject handleRequest(Context context, int requestId, JSONObject workoutRequest) throws JSONException {
        final Logger LOG = LoggerFactory.getLogger(WorkoutRequestHandler.class);

        JSONObject workoutResponse = new JSONObject();
        if (workoutRequest.optString("state").equals("started") && workoutRequest.optString("gps").equals("on")) {
            int activityType = workoutRequest.optInt("activity", -1);
            LOG.info("Workout started, activity type is " + activityType);
            addStateResponse(workoutResponse, "success", "");
            OpenTracksController.startRecording(context);
        } else if (workoutRequest.optString("type").equals("req_distance")) {
            long timeSecs = GBApplication.app().getOpenTracksObserver().getTimeMillisChange() / 1000;
            float distanceCM = GBApplication.app().getOpenTracksObserver().getDistanceMeterChange() * 100;
            LOG.info("Workout distance requested, returning " + distanceCM + " cm, " + timeSecs + " sec");
            workoutResponse.put("workoutApp._.config.gps", new JSONObject()
                    .put("distance", distanceCM)
                    .put("duration", timeSecs)
            );
        } else if (workoutRequest.optString("state").equals("paused")) {
            LOG.info("Workout paused");
            addStateResponse(workoutResponse, "success", "");
            // Pause OpenTracks recording?
        } else if (workoutRequest.optString("state").equals("resumed")) {
            LOG.info("Workout resumed");
            addStateResponse(workoutResponse, "success", "");
            // Resume OpenTracks recording?
        } else if (workoutRequest.optString("state").equals("end")) {
            LOG.info("Workout stopped");
            addStateResponse(workoutResponse, "success", "");
            OpenTracksController.stopRecording(context);
        } else if (workoutRequest.optString("type").equals("req_route")) {
            LOG.info("Workout route image requested, returning error");
            addStateResponse(workoutResponse, "error", "");
            // Send the traveled route as an RLE encoded image (example name: 58270405)
            // Send back a JSON packet, example:
            // {"res":{"id":21,"set":{"workoutApp._.config.images":{"session_id":1213693133,"route":{"name":"58270405"},"layout_type":"vertical"}}}}
            // or
            // {"res":{"id":34,"set":{"workoutApp._.config.images":{"session_id":504875,"route":{"name":"211631088"},"layout_type":"horizontal"}}}}
        } else {
            LOG.info("Request not recognized: " + workoutRequest);
        }
        return workoutResponse;
    }
}