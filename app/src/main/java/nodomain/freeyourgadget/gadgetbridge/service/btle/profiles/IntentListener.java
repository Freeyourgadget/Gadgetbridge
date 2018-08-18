package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles;

import android.content.Intent;

/**
 * Callback interface for delivering results of ble requests.
 */
public interface IntentListener {
    void notify(Intent intent);
}
