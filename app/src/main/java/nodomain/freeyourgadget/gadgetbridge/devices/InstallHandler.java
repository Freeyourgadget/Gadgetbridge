/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Interface for the UI side of certain kinds of installation of things on the
 * gadget device. The actual element to install will be passed in the constructor.
 */
public interface InstallHandler {

    /**
     * Returns true if this handler is able to install the element.
     * #validateInstallation may only be called if this method returned true.
     */
    boolean isValid();

    /**
     * Checks whether the installation of the 'element' on the device is possible
     * and configures the InstallActivity accordingly (sets helpful texts,
     * enables/disables the "Install" button, etc.
     * <p/>
     * Note: may only be called if #isValid previously returned true.
     *
     * @param installActivity the activity to interact with
     * @param device          the device to which the element shall be installed
     */
    void validateInstallation(InstallActivity installActivity, GBDevice device);

    /**
     * Allows device specific code to be executed just before the installation starts
     */
    void onStartInstall(GBDevice device);
}
