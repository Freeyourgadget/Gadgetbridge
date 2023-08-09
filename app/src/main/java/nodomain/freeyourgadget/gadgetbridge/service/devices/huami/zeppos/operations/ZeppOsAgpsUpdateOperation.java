/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAgpsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Updates the AGPS EPO on a Zepp OS device. Update goes as follows:
 * 1. Request an upload start from {@link ZeppOsAgpsService}
 * 2. After successful ack from 1, upload the file to agps://upgrade using {@link ZeppOsFileTransferService}
 * 3. After successful ack from 2, trigger the actual update with {@link ZeppOsAgpsService}
 * 4. After successful ack from 3, update is finished. Trigger an AGPS config request from {@link ZeppOsConfigService}
 * to reload the AGPS update and expiration timestamps.
 */
public class ZeppOsAgpsUpdateOperation extends AbstractBTLEOperation<Huami2021Support>
        implements ZeppOsFileTransferService.Callback, ZeppOsAgpsService.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAgpsUpdateOperation.class);

    private static final String AGPS_UPDATE_URL = "agps://upgrade";
    private static final String AGPS_UPDATE_FILE = "uih.bin";

    private final ZeppOsAgpsFile file;
    private final byte[] fileBytes;

    private final ZeppOsAgpsService agpsService;
    private final ZeppOsFileTransferService fileTransferService;
    private final ZeppOsConfigService configService;

    public ZeppOsAgpsUpdateOperation(final Huami2021Support support,
                                     final ZeppOsAgpsFile file,
                                     final ZeppOsAgpsService agpsService,
                                     final ZeppOsFileTransferService fileTransferService,
                                     final ZeppOsConfigService configService) {
        super(support);
        this.file = file;
        this.fileBytes = file.getUihhBytes();
        this.agpsService = agpsService;
        this.fileTransferService = fileTransferService;
        this.configService = configService;
    }

    @Override
    protected void doPerform() throws IOException {
        agpsService.setCallback(this);
        agpsService.startUpload(file.getUihhBytes().length);
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null && getDevice().isConnected()) {
            unsetBusy();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    @Override
    public void onFileUploadFinish(final boolean success) {
        LOG.info("Finished file upload operation, success={}", success);

        agpsService.startUpdate();
    }

    @Override
    public void onFileUploadProgress(final int progress) {
        LOG.trace("File upload operation progress: {}", progress);

        // This makes the progress go from 0% to 50%, during file upload the other 50% are incremented
        // by the update process on the watch
        final int progressPercent = (int) ((((float) (progress)) / (fileBytes.length * 2)) * 100);
        updateProgress(progressPercent);
    }

    @Override
    public void onFileDownloadFinish(final String url, final String filename, final byte[] data) {
        LOG.warn("Received unexpected file: url={} filename={} length={}", url, filename, data.length);
    }

    @Override
    public void onAgpsUploadStartResponse(final boolean success) {
        if (!success) {
            onFinish(false);
            return;
        }

        fileTransferService.sendFile(AGPS_UPDATE_URL, AGPS_UPDATE_FILE, fileBytes, this);
    }

    @Override
    public void onAgpsProgressResponse(final int size, final int progress) {
        // First 50% are from file upload, so this one starts at 50%
        final int progressPercent = (int) ((((float) (size + progress)) / (size * 2)) * 100);
        updateProgress(progressPercent);
    }

    @Override
    public void onAgpsUpdateFinishResponse(final boolean success) {
        if (success) {
            try {
                final TransactionBuilder builder = performInitialized("request agps config");
                configService.requestConfig(builder, ZeppOsConfigService.ConfigGroup.AGPS);
                builder.queue(getQueue());
            } catch (final Exception e) {
                LOG.error("Failed to request agps config", e);
            }
        }

        onFinish(success);
    }

    private void updateProgress(final int progressPercent) {
        try {
            final TransactionBuilder builder = performInitialized("send agps update progress");
            builder.add(new SetProgressAction(getContext().getString(R.string.updatefirmwareoperation_update_in_progress), true, progressPercent, getContext()));
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    private void onFinish(final boolean success) {
        LOG.info("Finished agps update operation, success={}", success);

        agpsService.setCallback(null);

        final String notificationMessage = success ?
                getContext().getString(R.string.updatefirmwareoperation_update_complete) :
                getContext().getString(R.string.updatefirmwareoperation_write_failed);

        GB.updateInstallNotification(notificationMessage, false, 100, getContext());

        operationFinished();
    }
}
