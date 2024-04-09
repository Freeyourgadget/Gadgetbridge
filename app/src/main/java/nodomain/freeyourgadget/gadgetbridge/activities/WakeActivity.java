/*  Copyright (C) 2022-2024 Ganblejs

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

    public static void start(final Context context) {
        Intent intent = new Intent(context, WakeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

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
