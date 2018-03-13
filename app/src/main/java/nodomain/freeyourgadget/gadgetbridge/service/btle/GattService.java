/*  Copyright (C) 2015-2018 Carsten Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GattService {

    //part of the generic BLE specs see https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
    //the list is complete as of 2015-09-28
    public static final UUID UUID_SERVICE_ALERT_NOTIFICATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1811")));
    public static final UUID UUID_SERVICE_AUTOMATION_IO = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1815")));
    public static final UUID UUID_SERVICE_BATTERY_SERVICE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "180F")));
    public static final UUID UUID_SERVICE_BLOOD_PRESSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1810")));
    public static final UUID UUID_SERVICE_BODY_COMPOSITION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "181B")));
    public static final UUID UUID_SERVICE_BOND_MANAGEMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "181E")));
    public static final UUID UUID_SERVICE_CONTINUOUS_GLUCOSE_MONITORING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "181F")));
    public static final UUID UUID_SERVICE_CURRENT_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1805")));
    public static final UUID UUID_SERVICE_CYCLING_POWER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1818")));
    public static final UUID UUID_SERVICE_CYCLING_SPEED_AND_CADENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1816")));
    public static final UUID UUID_SERVICE_DEVICE_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "180A")));
    public static final UUID UUID_SERVICE_ENVIRONMENTAL_SENSING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "181A")));
    public static final UUID UUID_SERVICE_GENERIC_ACCESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1800")));
    public static final UUID UUID_SERVICE_GENERIC_ATTRIBUTE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1801")));
    public static final UUID UUID_SERVICE_GLUCOSE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1808")));
    public static final UUID UUID_SERVICE_HEALTH_THERMOMETER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1809")));
    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "180D")));
    public static final UUID UUID_SERVICE_HUMAN_INTERFACE_DEVICE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1812")));
    public static final UUID UUID_SERVICE_IMMEDIATE_ALERT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1802")));
    public static final UUID UUID_SERVICE_INDOOR_POSITIONING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1821")));
    public static final UUID UUID_SERVICE_INTERNET_PROTOCOL_SUPPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1820")));
    public static final UUID UUID_SERVICE_LINK_LOSS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1803")));
    public static final UUID UUID_SERVICE_LOCATION_AND_NAVIGATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1819")));
    public static final UUID UUID_SERVICE_NEXT_DST_CHANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1807")));
    public static final UUID UUID_SERVICE_PHONE_ALERT_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "180E")));
    public static final UUID UUID_SERVICE_PULSE_OXIMETER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1822")));
    public static final UUID UUID_SERVICE_REFERENCE_TIME_UPDATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1806")));
    public static final UUID UUID_SERVICE_RUNNING_SPEED_AND_CADENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1814")));
    public static final UUID UUID_SERVICE_SCAN_PARAMETERS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1813")));
    public static final UUID UUID_SERVICE_TX_POWER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "1804")));
    public static final UUID UUID_SERVICE_USER_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "181C")));
    public static final UUID UUID_SERVICE_WEIGHT_SCALE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "181D")));

    //do we need this?

    private static final Map<UUID, String> GATTSERVICE_DEBUG;

    static {
        GATTSERVICE_DEBUG = new HashMap<>();
        GATTSERVICE_DEBUG.put(UUID_SERVICE_GENERIC_ACCESS, "Generic Access Service");
        GATTSERVICE_DEBUG.put(UUID_SERVICE_GENERIC_ATTRIBUTE, "Generic Attribute Service");
        GATTSERVICE_DEBUG.put(UUID_SERVICE_IMMEDIATE_ALERT, "Immediate Alert");

    }

    public static String lookup(UUID uuid, String fallback) {
        String name = GATTSERVICE_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }

}
