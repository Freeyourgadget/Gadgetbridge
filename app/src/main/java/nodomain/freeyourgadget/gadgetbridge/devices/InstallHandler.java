package nodomain.freeyourgadget.gadgetbridge.devices;

import android.net.Uri;
import android.support.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.activities.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Interface for the UI side of certain kinds of installation of things on the
 * gadget device. The actual element to install will be passed in the constructor.
 */
public interface InstallHandler {

    /**
     * Returns true if this handler is able to install the element.
     */
    public boolean isValid();

    /**
     * Checks whether the installation of the 'element' on the device is possible
     * and configures the InstallActivity accordingly (sets helpful texts,
     * enables/disables the "Install" button, etc.
     * @param installActivity the activity to interact with
     * @param device the device to which the element shall be installed
     */
    void validateInstallation(InstallActivity installActivity, GBDevice device);
}
