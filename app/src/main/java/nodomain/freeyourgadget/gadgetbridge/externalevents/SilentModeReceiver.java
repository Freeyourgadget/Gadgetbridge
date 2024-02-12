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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class SilentModeReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(SilentModeReceiver.class);

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (!AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
            LOG.warn("Unexpected action {}", intent.getAction());
            return;
        }

        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int ringerMode = audioManager.getRingerMode();

        GBApplication.deviceService().onChangePhoneSilentMode(ringerMode);
    }
}
