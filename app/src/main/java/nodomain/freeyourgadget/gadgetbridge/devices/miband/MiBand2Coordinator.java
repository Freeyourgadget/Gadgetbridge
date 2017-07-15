/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband2.MiBand2FWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_START;

public class MiBand2Coordinator extends MiBandCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand2Coordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND2;
    }

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid mi2Service = new ParcelUuid(MiBandService.UUID_SERVICE_MIBAND2_SERVICE);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(mi2Service).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        if (candidate.supportsService(MiBand2Service.UUID_SERVICE_MIBAND2_SERVICE)) {
            return DeviceType.MIBAND2;
        }

        // and a heuristic for now
        try {
            BluetoothDevice device = candidate.getDevice();
//            if (isHealthWearable(device)) {
                String name = device.getName();
                if (name != null && name.equalsIgnoreCase(MiBandConst.MI_BAND2_NAME)) {
                    return DeviceType.MIBAND2;
                }
//            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return DeviceType.UNKNOWN;

    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new MiBand2SampleProvider(device, session);
    }

    public static DateTimeDisplay getDateDisplay(Context context) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        String dateFormatTime = context.getString(R.string.p_dateformat_time);
        if (dateFormatTime.equals(prefs.getString(MiBandConst.PREF_MI2_DATEFORMAT, dateFormatTime))) {
            return DateTimeDisplay.TIME;
        }
        return DateTimeDisplay.DATE_TIME;
    }

    public static boolean getActivateDisplayOnLiftWrist() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_ACTIVATE_DISPLAY_ON_LIFT, true);
    }

    public static Set<String> getDisplayItems() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getStringSet(MiBandConst.PREF_MI2_DISPLAY_ITEMS, null);
    }
    
    public static boolean getRotateWristToSwitchInfo() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO, false);
    }

    public static Date getDoNotDisturbStart() {
        Prefs prefs = GBApplication.getPrefs();
        String time = prefs.getString(PREF_MI2_DO_NOT_DISTURB_START, "01:00");

        DateFormat df = new SimpleDateFormat("HH:mm");
        try {
            return df.parse(time);
        } catch(Exception e) {
        }

        return new Date();
    }

    public static Date getDoNotDisturbEnd() {
        Prefs prefs = GBApplication.getPrefs();
        String time = prefs.getString(PREF_MI2_DO_NOT_DISTURB_END, "06:00");

        DateFormat df = new SimpleDateFormat("HH:mm");
        try {
            return df.parse(time);
        } catch(Exception e) {
        }

        return new Date();
    }

    public static DoNotDisturb getDoNotDisturb(Context context) {
        Prefs prefs = GBApplication.getPrefs();

        String dndOff = context.getString(R.string.p_off);
        String dndAutomatic = context.getString(R.string.p_automatic);
        String dndScheduled = context.getString(R.string.p_scheduled);

        String pref = prefs.getString(MiBandConst.PREF_MI2_DO_NOT_DISTURB, dndOff);

        if (dndAutomatic.equals(pref)) {
            return DoNotDisturb.AUTOMATIC;
        } else if (dndScheduled.equals(pref)) {
            return DoNotDisturb.SCHEDULED;
        }

        return DoNotDisturb.OFF;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        MiBand2FWInstallHandler handler = new MiBand2FWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }
}
