/*  Copyright (C) 2019 Sophanimus

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

package nodomain.freeyourgadget.gadgetbridge.devices.jyou;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class BFH16DeviceCoordinator extends AbstractDeviceCoordinator
{
    protected static final Logger LOG = LoggerFactory.getLogger(BFH16DeviceCoordinator.class);


    @Override
    public DeviceType getDeviceType() {
        return DeviceType.BFH16;
    }

    @Override
    public String getManufacturer() {
        return "Denver";
    }

    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {

        ParcelUuid bfhService1 = new ParcelUuid(BFH16Constants.BFH16_IDENTIFICATION_SERVICE1);
        ParcelUuid bfhService2 = new ParcelUuid(BFH16Constants.BFH16_IDENTIFICATION_SERVICE2);

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(bfhService1)
                .setServiceUuid(bfhService2)
                .build();

        return Collections.singletonList(filter);
    }

    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {

        String name = candidate.getDevice().getName();
        if (name != null) {
            if (name.startsWith("BFH-16")) {
                return DeviceType.BFH16;
            }
        }

        return DeviceType.UNKNOWN;

    }

    @Override
    public int getBondingStyle(GBDevice deviceCandidate){
        return BONDING_STYLE_NONE;
    }

    @Override
    public Class<? extends Activity> getPairingActivity(){
        return null;
    }

    //Additional required functions ______________________________________

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity()
    {
        return null;
    }


    //Supported ________________________________________________________

    @Override
    public int getAlarmSlotCount()
    {
        return 3;
    }

    @Override
    public boolean supportsFindDevice()
    {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device)
    {
        return true;
    }

    @Override
    public boolean supportsRealtimeData()
    {
        return true;
    }


    //NOT Supported ________________________________________________________

    @Override
    public boolean supportsActivityDataFetching(){
        return false;
    }

    @Override
    public boolean supportsActivityTracking()
    {
        return false;
    }

    @Override
    public boolean supportsAppsManagement()
    {
        return false;
    }

    @Override
    public boolean supportsCalendarEvents()
    {
        return false;
    }

    @Override
    public boolean supportsLedColor()
    {
        return false;
    }

    @Override
    public boolean supportsMusicInfo()
    {
        return false;
    }

    @Override
    public boolean supportsRgbLedColor()
    {
        return false;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device)
    {
        return false;
    }

    @Override
    public boolean supportsWeather()
    {
        return false;
    }






}
