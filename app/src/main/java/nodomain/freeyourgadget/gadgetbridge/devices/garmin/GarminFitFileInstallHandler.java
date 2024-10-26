/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitCourse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitWorkout;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class GarminFitFileInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GarminFitFileInstallHandler.class);

    protected final Context mContext;
    private byte[] rawBytes;
    private FitFile fitFile;
    private FileType.FILETYPE fileType;

    public GarminFitFileInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }

        // Quickly check whether it's a valid fit file without reading the entire thing
        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            final byte[] header = new byte[12];
            final int read = in.read(header);
            if (read != header.length) {
                throw new IOException("Not enough bytes for fit header");
            }
            if (BLETypeConversions.toUint32(header, 8) != FitFile.Header.MAGIC) {
                // not fit file
                return;
            }
        } catch (final Exception e) {
            LOG.error("Failed to validate fit file", e);
            return;
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            rawBytes = FileUtils.readAll(in, 10 * 1024 * 1024); // 10MB
            fitFile = FitFile.parseIncoming(rawBytes);

            final Optional<FitFileId> fitFileIdOpt = fitFile.getRecords().stream()
                    .filter(r -> r instanceof FitFileId)
                    .map(r -> (FitFileId) r)
                    .findFirst();

            if (!fitFileIdOpt.isPresent()) {
                LOG.error("Fit file has no ID");
                return;
            }

            final FitFileId fitFileId = fitFileIdOpt.get();
            if (fitFileId.getType() == null) {
                LOG.error("Fit file ID has null type");
                return;
            }

            fileType = fitFileId.getType();
        } catch (final Exception e) {
            LOG.error("Failed to read fit file", e);
        }
    }

    @Override
    public boolean isValid() {
        return fitFile != null && fileType != null;
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (fitFile == null || fileType == null) {
            return;
        }

        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof GarminCoordinator)) {
            LOG.warn("Coordinator is not a GarminCoordinator: {}", coordinator.getClass());
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }
        final GarminCoordinator garminCoordinator = (GarminCoordinator) coordinator;
        final boolean fileSupported = parseFitFile(installActivity, garminCoordinator, device);

        if (!fileSupported) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
    }

    public byte[] getRawBytes() {
        return rawBytes;
    }

    public FitFile getFitFile() {
        return fitFile;
    }

    public FileType.FILETYPE getFileType() {
        return fileType;
}

    private boolean parseFitFile(final InstallActivity installActivity, final GarminCoordinator coordinator, final GBDevice device) {
        final String name;
        final int kindName;

        switch (fileType) {
            case COURSES:
                if (!coordinator.supports(device, GarminCapability.COURSE_DOWNLOAD)) {
                    LOG.warn("Device does not support course download");
                    return false;
                }
                final Optional<FitCourse> fitCourseOpt = fitFile.getRecords().stream()
                        .filter(r -> r instanceof FitCourse)
                        .map(r -> (FitCourse) r)
                        .findFirst();

                if (!fitCourseOpt.isPresent()) {
                    LOG.error("Fit file has no course record");
                    return false;
                }
                final FitCourse fitCourse = fitCourseOpt.get();

                name = String.valueOf(fitCourse.getName());
                kindName = R.string.kind_gpx_route;

                break;
            case WORKOUTS:
                if (!coordinator.supports(device, GarminCapability.WORKOUT_DOWNLOAD)) {
                    LOG.warn("Device does not support workout download");
                    return false;
                }
                final Optional<FitWorkout> fitWorkoutOpt = fitFile.getRecords().stream()
                        .filter(r -> r instanceof FitWorkout)
                        .map(r -> (FitWorkout) r)
                        .findFirst();

                if (!fitWorkoutOpt.isPresent()) {
                    LOG.error("Fit file has no workout record");
                    return false;
                }
                final FitWorkout fitWorkout = fitWorkoutOpt.get();

                name = String.valueOf(fitWorkout.getName());
                kindName = R.string.menuitem_workout;

                break;
            default:
                LOG.warn("Unsupported fit file type: {}", fileType);
                return false;
        }

        final GenericItem fwItem = new GenericItem(mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                mContext.getString(kindName),
                name
        ));
        fwItem.setIcon(coordinator.getDefaultIconResource());

        final StringBuilder builder = new StringBuilder();
        final String kindNameString = mContext.getString(kindName);
        builder.append(mContext.getString(R.string.fw_upgrade_notice, kindNameString));
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);

        return true;
    }
}
