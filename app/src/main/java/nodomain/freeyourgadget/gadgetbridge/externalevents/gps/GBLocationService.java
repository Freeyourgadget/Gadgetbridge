/*  Copyright (C) 2022-2024 halemmerich, José Rebelo, LukasEdl, Martin Boonk

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
package nodomain.freeyourgadget.gadgetbridge.externalevents.gps;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;


/**
 * A static location manager, which keeps track of what providers are currently running. A notification is kept
 * while there is at least one provider running.
 */
public class GBLocationService extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBLocationService.class);

    public static final String ACTION_START = "GBLocationService.START";
    public static final String ACTION_STOP = "GBLocationService.STOP";
    public static final String ACTION_STOP_ALL = "GBLocationService.STOP_ALL";

    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_INTERVAL = "extra_interval";

    private final Context context;
    private final Map<GBDevice, List<GBLocationProvider>> providersByDevice = new HashMap<>();

    public GBLocationService(final Context context) {
        this.context = context;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null) {
            LOG.warn("Action is null");
            return;
        }

        final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);

        switch (intent.getAction()) {
            case ACTION_START:
                if (device == null) {
                    LOG.error("Device is null for {}", intent.getAction());
                    return;
                }

                final GBLocationProviderType providerType = GBLocationProviderType.valueOf(
                        intent.hasExtra(EXTRA_TYPE) ? intent.getStringExtra(EXTRA_TYPE) : "GPS"
                );
                final int updateInterval = intent.getIntExtra(EXTRA_INTERVAL, 1000);

                LOG.debug("Starting location provider {} for {}", providerType, device.getAliasOrName());

                if (!providersByDevice.containsKey(device)) {
                    providersByDevice.put(device, new ArrayList<>());
                }

                updateNotification();

                final List<GBLocationProvider> existingProviders = providersByDevice.get(device);

                final GBLocationListener locationListener = new GBLocationListener(device);
                final GBLocationProvider locationProvider = providerType.newInstance(context, locationListener);
                locationProvider.start(updateInterval);
                Objects.requireNonNull(existingProviders).add(locationProvider);
                return;
            case ACTION_STOP:
                if (device != null) {
                    stopDevice(device);
                    updateNotification();
                } else {
                    stopAll();
                }
                return;
            case ACTION_STOP_ALL:
                stopAll();
                return;
            default:
                LOG.warn("Unknown action {}", intent.getAction());
        }
    }

    public void stopDevice(final GBDevice device) {
        LOG.debug("Stopping location providers for {}", device.getAliasOrName());

        final List<GBLocationProvider> providers = providersByDevice.remove(device);
        if (providers != null) {
            for (final GBLocationProvider provider : providers) {
                provider.stop();
            }
        }
    }

    public IntentFilter buildFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_START);
        intentFilter.addAction(ACTION_STOP);
        return intentFilter;
    }

    public void stopAll() {
        LOG.info("Stopping location service for all devices");

        final List<GBDevice> gbDevices = new ArrayList<>(providersByDevice.keySet());
        for (GBDevice d : gbDevices) {
            stopDevice(d);
        }

        updateNotification();
    }

    public static void start(final Context context,
                             @NonNull final GBDevice device,
                             final GBLocationProviderType providerType,
                             final int updateInterval) {
        final Intent intent = new Intent(ACTION_START);
        intent.putExtra(GBDevice.EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_TYPE, providerType.name());
        intent.putExtra(EXTRA_INTERVAL, updateInterval);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void stop(final Context context, @Nullable final GBDevice device) {
        final Intent intent = new Intent(ACTION_STOP);
        intent.putExtra(GBDevice.EXTRA_DEVICE, device);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void updateNotification() {
        if (!providersByDevice.isEmpty()) {
            final Intent notificationIntent = new Intent(context, GBLocationService.class);
            notificationIntent.setPackage(BuildConfig.APPLICATION_ID);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            final PendingIntent pendingIntent = PendingIntentUtils.getActivity(context, 0, notificationIntent, 0, false);

            final NotificationCompat.Builder nb = new NotificationCompat.Builder(context, GB.NOTIFICATION_CHANNEL_ID_GPS)
                    .setTicker(context.getString(R.string.notification_gps_title))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(context.getString(R.string.notification_gps_title))
                    .setContentText(context.getString(R.string.notification_gps_text, providersByDevice.size()))
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_gps_location)
                    .setOngoing(true);

            GB.notify(GB.NOTIFICATION_ID_GPS, nb.build(), context);
        } else {
            GB.removeNotification(GB.NOTIFICATION_ID_GPS, context);
        }
    }
}
