package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class WatchXPlusSampleProvider extends AbstractSampleProvider<WatchXPlusActivitySample> {
    private GBDevice mDevice;
    private DaoSession mSession;

    private final float movementDivisor = 1500.0f;

    private static final Logger LOG = LoggerFactory.getLogger(WatchXPlusSampleProvider.class);

    public WatchXPlusSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
        mSession = session;
        mDevice = device;
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        LOG.info(" toRawActivityKind: " + activityKind);
        return activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        float newIntensity = 0;
        if (rawIntensity <= 0) {
            //newIntensity = (rawIntensity * 0.5f) + 0.7f;
            newIntensity = rawIntensity;
        } else {
            newIntensity = rawIntensity / movementDivisor;
        }
        //LOG.info(" normalizeIntensity: " + rawIntensity + " to " + newIntensity);
        return newIntensity;
        //return rawIntensity;
    }

    @Override
    public WatchXPlusActivitySample createActivitySample() {
        return new WatchXPlusActivitySample();
    }

    @Override
    public AbstractDao<WatchXPlusActivitySample, ?> getSampleDao() {
        return getSession().getWatchXPlusActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return WatchXPlusActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return WatchXPlusActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return WatchXPlusActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public List<WatchXPlusActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        boolean showRawData = GBApplication.getDeviceSpecificSharedPrefs(mDevice.getAddress()).getBoolean(WatchXPlusConstants.PREF_SHOW_RAW_GRAPH, false);
        if (showRawData) {
            return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
        }
        List<WatchXPlusActivitySample> samples = getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
        int numEntries = samples.size();

      //  LOG.info(" testing: ts_from:" + timestamp_from + " ts_to: " + timestamp_to);

    /*
        LOG.info(" testing: samples: ");
        for (int i = 0; i < numEntries; i++) {
            LOG.info(" s: " + i + " : " + samples.get(i).toString());
        }
*/
        List<WatchXPlusActivitySample> resultList = new ArrayList<>(numEntries);

        // how many elements to scan for sleep sate before and after sleep block
        int seekAhead = 10;
        boolean secondBlock = false;

// find sleep start and sleep stop index based on ActivityKind.TYPE_DEEP_SLEEP BLOCK 1
        int sleepStartIndex_1 = 0;
        int sleepStopIndex_1 = numEntries;
        int countNextSleepStart_1 = 0;
        int countNextSleepStop_1 = 0;

        for (int i = 0; i < numEntries; i++) {
            if (samples.get(i).getRawKind() == ActivityKind.TYPE_DEEP_SLEEP) {
                // normalize RawIntensity
                samples.get(i).setRawIntensity(1000);
                // find sleep start index
                if (sleepStartIndex_1 == 0) {
                    sleepStartIndex_1 = i;
                    sleepStopIndex_1 = sleepStartIndex_1;
                    countNextSleepStop_1 = sleepStopIndex_1;
                } else {
                    if (countNextSleepStart_1 == 0) {
                        countNextSleepStart_1 = i;
                        // reset start index if next index is far ahead
                        if ((countNextSleepStart_1 - sleepStartIndex_1) > seekAhead * 2) {
                            sleepStartIndex_1 = countNextSleepStart_1;
                            sleepStopIndex_1 = sleepStartIndex_1;
                            countNextSleepStop_1 = sleepStopIndex_1;
                        }
                    }
                }


                        if ((i - sleepStopIndex_1) < (seekAhead * 3)) {
                            sleepStopIndex_1 = i;
                        }
                        countNextSleepStop_1 = i;
            }
        }

// find sleep start and sleep stop index based on ActivityKind.TYPE_DEEP_SLEEP BLOCK 2
        int sleepStartIndex_2 = 0;
        int sleepStopIndex_2 = numEntries;
        int countNextSleepStart_2 = 0;
        int countNextSleepStop_2 = 0;
        int next_block = numEntries;

        for (int i = sleepStopIndex_1 + 1; i < numEntries; i++) {
            if (samples.get(i).getRawKind() == ActivityKind.TYPE_DEEP_SLEEP) {
                // normalize RawIntensity
                samples.get(i).setRawIntensity(1000);
                // find sleep start index
                if (sleepStartIndex_2 == 0) {
                    sleepStartIndex_2 = i;
                    sleepStopIndex_2 = sleepStartIndex_2;
                    countNextSleepStop_2 = sleepStopIndex_2;
                } else {
                    if (countNextSleepStart_2 == 0) {
                        countNextSleepStart_2 = i;
                        // reset start index if next index is far ahead
                        if ((countNextSleepStart_2 - sleepStartIndex_2) > seekAhead * 2) {
                            sleepStartIndex_2 = countNextSleepStart_2;
                            sleepStopIndex_2 = sleepStartIndex_2;
                            countNextSleepStop_2 = sleepStopIndex_2;
                        }
                    }
                }
                if ((i - sleepStopIndex_2) < (seekAhead * 3)) {
                    sleepStopIndex_2 = i;
                }
                countNextSleepStop_2 = i;
            }
        }
        if (sleepStartIndex_2 != 0) {
            secondBlock = true;
            LOG.info(" second block ");
        }

        LOG.info(" sleep_1 begin index:" + sleepStartIndex_1 + " next index: " + countNextSleepStart_1 + " sleep end index: " + sleepStopIndex_1 + " sleep end: " + countNextSleepStop_1);
        if (secondBlock) {
            LOG.info(" sleep_2 begin index:" + sleepStartIndex_2 + " next index: " + countNextSleepStart_2 + " sleep end index: " + sleepStopIndex_2 + " sleep end: " + countNextSleepStop_2);
        }

// SLEEP BLOCK 1
        // add all activity before sleep start
        if (secondBlock) {
            next_block = sleepStartIndex_2;
        }
        int newSleepStartIndex_1 = 0;
        if (sleepStartIndex_1 >= seekAhead) {
          newSleepStartIndex_1 = sleepStartIndex_1 - seekAhead;
        } else {
            newSleepStartIndex_1 = 0;
        }
        for (int i = 0; i < newSleepStartIndex_1; i++) {
            if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                if (samples.get(i).getRawIntensity() <= 300) {
                    samples.get(i).setRawIntensity(200);
                } else if ((samples.get(i).getRawIntensity() <= 1000) && (samples.get(i).getRawIntensity() > 100)) {
                    samples.get(i).setRawIntensity(400);
                } if (samples.get(i).getRawIntensity() > 1000) {
                    samples.get(i).setRawIntensity(600);
                }
                samples.get(i).setRawKind(1);
                resultList.add(samples.get(i));
            } else {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) {
                    if (i < (newSleepStartIndex_1 - 3)) {
                        if ((samples.get(i + 1).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) || (samples.get(i + 2).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) || (samples.get(i + 3).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP)) {
                            samples.get(i).setRawKind(1);
                            //samples.get(i).setRawIntensity(700);
                        } else {
                            samples.get(i).setRawIntensity(1000);
                        }
                    }
                    //samples.get(i).setRawIntensity(1000);
                } else {
                    samples.get(i).setRawIntensity(1000);
                }
                resultList.add(samples.get(i));
            }
        }

// add sleep activity
        int newSleepStopIndex_1;

        if ((sleepStopIndex_1 + seekAhead) < next_block) {
            newSleepStopIndex_1 = sleepStopIndex_1 + seekAhead * 2;
        } else {
            newSleepStopIndex_1 = next_block;
        }

        boolean replaceActivity_1 = false;
        for (int i = newSleepStartIndex_1; i < newSleepStopIndex_1; i++) {
            ActivitySample sample = samples.get(i);
            if (i < sleepStartIndex_1) {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    replaceActivity_1 = true;
                    samples.get(i).setRawIntensity(500);
                    resultList.add(samples.get(i));
                } else {
                    if (replaceActivity_1) {
                        samples.get(i).setRawKind(2);
                        samples.get(i).setRawIntensity(500);
                        resultList.add(samples.get(i));
                    } else {
                        samples.get(i).setRawIntensity(500);
                        resultList.add(samples.get(i));
                    }
                }
            }
            if ((samples.get(i).getRawKind() == ActivityKind.TYPE_DEEP_SLEEP) || (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP)) {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    if (i > 0) {
                        if (samples.get(i - 1).getHeartRate() > 0) {
                            samples.get(i).setHeartRate(samples.get(i - 1).getHeartRate());
                        }
                    } else {
                        if (samples.get(i + 1).getHeartRate() > 0) {
                            samples.get(i).setHeartRate(samples.get(i + 1).getHeartRate());
                        }
                    }
                    samples.get(i).setRawIntensity(500);
                    resultList.add(samples.get(i));
                } else {
                    samples.get(i).setRawIntensity(1000);
                    resultList.add(samples.get(i));
                }
            }

            if ((samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) && (i > sleepStopIndex_1)) {
                samples.get(i).setRawIntensity(500);
                resultList.add(samples.get(i));
            }
      }

// add remaining activity
        if (newSleepStopIndex_1 < next_block) {
            for (int i = newSleepStopIndex_1; i < (next_block-1); i++) {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    if (samples.get(i).getRawIntensity() <= 300) {
                        samples.get(i).setRawIntensity(200);
                    } else if ((samples.get(i).getRawIntensity() <= 1000) && (samples.get(i).getRawIntensity() > 100)) {
                        samples.get(i).setRawIntensity(400);
                    } if (samples.get(i).getRawIntensity() > 1000) {
                        samples.get(i).setRawIntensity(600);
                    }
                    samples.get(i).setRawKind(1);
                    resultList.add(samples.get(i));
                } else {
                    if (samples.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) {
                        if (i < (next_block - 3)) {
                            if ((samples.get(i + 1).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) || (samples.get(i + 2).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) || (samples.get(i + 3).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP)) {
                                samples.get(i).setRawKind(1);
                                //samples.get(i).setRawIntensity(700);
                            } else {
                                samples.get(i).setRawIntensity(1000);
                            }
                        }
                        //samples.get(i).setRawIntensity(1000);
                    } else {
                        samples.get(i).setRawIntensity(1000);
                    }
                    resultList.add(samples.get(i));
                }
            }
        }

        if (!secondBlock) {
            samples.get(next_block-1).setRawIntensity(100);
            samples.get(next_block-1).setRawKind(-1);
            resultList.add(samples.get(next_block-1));
            return resultList;
        }

// SLEEP BLOCK 2
// add sleep activity
        int newSleepStopIndex_2;
        int newSleepStartIndex_2 = 0;
        boolean replaceActivity_2 = false;
        if (sleepStartIndex_2 >= next_block + seekAhead) {
            newSleepStartIndex_2 = sleepStartIndex_2 - seekAhead;
        } else {
            newSleepStartIndex_2 = next_block;
        }
        if ((sleepStopIndex_2 + seekAhead) < numEntries) {
            newSleepStopIndex_2 = sleepStopIndex_2 + seekAhead;
        } else {
            newSleepStopIndex_2 = numEntries;
        }
        for (int i = newSleepStartIndex_2; i < newSleepStopIndex_2; i++) {
            ActivitySample sample = samples.get(i);
/*
            if ((samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) && (i < sleepStartIndex_2)) {
                samples.get(i).setRawIntensity(500);
                resultList.add(samples.get(i));
            }
 */
            if (i < sleepStartIndex_2) {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    replaceActivity_2 = true;
                    samples.get(i).setRawIntensity(500);
                    resultList.add(samples.get(i));
                } else {
                    if (replaceActivity_2) {
                        samples.get(i).setRawKind(2);
                        samples.get(i).setRawIntensity(500);
                        resultList.add(samples.get(i));
                    } else {
                        samples.get(i).setRawIntensity(500);
                        resultList.add(samples.get(i));
                    }
                }
            }


            if ((samples.get(i).getRawKind() == ActivityKind.TYPE_DEEP_SLEEP) || (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP)) {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    if (i > 0) {
                        if (samples.get(i - 1).getHeartRate() > 0) {
                            samples.get(i).setHeartRate(samples.get(i - 1).getHeartRate());
                        }
                    } else {
                        if (samples.get(i + 1).getHeartRate() > 0) {
                            samples.get(i).setHeartRate(samples.get(i + 1).getHeartRate());
                        }
                    }
                    samples.get(i).setRawIntensity(500);
                    resultList.add(samples.get(i));
                } else {
                    samples.get(i).setRawIntensity(1000);
                    resultList.add(samples.get(i));
                }
            }
            if ((samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) && (i > sleepStopIndex_2)) {
                samples.get(i).setRawIntensity(500);
                resultList.add(samples.get(i));
            }
        }

        // add remaining activity
        if (newSleepStopIndex_2 < numEntries) {
            for (int i = newSleepStopIndex_2; i < (numEntries-1); i++) {
                if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    if (samples.get(i).getRawIntensity() <= 300) {
                        samples.get(i).setRawIntensity(200);
                    } else if ((samples.get(i).getRawIntensity() <= 1000) && (samples.get(i).getRawIntensity() > 100)) {
                        samples.get(i).setRawIntensity(400);
                    } if (samples.get(i).getRawIntensity() > 1000) {
                        samples.get(i).setRawIntensity(600);
                    }
                    samples.get(i).setRawKind(1);
                    resultList.add(samples.get(i));
                } else {
                    if (samples.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) {
                        if (i < (numEntries - 3)) {
                            if ((samples.get(i + 1).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) || (samples.get(i + 2).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) || (samples.get(i + 3).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP)) {
                                samples.get(i).setRawKind(1);
                                //samples.get(i).setRawIntensity(700);
                            } else {
                                samples.get(i).setRawIntensity(1000);
                            }
                        }
                        //samples.get(i).setRawIntensity(1000);
                    } else {
                        samples.get(i).setRawIntensity(1000);
                    }
                    resultList.add(samples.get(i));
                }
            }
        }
        samples.get(numEntries-1).setRawIntensity(-1);
        samples.get(numEntries-1).setRawKind(-1);
        resultList.add(samples.get(numEntries-1));
        return resultList;
    }
}
