package nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANDW_PSERIES_GUI_VPT_LEVEL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANDW_PSERIES_VPT_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANDW_PSERIES_VPT_LEVEL;
import static nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.BATTERY_UNKNOWN;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries.BandWBLEProfile.ANC_MODE_ON;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGatt;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.BFH16Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class BandWPSeriesDeviceSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BandWPSeriesDeviceSupport.class);

    private static final UUID UUID_RPC_SERVICE = UUID.fromString("85ba93a5-09ac-439a-8cc4-1c3f0cb4f29f");
    private static final UUID UUID_RPC_RESPONSE_CHARACTERISTIC = UUID.fromString("cb909093-3559-4b0c-9a7f-3f1773122fdc");
    private static final UUID UUID_RPC_NOTIFICATION_CHARACTERISTIC = UUID.fromString("df55d475-9a32-457a-9e20-38cf14e853fb");

    private final BandWBLEProfile<BandWPSeriesDeviceSupport> BandWBLEProfile;
    private final GBDeviceEventBatteryInfo[] batteryInfo = new GBDeviceEventBatteryInfo[3];

    public BandWPSeriesDeviceSupport() {
        super(LOG);
        addSupportedService(BFH16Constants.BFH16_GENERIC_ATTRIBUTE_SERVICE);
        addSupportedService(BFH16Constants.BFH16_GENERIC_ACCESS_SERVICE);
        addSupportedService(UUID_RPC_SERVICE);

        BandWBLEProfile = new BandWBLEProfile<>(this);
        addSupportedProfile(BandWBLEProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        getDevice().setBatteryLabel(R.string.left_earbud, 0);
        getDevice().setBatteryLabel(R.string.right_earbud, 1);
        getDevice().setBatteryLabel(R.string.battery_case, 2);

        for (int i = 0; i < 3; i++) {
            batteryInfo[i] = new GBDeviceEventBatteryInfo();
            batteryInfo[i].batteryIndex = i;
            batteryInfo[i].level = BATTERY_UNKNOWN;
            handleGBDeviceEvent(batteryInfo[i]);
        }

        // mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        builder.notify(getCharacteristic(UUID_RPC_RESPONSE_CHARACTERISTIC), true);
        builder.notify(getCharacteristic(UUID_RPC_NOTIFICATION_CHARACTERISTIC), true);
        BandWBLEProfile.requestFirmware(builder);
        BandWBLEProfile.requestDeviceName(builder);
        BandWBLEProfile.requestBatteryLevels(builder);
        BandWBLEProfile.requestAncModeState(builder);
        BandWBLEProfile.requestVptEnabled(builder);
        BandWBLEProfile.requestVptLevel(builder);
        return builder;
    }

    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();

        if (UUID_RPC_RESPONSE_CHARACTERISTIC.equals(characteristicUUID) || UUID_RPC_NOTIFICATION_CHARACTERISTIC.equals(characteristicUUID)) {
            return handleRPCResponse(characteristic);
        }
        return false;
    }

    private boolean handleRPCResponse(BluetoothGattCharacteristic characteristic) {
        BandWPSeriesResponse response = new BandWPSeriesResponse(characteristic.getValue());
        LOG.debug("Got RPC response: Type {}, commandID {}, namespace {}, errorCode {}, payload {}",
                response.messageType,
                response.commandId,
                response.namespace,
                response.errorCode,
                response.payload);
        if (response.errorCode != 0) {
            return false;
        }
        if (response.namespace == 0x02) {
            if (response.commandId == 0x01) {
                return handleFirmwareVersionResponse(response);
            }
        } else if (response.namespace == 0x03) {
            switch (response.commandId) {
                case 0x01:
                    return handleGetAncModeStateResponse(response);
                case 0x02:
                case 0x04:
                    return getIntResponseStatus(response);
                case 0x03:
                    return handleGetVptLevelResponse(response);
                case 0x05:
                    return handleGetVptEnabledResponse(response);
                case 0x06:
                    return getBooleanResponseStatus(response);
            }
        } else if (response.namespace == 0x05) {
            if (response.commandId == 0x01) {
                return handleDeviceNameResponse(response);
            }
        } else if (response.namespace == 0x08) {
            if (response.commandId == 0x17) {
                return handleBatteryLevels(response);
            }
        }
        return true;
    }

    private boolean handleGetAncModeStateResponse(BandWPSeriesResponse response) {
        if (!response.messageType.hasPayload) {
            GB.toast("No payload in response!", Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        int payloadValue;
        try {
            payloadValue = response.payloadUnpacker.unpackInt();
        } catch (IOException e) {
            GB.toast("Could not extract ancMode from payload: " + Arrays.toString(response.payload), Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
        editor.putBoolean(PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, payloadValue == ANC_MODE_ON);
        editor.apply();
        return true;
    }

    private boolean handleBatteryLevels(BandWPSeriesResponse response) {
        int[] levels = response.getPayloadFixArray();
        if (levels == null) {
            return false;
        }
        for (int i = 0; i < levels.length; i++) {
            if (i >= 3) {
                break;
            }
            int level = (levels[i] == 0xff) ? BATTERY_UNKNOWN : levels[i];
            LOG.debug("Battery {} has level {}", i, levels[i]);
            batteryInfo[i].level = level;
            handleGBDeviceEvent(batteryInfo[i]);
        }
        return true;
    }

    private boolean handleFirmwareVersionResponse(BandWPSeriesResponse response) {
        String firmwareString = response.getPayloadString();
        if (firmwareString == null) {
            return false;
        }
        String[] versions = firmwareString.split("\\(");
        String main_version = versions[0];
        String sub_version = versions[1].substring(0, versions[1].length()-1);
        GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();
        versionInfo.fwVersion = main_version;
        versionInfo.fwVersion2 = sub_version;
        LOG.debug("Got firmware version {}/{}", main_version, sub_version);
        handleGBDeviceEvent(versionInfo);
        return true;
    }

    private boolean handleDeviceNameResponse(BandWPSeriesResponse response) {
        String deviceName = response.getPayloadString();
        if (deviceName == null) {
            return false;
        }
        getDevice().setName(deviceName);
        LOG.debug("Set device name to {}", deviceName);
        return true;
    }

    private boolean handleGetVptEnabledResponse(BandWPSeriesResponse response) {
        if (!response.messageType.hasPayload) {
            GB.toast("No payload in response!", Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        boolean payloadValue;
        try {
            payloadValue = response.payloadUnpacker.unpackBoolean();
        } catch (IOException e) {
            GB.toast("Could not extract vptEnabled from payload: " + Arrays.toString(response.payload), Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        int vptLevel = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getInt(PREF_BANDW_PSERIES_VPT_LEVEL, 0);
        Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
        editor.putBoolean(PREF_BANDW_PSERIES_VPT_ENABLED, payloadValue);
        editor.putInt(PREF_BANDW_PSERIES_GUI_VPT_LEVEL, payloadValue ? vptLevel + 1 : 0);
        editor.apply();
        return true;
    }

    private boolean handleGetVptLevelResponse(BandWPSeriesResponse response) {
        if (!response.messageType.hasPayload) {
            GB.toast("No payload in response!", Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        int payloadValue;
        try {
            payloadValue = response.payloadUnpacker.unpackInt();
        } catch (IOException e) {
            GB.toast("Could not extract vptLevel from payload: " + Arrays.toString(response.payload), Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        boolean vptEnabled = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(PREF_BANDW_PSERIES_VPT_ENABLED, false);
        Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
        editor.putInt(PREF_BANDW_PSERIES_VPT_LEVEL, payloadValue);
        editor.putInt(PREF_BANDW_PSERIES_GUI_VPT_LEVEL, vptEnabled ? payloadValue + 1 : 0);
        editor.apply();
        return true;
    }

    public void onSendConfiguration(String config) {
        try {
            TransactionBuilder builder = performInitialized("sendConfig");
            switch (config) {
                case PREF_ACTIVE_NOISE_CANCELLING_TOGGLE:
                    boolean ancMode = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, true);
                    BandWBLEProfile.setAncModeState(builder, ancMode);
                    break;
                case PREF_BANDW_PSERIES_GUI_VPT_LEVEL:
                    int level = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getInt(PREF_BANDW_PSERIES_GUI_VPT_LEVEL, 0);
                    BandWBLEProfile.setVptEnabled(builder, level != 0);
                    if (level != 0) {
                        BandWBLEProfile.setVptLevel(builder, level - 1);
                    }
                    break;
            }
            performImmediately(builder);
        } catch (IOException e) {
            GB.toast("Failed to send settings update", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private boolean getBooleanResponseStatus(BandWPSeriesResponse response) {
        boolean payloadValue;
        try {
            payloadValue = response.payloadUnpacker.unpackBoolean();
        } catch (IOException e) {
            GB.toast("Could not extract response from payload: " + Arrays.toString(response.payload), Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        return payloadValue;
    }

    private boolean getIntResponseStatus(BandWPSeriesResponse response) {
        int payloadValue;
        try {
            payloadValue = response.payloadUnpacker.unpackInt();
        } catch (IOException e) {
            GB.toast("Could not extract response from payload: " + Arrays.toString(response.payload), Toast.LENGTH_SHORT, GB.ERROR);
            return false;
        }
        return payloadValue == 0;
    }
}
