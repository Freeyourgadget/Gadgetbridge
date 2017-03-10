/*  Copyright (C) 2015-2017 Daniele Gobbetti

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

import java.util.UUID;

public class GattDescriptor {

    //part of the generic BLE specs see https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorsHomePage.aspx
    //the list is complete as of 2015-09-28
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_EXTENDED_PROPERTIES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2900")));
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_USER_DESCRIPTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2901")));
    public static final UUID UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2902")));
    public static final UUID UUID_DESCRIPTOR_GATT_SERVER_CHARACTERISTIC_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2903")));
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_PRESENTATION_FORMAT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2904")));
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_AGGREGATE_FORMAT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2905")));
    public static final UUID UUID_DESCRIPTOR_VALID_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2906")));
    public static final UUID UUID_DESCRIPTOR_EXTERNAL_REPORT_REFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2907")));
    public static final UUID UUID_DESCRIPTOR_REPORT_REFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2908")));
    public static final UUID UUID_DESCRIPTOR_NUMBER_OF_DIGITALS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2909")));
    public static final UUID UUID_DESCRIPTOR_VALUE_TRIGGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290A")));
    public static final UUID UUID_DESCRIPTOR_ES_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290B")));
    public static final UUID UUID_DESCRIPTOR_ES_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290C")));
    public static final UUID UUID_DESCRIPTOR_ES_TRIGGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290D")));
    public static final UUID UUID_DESCRIPTOR_TIME_TRIGGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290E")));

}
