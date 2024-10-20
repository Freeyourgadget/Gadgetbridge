/*  Copyright (C) 2024 Vitalii Tomin

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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiAppManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFwHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiMusicManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWatchfaceManager;

public class HuaweiInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiInstallHandler.class);

    private final Context context;
    protected final HuaweiFwHelper helper;

    boolean valid = false;

    public HuaweiInstallHandler(Uri uri, Context context) {
        this.context = context;
        this.helper = new HuaweiFwHelper(uri, context);
    }

    private HuaweiMusicUtils.FormatRestrictions getRestriction(HuaweiMusicUtils.MusicCapabilities capabilities, String ext) {
        List<HuaweiMusicUtils.FormatRestrictions> restrictions = capabilities.formatsRestrictions;
        if(restrictions == null)
            return null;

        for(HuaweiMusicUtils.FormatRestrictions r: restrictions) {
            if(ext.equals(r.getName())) {
                return r;
            }
        }
        return null;
    }

    //TODO: add proper checks
    private boolean checkMediaCompatibility(HuaweiMusicUtils.MusicCapabilities capabilities, HuaweiMusicManager.AudioInfo currentMusicInfo) {
        if(capabilities == null) {
            LOG.error("No media info from device");
            return false;
        }
        String ext = currentMusicInfo.getExtension();

        List<String> supportedFormats = capabilities.supportedFormats;
        if(supportedFormats == null) {
            LOG.error("Format not supported {}", ext);
            return false;
        }
        if(!supportedFormats.contains(ext)) {
            LOG.error("Format not supported {}", ext);
            return false;
        }

        HuaweiMusicUtils.FormatRestrictions restrictions = getRestriction(capabilities, ext);
        if(restrictions == null) {
            LOG.info("no restriction for: {}", ext);
            return true;
        }

        LOG.info("bitrate {}", restrictions.bitrate);
        LOG.info("channels {}", restrictions.channels);
        LOG.info("musicEncode {}", restrictions.musicEncode);
        LOG.info("sampleRate {}", restrictions.sampleRate);
        LOG.info("unknownBitrate {}", restrictions.unknownBitrate);

        if(currentMusicInfo.getChannels() > restrictions.channels) {
            LOG.error("Not supported channels count {} > {}", currentMusicInfo.getChannels(), restrictions.channels);
            return false;
        }

        //TODO: check other restrictions.

        return true;
    }


    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof HuaweiCoordinatorSupplier)) {
            LOG.warn("Coordinator is not a HuaweiCoordinatorSupplier: {}", coordinator.getClass());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (helper.isWatchface()) {
            final HuaweiCoordinatorSupplier huaweiCoordinatorSupplier = (HuaweiCoordinatorSupplier) coordinator;

            HuaweiWatchfaceManager.WatchfaceDescription description = helper.getWatchfaceDescription();

            HuaweiWatchfaceManager.Resolution resolution = new HuaweiWatchfaceManager.Resolution();
            String deviceScreen = String.format("%d*%d", huaweiCoordinatorSupplier.getHuaweiCoordinator().getHeight(),
                    huaweiCoordinatorSupplier.getHuaweiCoordinator().getWidth());
            this.valid = resolution.isValid(description.screen, deviceScreen);

            installActivity.setInstallEnabled(true);

            GenericItem installItem = new GenericItem();

            if (helper.getPreviewBitmap() != null) {
                installItem.setPreview(helper.getPreviewBitmap());
            }

            installItem.setName(description.title);
            installActivity.setInstallItem(installItem);
            if (device.isBusy()) {
                LOG.error("Watchface cannot be installed (device busy)");
                installActivity.setInfoText("Watchface cannot be installed (device busy)");
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected()) {
                LOG.error("Watchface cannot be installed (not connected or wrong device)");
                installActivity.setInfoText("Watchface cannot be installed (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("Watchface cannot be installed");
                installActivity.setInfoText(context.getString(R.string.watchface_resolution_doesnt_match,
                        resolution.screenByThemeVersion(description.screen), deviceScreen));
                installActivity.setInstallEnabled(false);
                return;
            }

            installItem.setDetails(description.version);

            installItem.setIcon(R.drawable.ic_watchface);
            installActivity.setInfoText(context.getString(R.string.watchface_install_info, installItem.getName(), description.version, description.author));

            LOG.debug("Initialized HuaweiInstallHandler: Watchface");
        } else if (helper.isAPP()) {
            final HuaweiAppManager.AppConfig config = helper.getAppConfig();

            this.valid = true; //NOTE: nothing to verify for now

            installActivity.setInstallEnabled(true);

            GenericItem installItem = new GenericItem();

            if (helper.getPreviewBitmap() != null) {
                installItem.setPreview(helper.getPreviewBitmap());
            }

            installItem.setName(config.bundleName);
            installActivity.setInstallItem(installItem);
            if (device.isBusy()) {
                LOG.error("App cannot be installed (device busy)");
                installActivity.setInfoText("Firmware cannot be installed (device busy)");
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected()) {
                LOG.error("App cannot be installed (not connected or wrong device)");
                installActivity.setInfoText("Firmware cannot be installed (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("App cannot be installed");
                installActivity.setInstallEnabled(false);
                return;
            }

            installItem.setDetails(config.version);

            installItem.setIcon(R.drawable.ic_watchapp);

            installActivity.setInfoText(context.getString(R.string.app_install_info, installItem.getName(), config.version, config.vendor));

            LOG.debug("Initialized HuaweiInstallHandler: App");
        } else if (helper.isMusic()) {
            final HuaweiCoordinatorSupplier huaweiCoordinatorSupplier = (HuaweiCoordinatorSupplier) coordinator;

            HuaweiMusicUtils.MusicCapabilities capabilities = huaweiCoordinatorSupplier.getHuaweiCoordinator().getExtendedMusicInfoParams();
            if(capabilities == null) {
                capabilities = huaweiCoordinatorSupplier.getHuaweiCoordinator().getMusicInfoParams();
            }
            HuaweiMusicManager.AudioInfo currentMusicInfo = helper.getMusicInfo();

            boolean isMediaCompatible = checkMediaCompatibility(capabilities, currentMusicInfo);

            this.valid = isMediaCompatible && !TextUtils.isEmpty(helper.getMusicInfo().getFileName()) && !TextUtils.isEmpty(helper.getMusicInfo().getArtist()) && !TextUtils.isEmpty(helper.getMusicInfo().getTitle());

            installActivity.setInstallEnabled(true);

            GenericItem installItem = new GenericItem();

            installItem.setName(helper.getFileName());
            installActivity.setInstallItem(installItem);
            if (device.isBusy()) {
                LOG.error("Music cannot be uploaded (device busy)");
                installActivity.setInfoText("Music cannot be uploaded (device busy)");
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected()) {
                LOG.error("Music cannot be uploaded (not connected or wrong device)");
                installActivity.setInfoText("Music cannot be uploaded (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("Music cannot be uploaded");
                installActivity.setInfoText("Music cannot be uploaded");
                installActivity.setInstallEnabled(false);
                return;
            }

            installItem.setDetails(helper.getMusicInfo().getFileName());

            installItem.setIcon(R.drawable.ic_music_note);

            installActivity.setInfoText(context.getString(R.string.app_install_info, helper.getMusicInfo().getFileName(), helper.getMusicInfo().getTitle(), helper.getMusicInfo().getArtist()));

            LOG.debug("Initialized HuaweiInstallHandler: Music");
        }

    }

    @Override
    public boolean isValid() {
        return helper.isValid();
    }

    @Override
    public void onStartInstall(GBDevice device) {
        helper.unsetFwBytes();
    }
}
