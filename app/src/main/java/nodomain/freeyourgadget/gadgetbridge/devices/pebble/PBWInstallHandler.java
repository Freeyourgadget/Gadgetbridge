package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.content.Context;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class PBWInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PBWInstallHandler.class);

    private final Context mContext;
    private PBWReader mPBWReader;
    private final Uri mUri;

    public PBWInstallHandler(Uri uri, Context context) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }
        if (device.getType() != DeviceType.PEBBLE || !device.isConnected()) {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }

        String hwRev = device.getHardwareVersion();
        String platformName;
        if (hwRev.startsWith("snowy")) {
            platformName = "basalt";
        } else if (hwRev.startsWith("spalding")) {
            platformName = "chalk";
        } else {
            platformName = "aplite";
        }

        try {
            mPBWReader = new PBWReader(mUri, mContext, platformName);
        } catch (FileNotFoundException e) {
            installActivity.setInfoText("file not found");
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!mPBWReader.isValid()) {
            installActivity.setInfoText("pbw/pbz is broken or incompatible with your Hardware or Firmware.");
            installActivity.setInstallEnabled(false);
            return;
        }

        GenericItem installItem = new GenericItem();
        installItem.setIcon(R.drawable.ic_watchapp); // FIXME: do not set twice

        if (mPBWReader.isFirmware()) {
            installItem.setIcon(R.drawable.ic_firmware);

            String hwRevision = mPBWReader.getHWRevision();
            if (hwRevision != null && hwRevision.equals(device.getHardwareVersion())) {
                installItem.setName(mContext.getString(R.string.pbw_installhandler_pebble_firmware, ""));
                installItem.setDetails(mContext.getString(R.string.pbwinstallhandler_correct_hw_revision));

                installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, hwRevision));
                installActivity.setInstallEnabled(true);
            } else {
                if (hwRevision != null) {
                    installItem.setName(mContext.getString(R.string.pbw_installhandler_pebble_firmware, hwRevision));
                    installItem.setDetails(mContext.getString(R.string.pbwinstallhandler_incorrect_hw_revision));
                }
                installActivity.setInfoText(mContext.getString(R.string.pbw_install_handler_hw_revision_mismatch));
                installActivity.setInstallEnabled(false);
            }
        } else {
            GBDeviceApp app = mPBWReader.getGBDeviceApp();
            if (app != null) {
                installItem.setName(app.getName());
                installItem.setDetails(mContext.getString(R.string.pbwinstallhandler_app_item, app.getCreator(), app.getVersion()));
                int drawable;
                if (mPBWReader.isLanguage()) {
                    drawable = R.drawable.ic_languagepack;
                } else {
                    switch (app.getType()) {
                        case WATCHFACE:
                            drawable = R.drawable.ic_watchface;
                            break;
                        case APP_ACTIVITYTRACKER:
                            drawable = R.drawable.ic_activitytracker;
                            break;
                        default:
                            drawable = R.drawable.ic_watchapp;
                    }
                }
                installItem.setIcon(drawable);

                installActivity.setInfoText(mContext.getString(R.string.app_install_info, app.getName(), app.getVersion(), app.getCreator()));
                installActivity.setInstallEnabled(true);
            } else {
                installActivity.setInfoText(mContext.getString(R.string.pbw_install_handler_unable_to_install, mUri.getPath()));
                installActivity.setInstallEnabled(false);
            }
        }

        if (installItem.getName() != null) {
            installActivity.setInstallItem(installItem);
        }
    }

    @Override
    public void onStartInstall(GBDevice device) {
        if (mPBWReader.isFirmware() || mPBWReader.isLanguage()) {
            return;
        }

        if (!device.getFirmwareVersion().startsWith("v3")) {
            return;
        }

        File destDir;
        GBDeviceApp app = mPBWReader.getGBDeviceApp();
        try {
            destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            destDir.mkdirs();
            FileUtils.copyURItoFile(mContext, mUri, new File(destDir, app.getUUID().toString() + ".pbw"));
        } catch (IOException e) {
            LOG.error("Installation failed: " + e.getMessage(), e);
            return;
        }

        File outputFile = new File(destDir, app.getUUID().toString() + ".json");
        Writer writer;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            LOG.error("Failed to open output file: " + e.getMessage(), e);
            return;
        }
        try {
            LOG.info(app.getJSON().toString());
            JSONObject appJSON = app.getJSON();
            JSONObject appKeysJSON = mPBWReader.getAppKeysJSON();
            if (appKeysJSON != null) {
                appJSON.put("appKeys", appKeysJSON);
            }
            writer.write(appJSON.toString());

            writer.close();
        } catch (IOException e) {
            LOG.error("Failed to write to output file: " + e.getMessage(), e);
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }

        String jsConfigFile = mPBWReader.getJsConfigurationFile();

        if (jsConfigFile != null) {
            outputFile = new File(destDir, app.getUUID().toString() + "_config.js");
            try {
                writer = new BufferedWriter(new FileWriter(outputFile));
            } catch (IOException e) {
                LOG.error("Failed to open output file: " + e.getMessage(), e);
                return;
            }
            try {
                writer.write(jsConfigFile);
                writer.close();
            } catch (IOException e) {
                LOG.error("Failed to write to output file: " + e.getMessage(), e);
            }
        }
    }

    public boolean isValid() {
        // always pretend it is valid, as we cant know yet about hw/fw version
        return true;
    }

}