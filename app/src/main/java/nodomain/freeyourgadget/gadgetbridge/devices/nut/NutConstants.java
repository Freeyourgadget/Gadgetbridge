/*  Copyright (C) 2020-2021 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.nut;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;

public class NutConstants {
    /**
     * Just battery info
     */
    public static final UUID SERVICE_BATTERY = GattService.UUID_SERVICE_BATTERY_SERVICE;
    public static final UUID CHARAC_BATTERY_INFO = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    /**
     * Device info available.
     **/
    public static final UUID SERVICE_DEVICE_INFO = GattService.UUID_SERVICE_DEVICE_INFORMATION;
    /**
     * Firmware version.
     * Used with {@link NutConstants#SERVICE_DEVICE_INFO}
     */
    public static final UUID CHARAC_FIRMWARE_VERSION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    /**
     * System ID.
     * Used with {@link NutConstants#SERVICE_DEVICE_INFO}
     */
    public static final UUID CHARAC_SYSTEM_ID = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");
    /**
     * Hardware version.
     * Used with {@link NutConstants#SERVICE_DEVICE_INFO}
     */
    public static final UUID CHARAC_HARDWARE_VERSION = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    /**
     * Manufacturer name.
     * Used with {@link NutConstants#SERVICE_DEVICE_INFO}
     */
    public static final UUID CHARAC_MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");


    /**
     * Link loss alert service.
     */
    public static final UUID SERVICE_LINK_LOSS = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");


    /**
     * Immediate alert service.
     */
    public static final UUID SERVICE_IMMEDIATE_ALERT = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    /**
     * Immediate alert level
     * Used with {@link NutConstants#SERVICE_IMMEDIATE_ALERT}
     */
    public static final UUID CHARAC_LINK_LOSS_ALERT_LEVEL = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");


    /**
     * Proprietary command endpoint.
     * TODO: Anything else in this service on other devices?
     */
    public static final UUID SERVICE_PROPRIETARY_NUT = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
    /**
     * Shutdown or reset.
     * Used with {@link NutConstants#SERVICE_PROPRIETARY_NUT}
     */
    public static final UUID CHARAC_CHANGE_POWER = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    /**
     * Commands for proprietary service.
     * Used with {@link NutConstants#SERVICE_PROPRIETARY_NUT}
     */
    public static final UUID CHARAC_DFU_PW = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    /**
     * Authentication using 16-byte key?
     * Used with {@link NutConstants#SERVICE_PROPRIETARY_NUT}
     * TODO: Exists only on Nut Mini?
     */
    public static final UUID CHARAC_AUTH_STATUS = UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb");


    /**
     * Ringing configuration.
     * TODO: Exact purpose?
     */
    public static final UUID SERVICE_UNKNOWN_2 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    /**
     * Ringing configuration.
     * Used with {@link NutConstants#SERVICE_UNKNOWN_2}
     * TODO: Something else on other devices?
     */
    public static final UUID CHARAC_UNKNOWN_2 = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");


    /**
     * Very little mention online, specific to Nut devices?
     */
    public static final UUID UNKNOWN_3 = UUID.fromString("00001530-0000-1000-8000-00805f9b34fb");
}
