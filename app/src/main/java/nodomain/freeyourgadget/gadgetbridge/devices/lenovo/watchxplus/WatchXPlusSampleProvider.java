package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private final float movementDivisor = 950.0f;

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

    // generate ActivityKind.TYPE_NOT_MEASURED if there are no data for more than 15 min. and less than 60 min.
    // generate ActivityKind.TYPE_NOT_WORN if there are no data for more than 60 min.
    @NonNull
    private List<WatchXPlusActivitySample> checkActivityData(List<WatchXPlusActivitySample> samples, int notMeasuredTS, int notWornTS) {
        int oldTS = 0;
        int newTS = 0;
        oldTS = samples.get(0).getTimestamp();
        for (int i = 0; i < samples.size(); i++) {
            //oldTS = resultList.get(i).getTimestamp();
            newTS = samples.get(i).getTimestamp();
            if ((newTS - oldTS) < notMeasuredTS) { //check data timestamp diff is more than 15 min
                oldTS = samples.get(i).getTimestamp();
            } else if (((newTS - oldTS) > notMeasuredTS) && ((newTS - oldTS) < notWornTS)) { //set data to ActivityKind.TYPE_NOT_MEASURED) if timestamp diff is more than 15 min
                samples.get(i-1).setRawKind(ActivityKind.TYPE_NOT_MEASURED);
                samples.get(i).setRawKind(ActivityKind.TYPE_NOT_MEASURED);
                oldTS = samples.get(i).getTimestamp();
            } else if ((newTS - oldTS) > notWornTS) { //set data to ActivityKind.TYPE_NOT_WORN if timestamp diff is more than 60 min
                samples.get(i-1).setRawKind(ActivityKind.TYPE_NOT_WORN);
                samples.get(i).setRawKind(ActivityKind.TYPE_NOT_WORN);
                oldTS = samples.get(i).getTimestamp();
            }
        }
        return samples;
    }




    @Override
    public List<WatchXPlusActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        boolean showRawData = GBApplication.getDeviceSpecificSharedPrefs(mDevice.getAddress()).getBoolean(WatchXPlusConstants.PREF_SHOW_RAW_GRAPH, false);
        if (showRawData) {
            return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
        }
        List<WatchXPlusActivitySample> samples = getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
        int numEntries = samples.size();
        if (numEntries < 3) {
            return samples;
        }

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
                        if ((countNextSleepStart_1 - sleepStartIndex_1) > seekAhead * 3) {
                            sleepStartIndex_1 = countNextSleepStart_1;
                            sleepStopIndex_1 = sleepStartIndex_1;
                            countNextSleepStop_1 = sleepStopIndex_1;
                        }
                    }
                }


                        if ((i - sleepStopIndex_1) < (seekAhead * 4)) {
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
                // find sleep start index
                if (sleepStartIndex_2 == 0) {
                    sleepStartIndex_2 = i;
                    sleepStopIndex_2 = sleepStartIndex_2;
                    countNextSleepStop_2 = sleepStopIndex_2;
                } else {
                    if (countNextSleepStart_2 == 0) {
                        countNextSleepStart_2 = i;
                        // reset start index if next index is far ahead
                        if ((countNextSleepStart_2 - sleepStartIndex_2) > seekAhead * 3) {
                            sleepStartIndex_2 = countNextSleepStart_2;
                            sleepStopIndex_2 = sleepStartIndex_2;
                            countNextSleepStop_2 = sleepStopIndex_2;
                        }
                    }
                }
                if ((i - sleepStopIndex_2) < (seekAhead * 4)) {
                    sleepStopIndex_2 = i;
                }
                countNextSleepStop_2 = i;
            }
        }
        if (sleepStartIndex_2 != 0) {
            secondBlock = true;
            LOG.info(" Found second block ");
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

        if ((sleepStopIndex_1 + seekAhead * 2) < next_block) {
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
                    samples.get(i).setRawIntensity(600);
                    resultList.add(samples.get(i));
                } else {
                    if (replaceActivity_1) {
                        samples.get(i).setRawKind(2);
                        samples.get(i).setRawIntensity(600);
                        resultList.add(samples.get(i));
                    } else {
                        samples.get(i).setRawIntensity(600);
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
                    samples.get(i).setRawIntensity(600);
                    resultList.add(samples.get(i));
                } else {
                    samples.get(i).setRawIntensity(1000);
                    resultList.add(samples.get(i));
                }
            }

            if ((samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) && (i > sleepStopIndex_1)) {
                samples.get(i).setRawIntensity(600);
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
// SLEEP BLOCK 2
      if (secondBlock) {
// add sleep activity
          int newSleepStopIndex_2;
          int newSleepStartIndex_2 = 0;
          boolean replaceActivity_2 = false;
          if (sleepStartIndex_2 >= next_block + seekAhead) {
              newSleepStartIndex_2 = sleepStartIndex_2 - seekAhead;
          } else {
              newSleepStartIndex_2 = next_block;
          }
          if ((sleepStopIndex_2 + seekAhead * 2) < numEntries) {
              newSleepStopIndex_2 = sleepStopIndex_2 + seekAhead * 2;
          } else {
              newSleepStopIndex_2 = numEntries;
          }
          for (int i = newSleepStartIndex_2; i < newSleepStopIndex_2; i++) {
              ActivitySample sample = samples.get(i);
              if (i < sleepStartIndex_2) {
                  if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                      replaceActivity_2 = true;
                      samples.get(i).setRawIntensity(600);
                      resultList.add(samples.get(i));
                  } else {
                      if (replaceActivity_2) {
                          samples.get(i).setRawKind(2);
                          samples.get(i).setRawIntensity(600);
                          resultList.add(samples.get(i));
                      } else {
                          samples.get(i).setRawIntensity(600);
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
                      samples.get(i).setRawIntensity(600);
                      resultList.add(samples.get(i));
                  } else {
                      samples.get(i).setRawIntensity(1000);
                      resultList.add(samples.get(i));
                  }
              }
              if ((samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) && (i > sleepStopIndex_2)) {
                  samples.get(i).setRawIntensity(600);
                  resultList.add(samples.get(i));
              }
          }

          // add remaining activity
          if (newSleepStopIndex_2 < numEntries) {
              for (int i = newSleepStopIndex_2; i < (numEntries - 1); i++) {
                  if (samples.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                      if (samples.get(i).getRawIntensity() <= 300) {
                          samples.get(i).setRawIntensity(200);
                      } else if ((samples.get(i).getRawIntensity() <= 1000) && (samples.get(i).getRawIntensity() > 100)) {
                          samples.get(i).setRawIntensity(400);
                      }
                      if (samples.get(i).getRawIntensity() > 1000) {
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
      }
    // add one ActivityKind.TYPE_NOT_MEASURED at end of data
        samples.get(numEntries-1).setRawIntensity(0);
        samples.get(numEntries-1).setRawKind(ActivityKind.TYPE_NOT_MEASURED);
        samples.get(numEntries-1).setHeartRate(0);
        resultList.add(samples.get(numEntries-1));

    // find all steps, total activity intensity  and maxHR
        int totalSteps = 0;
        int maxHeartRate = 10;
        numEntries = resultList.size();
        for (int i = 0; i < numEntries-1; i++) {
            if (resultList.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) {
                if (resultList.get(i).getSteps() > 0) {
                    totalSteps = totalSteps + resultList.get(i).getSteps();
                }
            }
            if (resultList.get(i).getHeartRate() > maxHeartRate) {
                maxHeartRate = resultList.get(i).getHeartRate();
            }
        }

    // reformat activity data based on heart rate
        int newIntensity, correctedSteps;
        int totalIntensity = 0;
        for (int i = 0; i < numEntries-1; i++) {
            if ((resultList.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) || (resultList.get(i).getRawKind() == ActivityKind.TYPE_LIGHT_SLEEP)) {
                if (resultList.get(i).getRawIntensity() <= 600) { // set interpolated intensity based on heart rate for every TYPE_ACTIVITY which are converted from TYPE_LIGHT_SLEEP
                    if (resultList.get(i).getHeartRate() < 10) {
                        newIntensity = resultList.get(i).getRawIntensity() + ((maxHeartRate - resultList.get(i+1).getHeartRate()) * 2);
                    } else {
                        newIntensity = resultList.get(i).getRawIntensity() + ((maxHeartRate - resultList.get(i).getHeartRate()) * 2);
                    }
                } else { // because there are not RAW intensity values for every TYPE_ACTIVITY set interpolated intensity based on heart rate
                    newIntensity = resultList.get(i).getRawIntensity() - ((maxHeartRate - resultList.get(i).getHeartRate()) * 2);
                }
                /*
                if (stepsPerActivity > 0.0f) { // because there are not steps values for every TYPE_ACTIVITY set interpolated steps
                    correctedSteps = (int) (resultList.get(i).getRawIntensity() / stepsPerActivity);
                    resultList.get(i).setSteps(correctedSteps);
                }
                 */
                resultList.get(i).setRawIntensity(newIntensity);
                if (resultList.get(i).getRawIntensity() > 0) {
                    totalIntensity = totalIntensity + newIntensity;
                }
            } else { // because there are not TYPE_DEEP_SLEEP intensity set random DEEP_SLEEP intensity
                Random r = new Random();
                newIntensity = resultList.get(i).getRawIntensity() - ((maxHeartRate - (int)(r.nextFloat() * maxHeartRate)) * 2);
                resultList.get(i).setRawIntensity(newIntensity);
                if (resultList.get(i).getRawIntensity() > 0) {
                    totalIntensity = totalIntensity + newIntensity;
                }
            }
        }

        // because there are not steps values for every TYPE_ACTIVITY set interpolated steps
        float stepsPerActivity = 0.000f;
        int newTotalSteps = 0;
        int activityCount = 0;
        if (totalSteps > 0) {
            stepsPerActivity = totalIntensity / totalSteps;
            for (int i = 0; i < numEntries - 1; i++) {
                if (resultList.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) {
                    if (stepsPerActivity > 0.0f) {
                        correctedSteps = (int) (resultList.get(i).getRawIntensity() / stepsPerActivity);
                        resultList.get(i).setSteps(correctedSteps);
                        newTotalSteps = newTotalSteps + correctedSteps;
                        activityCount = activityCount + 1;
                    }
                }
            }
        }
        if (newTotalSteps < totalSteps) {
            int stepsDiff = newTotalSteps - totalSteps;
            int increaseStepsWith = stepsDiff / activityCount;
            if (increaseStepsWith <= 1) {
                increaseStepsWith = 2;
            }
            newTotalSteps = 0;
            for (int i = 0; i < numEntries - 1; i++) {
                if (resultList.get(i).getRawKind() == ActivityKind.TYPE_ACTIVITY) {
                    correctedSteps = resultList.get(i).getSteps() + increaseStepsWith;
                    newTotalSteps = newTotalSteps + correctedSteps;
                    if (newTotalSteps <= totalSteps) {
                        resultList.get(i).setSteps(correctedSteps);
                    } else {
                        break;
                    }
                }
            }
        }
        return checkActivityData(resultList, 900, 3600);
    }
}
