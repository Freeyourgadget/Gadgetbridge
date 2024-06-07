package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class GarminPreferences {
    public static final String PREF_GARMIN_CAPABILITIES = "garmin_capabilities";
    public static final String PREF_FEAT_CANNED_MESSAGES = "feat_canned_messages";
    public static final String PREF_FEAT_CONTACTS = "feat_contacts";
    public static final String PREF_AGPS_KNOWN_URLS = "garmin_agps_known_urls";
    public static final String PREF_GARMIN_AGPS_STATUS = "garmin_agps_status_%s";
    public static final String PREF_GARMIN_AGPS_UPDATE_TIME = "garmin_agps_update_time_%s";
    public static final String PREF_GARMIN_AGPS_FOLDER = "garmin_agps_folder";
    public static final String PREF_GARMIN_AGPS_FILENAME = "garmin_agps_filename_%s";
    public static final String PREF_GARMIN_REALTIME_SETTINGS = "garmin_realtime_settings";

    public static String agpsStatus(final String url) {
        return String.format(GarminPreferences.PREF_GARMIN_AGPS_STATUS, CheckSums.md5(url));
    }

    public static String agpsUpdateTime(final String url) {
        return String.format(GarminPreferences.PREF_GARMIN_AGPS_UPDATE_TIME, CheckSums.md5(url));
    }

    public static String agpsFilename(final String url) {
        return String.format(GarminPreferences.PREF_GARMIN_AGPS_FILENAME, CheckSums.md5(url));
    }
}
