package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class WakeActivity extends Activity {
    /*
    By starting this activity via an intent sent e.g. by a Bangle.js the keyguard can be
    dismissed automatically if the android device is in a trusted state (e.g. using a GB Device (Bangle.js) as a
    trusted device via smart lock settings)

    First try to start the activity you want to start with an intent and then start this activity with a second intent, both initiated on the Bangle.js, or other device.
     */

    private void dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
        // Unlock the android device if it's in a trusted state, otherwise request user action to unlock
        dismissKeyguard();
        // Go back to last activity, which can have been waiting to start under
        // the lock screen, e.g. it was previously initiated via intent message from Bangle.js
        this.onBackPressed();
    }
}
