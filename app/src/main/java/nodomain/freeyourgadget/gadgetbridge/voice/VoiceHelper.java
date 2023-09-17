/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.voice;

import static nodomain.freeyourgadget.gadgetbridge.activities.voice.VoiceHelperSettingsConst.VOICE_HELPER_PACKAGE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.voice.IOpusCodecService;

public class VoiceHelper {
    private static final Logger LOG = LoggerFactory.getLogger(VoiceHelper.class);

    public static final List<String> KNOWN_PACKAGES = new ArrayList<String>() {{
        add("nodomain.freeyourgadget.voice");
        add("nodomain.freeyourgadget.voice.debug");
        add("nodomain.freeyourgadget.voice.nightly");
    }};

    private final Context mContext;
    private final Callback mCallback;
    private final String packageName;

    private ServiceConnection mConnection;
    private IOpusCodecService mOpusCodecService;

    public VoiceHelper(final Context context, final Callback callback) {
        this.mContext = context;
        this.mCallback = callback;

        final List<CharSequence> installedPackages = findInstalledPackages(mContext);
        if (installedPackages.isEmpty()) {
            LOG.warn("Voice Helper is not installed");
            this.packageName = null;
            return;
        }
        final Prefs prefs = GBApplication.getPrefs();
        this.packageName = prefs.getString(VOICE_HELPER_PACKAGE, installedPackages.get(0).toString());
    }

    public void connect() {
        LOG.info("Connecting to Voice Helper");

        mConnection = new ServiceConnection() {
            public void onServiceConnected(final ComponentName className, final IBinder service) {
                LOG.info("onServiceConnected {}", className);

                mOpusCodecService = IOpusCodecService.Stub.asInterface(service);
                mCallback.onVoiceHelperConnection(true);
            }

            // Called when the connection with the service disconnects unexpectedly.
            public void onServiceDisconnected(final ComponentName className) {
                LOG.error("Service has unexpectedly disconnected");
                mOpusCodecService = null;
                mCallback.onVoiceHelperConnection(false);
            }
        };
        final Intent intent = new Intent("nodomain.freeyourgadget.voice.OpusCodecService");
        intent.setPackage(packageName);
        boolean res = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if (res) {
            LOG.info("Bound to Voice Helper");
        } else {
            LOG.warn("Could not bind to Voice Helper");
        }
    }

    public void disconnect() {
        mContext.unbindService(mConnection);
    }

    public boolean isConnected() {
        return mOpusCodecService != null;
    }

    public OpusCodec createOpusCodec() throws RemoteException {
        return new OpusCodec(mOpusCodecService);
    }

    public static String getPermission(final String packageName) {
        return String.format(Locale.ROOT, "%s.VOICE_HELPER", packageName);
    }

    public static List<CharSequence> findInstalledPackages(final Context context) {
        final List<CharSequence> installedPackages = new ArrayList<>();
        for (final String knownPackage : KNOWN_PACKAGES) {
            if (isPackageInstalled(context, knownPackage)) {
                installedPackages.add(knownPackage);
            }
        }
        return installedPackages;
    }

    private static boolean isPackageInstalled(final Context context, final String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (final PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public interface Callback {
        void onVoiceHelperConnection(boolean connected);
    }
}
