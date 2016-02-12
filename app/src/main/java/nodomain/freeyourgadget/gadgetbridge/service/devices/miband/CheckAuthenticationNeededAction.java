package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.bluetooth.BluetoothGatt;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.PlainAction;

public class CheckAuthenticationNeededAction extends PlainAction {
    private final GBDevice mDevice;

    public CheckAuthenticationNeededAction(GBDevice device) {
        super();
        mDevice = device;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        // the state is set in MiBandSupport.handleNotificationNotif()
        switch (mDevice.getState()) {
            case AUTHENTICATION_REQUIRED: // fall through
            case AUTHENTICATING:
                return false; // abort the whole thing
            default:
                return true;
        }
    }
}
