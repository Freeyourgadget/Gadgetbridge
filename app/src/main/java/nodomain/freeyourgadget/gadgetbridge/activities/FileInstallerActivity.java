/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, José Rebelo, Lem Dulfo, Petr Vaněk, Taavi Eomäe

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

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

/**
 * An entrypoint activity to install a file to a device. This activity will find the compatible
 * {@link InstallHandler} and pass the corresponding {@link DeviceType} to the corresponding install
 * activity.
 */
public class FileInstallerActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(FileInstallerActivity.class);

    private Uri uri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_installer);

        uri = getIntent().getData();
        if (uri == null) { // For "share" intent
            uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        }

        new FindInstallHandlerTask().execute(uri);
    }

    private List<DeviceType> getAllDeviceTypesConnectedFirst() {
        final Set<DeviceCoordinator> connectedCoordinators = GBApplication.app().getDeviceManager()
                .getSelectedDevices().stream()
                .filter(GBDevice::isConnected)
                .map(GBDevice::getDeviceCoordinator)
                .collect(Collectors.toSet());

        return Arrays.stream(DeviceType.values())
                .sorted((d1, d2) -> {
                    final DeviceCoordinator c1 = d1.getDeviceCoordinator();
                    final DeviceCoordinator c2 = d2.getDeviceCoordinator();
                    if (connectedCoordinators.contains(c1)) {
                        return -1;
                    } else if (connectedCoordinators.contains(c2)) {
                        return 1;
                    } else {
                        return 0;
                    }
                }).collect(Collectors.toList());
    }

    private class FindInstallHandlerTask extends AsyncTask<Uri, Void, FindInstallHandlerTask.Result> {
        @Override
        protected Result doInBackground(final Uri... uris) {
            for (final DeviceType deviceType : getAllDeviceTypesConnectedFirst()) {
                final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
                final InstallHandler handler = coordinator.findInstallHandler(uris[0], FileInstallerActivity.this);
                if (handler != null) {
                    LOG.info("Found install handler {} from {}", handler.getClass(), coordinator.getClass());
                    return new Result(deviceType, handler);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Result result) {
            final Intent intent;
            if (result != null) {
                if (result.installHandler.getInstallActivity() != null) {
                    intent = new Intent(FileInstallerActivity.this, result.installHandler.getInstallActivity());
                } else {
                    intent = new Intent(FileInstallerActivity.this, FwAppInstallerActivity.class);
                }
                intent.putExtra(FwAppInstallerActivity.EXTRA_DEVICE_TYPE_NAME, result.deviceType.name());
            } else {
                intent = new Intent(FileInstallerActivity.this, FwAppInstallerActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, null);
            startActivity(intent);
            finish();
        }

        private class Result {
            private final DeviceType deviceType;
            private final InstallHandler installHandler;

            private Result(final DeviceType deviceType, final InstallHandler installHandler) {
                this.deviceType = deviceType;
                this.installHandler = installHandler;
            }
        }
    }
}
