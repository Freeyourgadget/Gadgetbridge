/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.capabilities.widgets;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Provide an interface to manage a device's widgets in a device-independent manner.
 */
public interface WidgetManager {
    /**
     * The widget layouts supported by this device.
     */
    List<WidgetLayout> getSupportedWidgetLayouts();

    /**
     * The widget parts that can be used to build a widget screen, for a specific widget type.x
     */
    List<WidgetPart> getSupportedWidgetParts(WidgetType targetWidgetType);

    /**
     * The currently configured widget screens on this device.
     */
    List<WidgetScreen> getWidgetScreens();

    GBDevice getDevice();

    /**
     * The minimum number of screens that must be present - deleting screens won't be allowed
     * past this number.
     */
    int getMinScreens();

    /**
     * The maximum number of screens that can be created.
     */
    int getMaxScreens();

    /**
     * Saves a screen. If the screen is new, the ID will be null, otherwise it matches the
     * screen that is being updated.
     */
    void saveScreen(WidgetScreen widgetScreen);

    /**
     * Deletes a screen.
     */
    void deleteScreen(WidgetScreen widgetScreen);

    /**
     * Send the currently configured screens to the device.
     */
    void sendToDevice();
}
