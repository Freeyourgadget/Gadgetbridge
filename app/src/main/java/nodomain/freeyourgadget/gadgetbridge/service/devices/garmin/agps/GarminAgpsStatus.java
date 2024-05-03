package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps;

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;


public enum GarminAgpsStatus {
    MISSING(R.string.agps_status_missing), // AGPS data file was not yet installed
    PENDING(R.string.agps_status_pending), // AGPS data file is waiting for installation
    CURRENT(R.string.agps_status_current), // AGPS data was successfully installed
    ERROR(R.string.agps_status_error);     // Unable to install AGPS data file

    private final @StringRes int text;

    GarminAgpsStatus(@StringRes int text) {
        this.text = text;
    }

    public @StringRes int getText() {
        return text;
    }
}
