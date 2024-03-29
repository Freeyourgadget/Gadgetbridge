/*  Copyright (C) 2023-2024 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

public enum GarminSystemEventType {
    SYNC_COMPLETE,
    SYNC_FAIL,
    FACTORY_RESET,
    PAIR_START,
    PAIR_COMPLETE,
    PAIR_FAIL,
    HOST_DID_ENTER_FOREGROUND,
    HOST_DID_ENTER_BACKGROUND,
    SYNC_READY,
    NEW_DOWNLOAD_AVAILABLE,
    DEVICE_SOFTWARE_UPDATE,
    DEVICE_DISCONNECT,
    TUTORIAL_COMPLETE,
    SETUP_WIZARD_START,
    SETUP_WIZARD_COMPLETE,
    SETUP_WIZARD_SKIPPED,
    TIME_UPDATED;
}
