/*  Copyright (C) 2020-2024 Andreas Böhler, Damien Gaignon, Daniel Dakhno,
    foxstidious, Johannes Krude, José Rebelo, Petr Vaněk

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
/*  Based on code from BlueWatcher, https://github.com/masterjc/bluewatcher */
package nodomain.freeyourgadget.gadgetbridge.devices.casio.gbx100;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.Casio2C2DDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100.CasioGBX100DeviceSupport;

public class CasioGBX100DeviceCoordinator extends Casio2C2DDeviceCoordinator {
    /** CASIO brand identifier in GB Device name */
    public static final String CASIO_IDENTIFIER = "CASIO";

    /** Sub-model string for GBX-100 in GB Device name */
    public static final String GBX_100_SUB_MODEL = "GBX-100";
    /** Sub-model string for GBD-200 in GB Device name */
    public static final String GBD_200_SUB_MODEL = "GBD-200";
    /** Sub-model string for GBD-100 in GB Device name */
    public static final String GBD_100_SUB_MODEL = "GBD-100";
    /** Sub-model string for GBD-H1000 in GB Device name */
    public static final String GBD_H1000_SUB_MODEL = "GBD-H1000";

    public static final String[] VARIANTS = {
            GBX_100_SUB_MODEL,
            GBD_200_SUB_MODEL,
            GBD_100_SUB_MODEL,
            GBD_H1000_SUB_MODEL};

    protected static final Logger LOG = LoggerFactory.getLogger(CasioGBX100DeviceCoordinator.class);

    @Override
    protected Pattern getSupportedDeviceName() {
        String pattern = CASIO_IDENTIFIER + ".*(";
        for (int i = 0; i < VARIANTS.length; i++) {
            pattern += VARIANTS[i];
            if (i < VARIANTS.length - 1) {
                pattern += "|";
            }
        }
        pattern += ")";
        return Pattern.compile(pattern);
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_LAZY;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new CasioGBX100SampleProvider(device, session);
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return false;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 4;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getCasioGBX100ActivitySampleDao().queryBuilder();
        qb.where(CasioGBX100ActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_autolight,
                R.xml.devicesettings_key_vibration,
                R.xml.devicesettings_operating_sounds,
                R.xml.devicesettings_fake_ring_duration,
                R.xml.devicesettings_autoremove_message,
                R.xml.devicesettings_transliteration,
                R.xml.devicesettings_preview_message_in_title,
                R.xml.devicesettings_casio_alert
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return CasioGBX100DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_casiogbx100;
    }
}
