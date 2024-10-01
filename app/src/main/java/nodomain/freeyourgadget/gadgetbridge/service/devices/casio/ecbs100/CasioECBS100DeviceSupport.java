/*  Copyright (C) 2023-2024 Johannes Krude, Jisakiel

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.ecbs100;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.BasicCasio2C2DSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.Casio2C2DSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioTimeZone;

// TODO abstract methods from GWB5600
public class CasioECBS100DeviceSupport extends BasicCasio2C2DSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CasioECBS100DeviceSupport.class);

    public CasioECBS100DeviceSupport() {
        super(LOG);
    }

    @Override
    public DevicePreference[] supportedDevicePreferences() {
        return new DevicePreference[] {
            new LanguagePreference(),
            new TimeFormatPreference(),
            new DayMonthOrderPreference(),
            new OperatingSoundPreference(),
            new HourlyChimePreference(),
            new AutoLightPreference(),
            new LongerLightDurationPreference(),
            new PowerSavingPreference(),
            new ConnectionDurationPreference(),
            new TimeSyncPreference(),
        };
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean connectFirstTime() {
    // remove this workaround in case Gadgetbridge fixes
    // https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3216
        setAutoReconnect(true);
        return connect();
    }



}
