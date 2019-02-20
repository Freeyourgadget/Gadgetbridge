package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeTimePreferenceActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zetime_preferences);

        //addTryListeners();

        Prefs prefs = GBApplication.getPrefs();

        final Preference heartrateMeasurementInterval = findPreference(ZeTimeConstants.PREF_ZETIME_HEARTRATE_INTERVAL);
        heartrateMeasurementInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));
                return true;
            }
        });

        final Preference screenOnDuration = findPreference(ZeTimeConstants.PREF_SCREENTIME);
        screenOnDuration.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_SCREENTIME);
                return true;
            }
        });

        final Preference wearSide = findPreference(ZeTimeConstants.PREF_WRIST);
        wearSide.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_WRIST);
                return true;
            }
        });

        final Preference analogMode = findPreference(ZeTimeConstants.PREF_ANALOG_MODE);
        analogMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_ANALOG_MODE);
                return true;
            }
        });

        final Preference activity = findPreference(ZeTimeConstants.PREF_ACTIVITY_TRACKING);
        activity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_ACTIVITY_TRACKING);
                return true;
            }
        });

        final Preference handmove = findPreference(ZeTimeConstants.PREF_HANDMOVE_DISPLAY);
        handmove.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_HANDMOVE_DISPLAY);
                return true;
            }
        });

        final Preference dnd = findPreference(ZeTimeConstants.PREF_DO_NOT_DISTURB);
        dnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_DO_NOT_DISTURB);
                return true;
            }
        });

        final Preference dnd_start = findPreference(ZeTimeConstants.PREF_DO_NOT_DISTURB_START);
        dnd_start.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_DO_NOT_DISTURB);
                return true;
            }
        });

        final Preference dnd_end = findPreference(ZeTimeConstants.PREF_DO_NOT_DISTURB_END);
        dnd_end.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_DO_NOT_DISTURB);
                return true;
            }
        });

        final Preference caloriesType = findPreference(ZeTimeConstants.PREF_CALORIES_TYPE);
        caloriesType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_CALORIES_TYPE);
                return true;
            }
        });

        final Preference timeFormat = findPreference(ZeTimeConstants.PREF_TIME_FORMAT);
        timeFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_TIME_FORMAT);
                return true;
            }
        });

        final Preference dateFormat = findPreference(ZeTimeConstants.PREF_DATE_FORMAT);
        dateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_DATE_FORMAT);
                return true;
            }
        });

        final Preference inactivityAlert = findPreference(ZeTimeConstants.PREF_INACTIVITY_ENABLE);
        inactivityAlert.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                    GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertStart = findPreference(ZeTimeConstants.PREF_INACTIVITY_START);
        inactivityAlertStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertEnd = findPreference(ZeTimeConstants.PREF_INACTIVITY_END);
        inactivityAlertEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertThreshold = findPreference(ZeTimeConstants.PREF_INACTIVITY_THRESHOLD);
        inactivityAlertThreshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertMo = findPreference(ZeTimeConstants.PREF_INACTIVITY_MO);
        inactivityAlertMo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertTu = findPreference(ZeTimeConstants.PREF_INACTIVITY_TU);
        inactivityAlertTu.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertWe = findPreference(ZeTimeConstants.PREF_INACTIVITY_WE);
        inactivityAlertWe.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertTh = findPreference(ZeTimeConstants.PREF_INACTIVITY_TH);
        inactivityAlertTh.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertFr = findPreference(ZeTimeConstants.PREF_INACTIVITY_FR);
        inactivityAlertFr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertSa = findPreference(ZeTimeConstants.PREF_INACTIVITY_SA);
        inactivityAlertSa.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference inactivityAlertSu = findPreference(ZeTimeConstants.PREF_INACTIVITY_SU);
        inactivityAlertSu.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_KEY);
                    }
                });
                return true;
            }
        });

        final Preference smsSignaling = findPreference(ZeTimeConstants.PREF_SMS_SIGNALING);
        smsSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_SMS_SIGNALING);
                return true;
            }
        });
        final Preference antiSignaling = findPreference(ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING);
        antiSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING);
                return true;
            }
        });
        final Preference calendarSignaling = findPreference(ZeTimeConstants.PREF_CALENDAR_SIGNALING);
        calendarSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_CALENDAR_SIGNALING);
                return true;
            }
        });
        final Preference callSignaling = findPreference(ZeTimeConstants.PREF_CALL_SIGNALING);
        callSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_CALL_SIGNALING);
                return true;
            }
        });
        final Preference missedSignaling = findPreference(ZeTimeConstants.PREF_MISSED_CALL_SIGNALING);
        missedSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_MISSED_CALL_SIGNALING);
                return true;
            }
        });
        final Preference emailSignaling = findPreference(ZeTimeConstants.PREF_EMAIL_SIGNALING);
        emailSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_EMAIL_SIGNALING);
                return true;
            }
        });
        final Preference inactivitySignaling = findPreference(ZeTimeConstants.PREF_INACTIVITY_SIGNALING);
        inactivitySignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_INACTIVITY_SIGNALING);
                return true;
            }
        });
        final Preference powerSignaling = findPreference(ZeTimeConstants.PREF_LOW_POWER_SIGNALING);
        powerSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_LOW_POWER_SIGNALING);
                return true;
            }
        });
        final Preference socialSignaling = findPreference(ZeTimeConstants.PREF_SOCIAL_SIGNALING);
        socialSignaling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(ZeTimeConstants.PREF_SOCIAL_SIGNALING);
                return true;
            }
        });

        GBApplication.deviceService().onReadConfiguration("do_it");
    }

    /**
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

//    private void addTryListeners() {
//        for (final NotificationType type : NotificationType.values()) {
//            String prefKey = "zetime_try_" + type.getGenericType();
//            final Preference tryPref = findPreference(prefKey);
//            if (tryPref != null) {
//                tryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                    @Override
//                    public boolean onPreferenceClick(Preference preference) {
//                        tryVibration(type);
//                        return true;
//                    }
//                });
//            } else {
//                GB.toast(getBaseContext(), "Unable to find preference key: " + prefKey + ", trying the vibration won't work", Toast.LENGTH_LONG, GB.WARN);
//            }
//        }
//    }
//
//    private void tryVibration(NotificationType type) {
//        NotificationSpec spec = new NotificationSpec();
//        spec.type = type;
//        GBApplication.deviceService().onNotification(spec);
//    }
}
