package nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries;

import static nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.BATTERY_UNKNOWN;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGatt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.BFH16Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class BandWPSeriesDeviceSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BandWPSeriesDeviceSupport.class);

    private static final UUID UUID_RPC_SERVICE = UUID.fromString("85ba93a5-09ac-439a-8cc4-1c3f0cb4f29f");
    private static final UUID UUID_RPC_RESPONSE_CHARACTERISTIC = UUID.fromString("cb909093-3559-4b0c-9a7f-3f1773122fdc");

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
        BandWBLEProfile.requestFirmware(builder);
        BandWBLEProfile.requestDeviceName(builder);
        BandWBLEProfile.requestBatteryLevels(builder);
        return builder;
    }

    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();

        if (UUID_RPC_RESPONSE_CHARACTERISTIC.equals(characteristicUUID)) {
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

    @Override
    public boolean useAutoConnect() {
        return true;
    }
}
