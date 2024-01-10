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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DataStructureFactory {

    private static final Logger logger = LoggerFactory.getLogger(DataStructureFactory.class.getSimpleName());
    private static final int HEADER_SIZE = 4;

    public List<WithingsStructure> createStructuresFromRawData(byte[] rawData) {
        List<WithingsStructure> structures = new ArrayList<>();
        if (rawData == null) {
            return structures;
        }

        List<byte[]> rawDataStructures = splitRawData(rawData);
        for (byte[] rawDataStructure : rawDataStructures) {
            WithingsStructure structure = null;

            short structureTypeFromResponse = (short) BLETypeConversions.toInt16(rawDataStructure[1], rawDataStructure[0]);

            switch (structureTypeFromResponse) {
                case WithingsStructureType.HR:
                    structure = new HeartRate();
                    break;
                case WithingsStructureType.LIVE_HR:
                    structure = new LiveHeartRate();
                    break;
                case WithingsStructureType.BATTERY_STATUS:
                    structure = new BatteryValues();
                    break;
                case WithingsStructureType.SCREEN_SETTINGS:
                    structure = new ScreenSettings();
                    break;
                case WithingsStructureType.ANCS_STATUS:
                    structure = new AncsStatus();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_TIME:
                    structure = new ActivitySampleTime();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_DURATION:
                    structure = new ActivitySampleDuration();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_MOVEMENT:
                    structure = new ActivitySampleMovement();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_CALORIES:
                    structure = new ActivitySampleCalories();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_CALORIES_2:
                    structure = new ActivitySampleCalories2();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_SLEEP:
                    structure = new ActivitySampleSleep();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_WALK:
                    structure = new ActivitySampleWalk();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_RUN:
                    structure = new ActivitySampleRun();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_SWIM:
                    structure = new ActivitySampleSwim();
                    break;
                case WithingsStructureType.ACTIVITY_HR:
                    structure = new ActivityHeartrate();
                    break;
                case WithingsStructureType.PROBE_REPLY:
                    structure = new ProbeReply();
                    break;
                case WithingsStructureType.CHALLENGE:
                    structure = new Challenge();
                    break;
                case WithingsStructureType.CHALLENGE_RESPONSE:
                    structure = new ChallengeResponse();
                    break;
                case WithingsStructureType.ACTIVITY_SAMPLE_UNKNOWN:
                    structure = new ActivitySampleUnknown();
                    break;
                case WithingsStructureType.END_OF_TRANSMISSION:
                    structure = new EndOfTransmission();
                    break;
                case WithingsStructureType.WORKOUT_TYPE:
                    structure = new WorkoutType();
                    break;
                case WithingsStructureType.LIVE_WORKOUT_START:
                    structure = new LiveWorkoutStart();
                    break;
                case WithingsStructureType.LIVE_WORKOUT_END:
                    structure = new LiveWorkoutEnd();
                    break;
                case WithingsStructureType.LIVE_WORKOUT_PAUSE_STATE:
                    structure = new LiveWorkoutPauseState();
                    break;
                case WithingsStructureType.WORKOUT_SCREEN_LIST:
                    structure = new WorkoutScreenList();
                    break;
                case WithingsStructureType.IMAGE_META_DATA:
                    structure = new ImageMetaData();
                    break;
                case WithingsStructureType.GLYPH_ID:
                    structure = new GlyphId();
                    break;
                case WithingsStructureType.NOTIFICATION_APP_ID:
                    structure = new GlyphId();
                    break;
                default:
                    structure = null;
                    logger.info("Received yet unknown structure type: " + structureTypeFromResponse);
            }

            if (structure != null) {
                structure.fillFromRawData(removeHeaderBytes(rawDataStructure));
                structures.add(structure);
            }
        }

        return structures;
    }

    private List<byte[]> splitRawData(byte[] rawData) {
        int remainingBytes = rawData.length;
        List<byte[]> result = new ArrayList<>();

        while(remainingBytes > 3) {
            short structureLength = (short) BLETypeConversions.toInt16(rawData[3], rawData[2]);
            remainingBytes -= (structureLength + HEADER_SIZE);
            try {
                result.add(Arrays.copyOfRange(rawData, 0, structureLength + HEADER_SIZE));
                if (remainingBytes > 0) {
                    rawData = Arrays.copyOfRange(rawData, structureLength + HEADER_SIZE, rawData.length);
                }
            } catch (Exception e) {
                logger.warn("Splitting of data failed: " + GB.hexdump(rawData));
            }
        }

        return result;
    }

    private byte[] removeHeaderBytes(byte[] data) {
        return Arrays.copyOfRange(data, HEADER_SIZE, data.length);
    }
}
