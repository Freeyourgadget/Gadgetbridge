/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.colmi;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiSleepSessionSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepSessionSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.colmi.ColmiR0xDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ColmiR0xPacketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ColmiR0xPacketHandler.class);

    public static void hrIntervalSettings(ColmiR0xDeviceSupport support, byte[] value) {
        if (value[1] == ColmiR0xConstants.PREF_WRITE) return;  // ignore empty response when writing setting
        boolean enabled = value[2] == 0x01;
        int minutes = value[3];
        LOG.info("Received HR interval preference: {} minutes, enabled={}", minutes, enabled);
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL,
                String.valueOf(minutes * 60)
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void spo2Settings(ColmiR0xDeviceSupport support, byte[] value) {
        boolean enabled = value[2] == 0x01;
        LOG.info("Received SpO2 preference: {}", enabled ? "enabled" : "disabled");
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING,
                enabled
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void stressSettings(ColmiR0xDeviceSupport support, byte[] value) {
        boolean enabled = value[2] == 0x01;
        LOG.info("Received stress preference: {}", enabled ? "enabled" : "disabled");
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING,
                enabled
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void goalsSettings(byte[] value) {
        int steps = BLETypeConversions.toUint32(value[2], value[3], value[4], (byte) 0);
        int calories = BLETypeConversions.toUint32(value[5], value[6], value[7], (byte) 0);
        int distance = BLETypeConversions.toUint32(value[8], value[9], value[10], (byte) 0);
        int sport = BLETypeConversions.toUint16(value[11], value[12]);
        int sleep = BLETypeConversions.toUint16(value[13], value[14]);
        LOG.info("Received goals preferences: {} steps, {} calories, {}m distance, {}min sport, {}min sleep", steps, calories, distance, sport, sleep);
    }

    public static void liveHeartRate(GBDevice device, Context context, byte[] value) {
        int errorCode = value[2];
        int hrResponse = value[3] & 0xff;
        switch (errorCode) {
            case 0:
                LOG.info("Received live heart rate response: {} bpm", hrResponse);
                break;
            case 1:
                GB.toast(context.getString(R.string.smart_ring_measurement_error_worn_incorrectly), Toast.LENGTH_LONG, GB.ERROR);
                LOG.warn("Live HR error code {} received from ring", errorCode);
                return;
            case 2:
                LOG.warn("Live HR error 2 (temporary error / missing data) received");
                return;
            default:
                GB.toast(String.format(context.getString(R.string.smart_ring_measurement_error_unknown), errorCode), Toast.LENGTH_LONG, GB.ERROR);
                LOG.warn("Live HR error code {} received from ring", errorCode);
                return;
        }
        if (hrResponse > 0) {
            try (DBHandler db = GBApplication.acquireDB()) {
                // Build sample object and save in database
                ColmiHeartRateSampleProvider sampleProvider = new ColmiHeartRateSampleProvider(device, db.getDaoSession());
                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
                ColmiHeartRateSample gbSample = new ColmiHeartRateSample();
                gbSample.setDeviceId(deviceId);
                gbSample.setUserId(userId);
                gbSample.setTimestamp(Calendar.getInstance().getTimeInMillis());
                gbSample.setHeartRate(hrResponse);
                sampleProvider.addSample(gbSample);
                // Send local intent with sample for listeners like the heart rate dialog
                Intent liveIntent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES);
                liveIntent.putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, gbSample);
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(liveIntent);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording heart rate samples", e);
            }
        }
    }

    public static void liveActivity(byte[] value) {
        int steps = BLETypeConversions.toUint32(value[4], value[3], value[2], (byte) 0);
        int calories = BLETypeConversions.toUint32(value[7], value[6], value[5], (byte) 0) / 10;
        int distance = BLETypeConversions.toUint32(value[10], value[9], value[8], (byte) 0);
        LOG.info("Received live activity notification: {} steps, {} calories, {}m distance", steps, calories, distance);
    }

    public static void historicalActivity(GBDevice device, Context context, byte[] value) {
        if ((value[1] & 0xff) == 0xff) {
            device.unsetBusyTask();
            device.sendDeviceUpdateIntent(context);
            LOG.info("Empty activity history, sync aborted");
        } else if ((value[1] & 0xff) == 0xf0) {
            // initial packet, doesn't contain anything interesting
        } else {
            // Unpack timestamp and data
            Calendar sampleCal = Calendar.getInstance();
            // The code below converts the raw hex value to a date. That seems wrong, but is correct,
            // because this date is for some reason transmitted as ints used as literal bytes:
            // A date like 2024-08-18 would be transmitted as 0x24 0x08 0x18.
            sampleCal.set(Calendar.YEAR, 2000 + Integer.valueOf(String.format("%02x", value[1])));
            sampleCal.set(Calendar.MONTH, Integer.valueOf(String.format("%02x", value[2])) - 1);
            sampleCal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(String.format("%02x", value[3])));
            sampleCal.set(Calendar.HOUR_OF_DAY, value[4] / 4);  // And the hour is transmitted as nth quarter of the day...
            sampleCal.set(Calendar.MINUTE, 0);
            sampleCal.set(Calendar.SECOND, 0);
            int calories = BLETypeConversions.toUint16(value[7], value[8]);
            int steps = BLETypeConversions.toUint16(value[9], value[10]);
            int distance = BLETypeConversions.toUint16(value[11], value[12]);
            LOG.info("Received activity sample: {} - {} calories, {} steps, {} distance", sampleCal.getTime(), calories, steps, distance);
            // Build sample object and save in database
            try (DBHandler db = GBApplication.acquireDB()) {
                ColmiActivitySampleProvider sampleProvider = new ColmiActivitySampleProvider(device, db.getDaoSession());
                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
                ColmiActivitySample gbSample = sampleProvider.createActivitySample();
                gbSample.setProvider(sampleProvider);
                gbSample.setDeviceId(deviceId);
                gbSample.setUserId(userId);
                gbSample.setRawKind(ActivityKind.ACTIVITY.getCode());
                gbSample.setTimestamp((int) (sampleCal.getTimeInMillis() / 1000));
                gbSample.setCalories(calories);
                gbSample.setSteps(steps);
                gbSample.setDistance(distance);
                sampleProvider.addGBActivitySample(gbSample);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording activity samples", e);
            }
            // Determine if this sync is done
            int currentActivityPacket = value[5];
            int totalActivityPackets = value[6];
            if (currentActivityPacket == totalActivityPackets - 1) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(context);
            }
        }
    }

    public static void historicalStress(GBDevice device, Context context, byte[] value) {
        ArrayList<ColmiStressSample> stressSamples = new ArrayList<>();
        int stressPacketNr = value[1] & 0xff;
        if (stressPacketNr == 0xff) {
            device.unsetBusyTask();
            device.sendDeviceUpdateIntent(context);
            LOG.info("Empty stress history, sync aborted");
        } else if (stressPacketNr == 0) {
            LOG.info("Received initial stress history response");
        } else {
            Calendar sampleCal = Calendar.getInstance();
            int startValue = stressPacketNr == 1 ? 3 : 2;  // packet 1 data starts at byte 3, others at byte 2
            int minutesInPreviousPackets = 0;
            if (stressPacketNr > 1) {
                // 30 is the interval in minutes between values/measurements
                minutesInPreviousPackets = 12 * 30;  // 12 values in packet 1
                minutesInPreviousPackets += (stressPacketNr - 2) * 13 * 30;  // 13 values per packet
            }
            for (int i = startValue; i < value.length - 1; i++) {
                if (value[i] != 0x00) {
                    // Determine time of day
                    int minuteOfDay = minutesInPreviousPackets + (i - startValue) * 30;
                    sampleCal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
                    sampleCal.set(Calendar.MINUTE, minuteOfDay % 60);
                    LOG.info("Stress level is {} at {}", value[i] & 0xff, sampleCal.getTime());
                    // Build sample object and save in database
                    ColmiStressSample gbSample = new ColmiStressSample();
                    gbSample.setTimestamp(sampleCal.getTimeInMillis());
                    gbSample.setStress(value[i] & 0xff);
                    stressSamples.add(gbSample);
                }
            }
            if (!stressSamples.isEmpty()) {
                try (DBHandler db = GBApplication.acquireDB()) {
                    ColmiStressSampleProvider sampleProvider = new ColmiStressSampleProvider(device, db.getDaoSession());
                    Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                    Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
                    for (final ColmiStressSample sample : stressSamples) {
                        sample.setDeviceId(deviceId);
                        sample.setUserId(userId);
                    }
                    LOG.info("Will persist {} stress samples", stressSamples.size());
                    sampleProvider.addSamples(stressSamples);
                } catch (Exception e) {
                    LOG.error("Error acquiring database for recording stress samples", e);
                }
            }
            if (stressPacketNr == 4) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(context);
            }
        }
    }

    public static void historicalSpo2(GBDevice device, byte[] value) {
        ArrayList<ColmiSpo2Sample> spo2Samples = new ArrayList<>();
        int length = BLETypeConversions.toUint16(value[2], value[3]);
        int index = 6; // start of data (day nr, followed by values)
        int spo2_days_ago = -1;
        while (spo2_days_ago != 0 && index - 6 < length) {
            spo2_days_ago = value[index];
            Calendar syncingDay = Calendar.getInstance();
            syncingDay.add(Calendar.DAY_OF_MONTH, 0 - spo2_days_ago);
            syncingDay.set(Calendar.MINUTE, 0);
            syncingDay.set(Calendar.SECOND, 0);
            index++;
            for (int hour=0; hour<=23; hour++) {
                syncingDay.set(Calendar.HOUR_OF_DAY, hour);
                float spo2_min = value[index];
                index++;
                float spo2_max = value[index];
                index++;
                if (spo2_min > 0 && spo2_max > 0) {
                    LOG.info("Received SpO2 data from {} days ago at {}:00: min={}, max={}", spo2_days_ago, hour, spo2_min, spo2_max);
                    ColmiSpo2Sample spo2Sample = new ColmiSpo2Sample();
                    spo2Sample.setTimestamp(syncingDay.getTimeInMillis());
                    spo2Sample.setSpo2(Math.round((spo2_min + spo2_max) / 2.0f));
                    spo2Samples.add(spo2Sample);
                }
                if (index - 6 >= length) {
                    break;
                }
            }
        }
        if (!spo2Samples.isEmpty()) {
            try (DBHandler db = GBApplication.acquireDB()) {
                ColmiSpo2SampleProvider sampleProvider = new ColmiSpo2SampleProvider(device, db.getDaoSession());
                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
                for (final ColmiSpo2Sample sample : spo2Samples) {
                    sample.setDeviceId(deviceId);
                    sample.setUserId(userId);
                }
                LOG.info("Will persist {} SpO2 samples", spo2Samples.size());
                sampleProvider.addSamples(spo2Samples);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording SpO2 samples", e);
            }
        }
    }

    public static void historicalSleep(GBDevice gbDevice, Context context, byte[] value) {
        int packetLength = BLETypeConversions.toUint16(value[2], value[3]);
        if (packetLength < 2) {
            LOG.info("Received empty sleep data packet: {}", StringUtils.bytesToHex(value));
        } else {
            int daysInPacket = value[6];
            LOG.debug("Received sleep data packet for {} days: {}", daysInPacket, StringUtils.bytesToHex(value));
            int index = 7;
            for (int i = 1; i <= daysInPacket; i++) {
                // Parse sleep session
                int daysAgo = value[index];
                index++;
                int dayBytes = value[index];
                index++;
                int sleepStart = BLETypeConversions.toUint16(value[index], value[index + 1]);
                index += 2;
                int sleepEnd = BLETypeConversions.toUint16(value[index], value[index + 1]);
                index += 2;
                // Calculate sleep start timestamp
                Calendar sessionStart = Calendar.getInstance();
                sessionStart.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
                sessionStart.set(Calendar.HOUR_OF_DAY, 0);
                sessionStart.set(Calendar.MINUTE, 0);
                sessionStart.set(Calendar.SECOND, 0);
                if (sleepStart > sleepEnd) {
                    // Sleep started a day earlier, so before midnight
                    sessionStart.add(Calendar.DAY_OF_MONTH, -1);
                    sessionStart.add(Calendar.MINUTE, sleepStart);
                } else {
                    // Sleep started this day, so after midnight
                    sessionStart.add(Calendar.MINUTE, sleepStart);
                }
                // Calculate sleep end timestamp
                Calendar sessionEnd = Calendar.getInstance();
                sessionEnd.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
                sessionEnd.set(Calendar.HOUR_OF_DAY, 0);
                sessionEnd.set(Calendar.MINUTE, sleepEnd);
                sessionEnd.set(Calendar.SECOND, 0);
                LOG.info("Sleep session starts at {} and ends at {}", sessionStart.getTime(), sessionEnd.getTime());
                // Build sample object to persist
                final ColmiSleepSessionSample sessionSample = new ColmiSleepSessionSample();
                sessionSample.setTimestamp(sessionStart.getTimeInMillis());
                sessionSample.setWakeupTime(sessionEnd.getTimeInMillis());
                // Handle sleep stages
                final List<ColmiSleepStageSample> stageSamples = new ArrayList<>();
                Calendar sleepStage = (Calendar) sessionStart.clone();
                for (int j = 4; j < dayBytes; j += 2) {
                    int sleepMinutes = value[index + 1];
                    LOG.info("Sleep stage type={} starts at {} and lasts for {} minutes", value[index], sleepStage.getTime(), sleepMinutes);
                    final ColmiSleepStageSample sample = new ColmiSleepStageSample();
                    sample.setTimestamp(sleepStage.getTimeInMillis());
                    sample.setDuration(value[index + 1]);
                    sample.setStage(value[index]);
                    stageSamples.add(sample);
                    // Prepare for next sample
                    index += 2;
                    sleepStage.add(Calendar.MINUTE, sleepMinutes);
                }
                // Persist sleep session
                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();

                    final Device device = DBHelper.getDevice(gbDevice, session);
                    final User user = DBHelper.getUser(session);

                    final ColmiSleepSessionSampleProvider sampleProvider = new ColmiSleepSessionSampleProvider(gbDevice, session);

                    sessionSample.setDevice(device);
                    sessionSample.setUser(user);

                    LOG.debug("Will persist 1 sleep session sample from {} to {}", sessionSample.getTimestamp(), sessionSample.getWakeupTime());
                    sampleProvider.addSample(sessionSample);
                } catch (final Exception e) {
                    GB.toast(context, "Error saving sleep session sample", Toast.LENGTH_LONG, GB.ERROR, e);
                }
                // Persist sleep stages
                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();

                    final Device device = DBHelper.getDevice(gbDevice, session);
                    final User user = DBHelper.getUser(session);

                    final ColmiSleepStageSampleProvider sampleProvider = new ColmiSleepStageSampleProvider(gbDevice, session);

                    for (final ColmiSleepStageSample sample : stageSamples) {
                        sample.setDevice(device);
                        sample.setUser(user);
                    }

                    LOG.debug("Will persist {} sleep stage samples", stageSamples.size());
                    sampleProvider.addSamples(stageSamples);
                } catch (final Exception e) {
                    GB.toast(context, "Error saving sleep stage samples", Toast.LENGTH_LONG, GB.ERROR, e);
                }
            }
        }
    }
}
