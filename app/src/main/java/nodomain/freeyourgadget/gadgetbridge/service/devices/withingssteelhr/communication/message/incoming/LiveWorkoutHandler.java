/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.incoming;

import android.location.LocationManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.WithingsSteelHRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.activity.WithingsActivityType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.LiveWorkoutEnd;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.LiveWorkoutPauseState;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.LiveWorkoutStart;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructure;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructureType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WorkoutGpsState;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WorkoutType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.WithingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.WithingsMessageType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LiveWorkoutHandler implements IncomingMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(LiveWorkoutHandler.class);
    private final WithingsSteelHRDeviceSupport support;
    private BaseActivitySummary baseActivitySummary;

    public LiveWorkoutHandler(WithingsSteelHRDeviceSupport support) {
        this.support = support;
    }

    public void handleMessage(Message message) {
        List<WithingsStructure> data = message.getDataStructures();
        if (data != null) {
            handleLiveData(data);
        }
    }

    private void handleLiveData(List<WithingsStructure> dataList) {
        for (WithingsStructure data : dataList) {
            switch (data.getType()) {
                case WithingsStructureType.LIVE_WORKOUT_START:
                    handleStart((LiveWorkoutStart) data);
                    break;
                case WithingsStructureType.LIVE_WORKOUT_END:
                    handleEnd((LiveWorkoutEnd) data);
                    break;
                case WithingsStructureType.LIVE_WORKOUT_PAUSE_STATE:
                    handlePause((LiveWorkoutPauseState) data);
                    break;
                case WithingsStructureType.WORKOUT_TYPE:
                    handleType((WorkoutType) data);
                    break;
                default:
                    logger.info("Received yet unhandled live workout data of type '" + data.getType() + "' with data '" + GB.hexdump(data.getRawData()) + "'.");
            }
        }
    }

    private void handleStart(LiveWorkoutStart workoutStart) {
        sendGpsState();
        if (baseActivitySummary == null) {
            baseActivitySummary = new BaseActivitySummary();
        }

        baseActivitySummary.setStartTime(workoutStart.getStarttime());
    }

    private void handlePause(LiveWorkoutPauseState workoutPause) {
        // Not sure what to do with these events at the moment so we just log them.
        if (workoutPause.getStarttime() == null) {
            if (workoutPause.getLengthInSeconds() > 0) {
                logger.info("Got workout pause end with duration: " + workoutPause.getLengthInSeconds());
            } else {
                logger.info("Currently no pause happened");
            }
        } else {
            logger.info("Got workout pause started at: " + workoutPause.getStarttime());
        }
    }

    private void handleEnd(LiveWorkoutEnd workoutEnd) {
        OpenTracksController.stopRecording(support.getContext());
        baseActivitySummary.setEndTime(workoutEnd.getEndtime());
        saveBaseActivitySummary();
        baseActivitySummary = null;
    }

    private void handleType(WorkoutType workoutType) {
        WithingsActivityType withingsWorkoutType = WithingsActivityType.fromCode(workoutType.getActivityType());
        OpenTracksController.startRecording(support.getContext(), withingsWorkoutType.toActivityKind());
        if (baseActivitySummary == null) {
            baseActivitySummary = new BaseActivitySummary();
        }

        baseActivitySummary.setActivityKind(withingsWorkoutType.toActivityKind());
    }

    private void sendGpsState() {
        Message message = new WithingsMessage((short)(WithingsMessageType.START_LIVE_WORKOUT | 16384), new WorkoutGpsState(isGpsEnabled()));
        support.sendToDevice(message);
    }

    private boolean isGpsEnabled() {
        final LocationManager manager = (LocationManager) support.getContext().getSystemService(support.getContext().LOCATION_SERVICE );
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER );
    }

    private void saveBaseActivitySummary() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Device device = DBHelper.getDevice(support.getDevice(), session);
            User user = DBHelper.getUser(session);
            baseActivitySummary.setDevice(device);
            baseActivitySummary.setUser(user);
            session.getBaseActivitySummaryDao().insertOrReplace(baseActivitySummary);
        } catch (Exception ex) {
            GB.toast(support.getContext(), "Error saving activity summary", Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }
}
