package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioGBX100DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.BcdUtil;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WEARLOCATION;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.GENDER_MALE;

public class SetConfigurationOperation  extends AbstractBTLEOperation<CasioGBX100DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(GetConfigurationOperation.class);
    private final CasioGBX100DeviceSupport support;
    private final CasioConstants.ConfigurationOption option;

    public SetConfigurationOperation(CasioGBX100DeviceSupport support, CasioConstants.ConfigurationOption option) {
        super(support);
        this.support = support;
        this.option = option;
    }

    @Override
    protected void doPerform() throws IOException {
        byte[] command = new byte[1];
        command[0] = CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE");
        TransactionBuilder builder = performInitialized("getConfiguration");
        builder.setGattCallback(this);
        support.writeAllFeaturesRequest(builder, command);
        builder.queue(getQueue());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            byte[] oldData = new byte[data.length];
            System.arraycopy(data, 0, oldData, 0, data.length);

            if (data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE")) {

                ActivityUser user = new ActivityUser();
                boolean all = (option == CasioConstants.ConfigurationOption.OPTION_ALL);
                if (option == CasioConstants.ConfigurationOption.OPTION_GENDER || all) {
                    if (user.getGender() == GENDER_MALE) {
                        data[1] = (byte) (data[1] & ~0x01);
                    } else {
                        data[1] = (byte) (data[1] | 0x01);
                    }
                }

                for(int i=2; i<data.length; i++) {
                    data[i] = (byte)~data[i];
                }

                if (option == CasioConstants.ConfigurationOption.OPTION_HEIGHT || all) {
                    int height = user.getHeightCm();
                    data[2] = BcdUtil.toBcd8(height % 100);
                    data[3] = BcdUtil.toBcd8((height - (height % 100)) / 100);
                }
                if (option == CasioConstants.ConfigurationOption.OPTION_WEIGHT || all) {
                    int weight = user.getWeightKg();
                    data[4] = BcdUtil.toBcd8(weight % 100);
                    data[5] = BcdUtil.toBcd8((weight - (weight % 100)) / 100);
                }
                if (option == CasioConstants.ConfigurationOption.OPTION_WRIST || all) {
                    String location = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getString(PREF_WEARLOCATION, "left");
                    if (location == "right") {
                        data[1] = (byte) (data[1] | 0x02);
                    } else {
                        data[1] = (byte) (data[1] & ~0x02);
                    }
                }
                if(option == CasioConstants.ConfigurationOption.OPTION_BIRTHDAY || all) {
                    int year = user.getYearOfBirth();
                    // Month and Day are not configured in Gadgetbridge!
                    int month = 1;
                    int day = 1;
                    data[6] = BcdUtil.toBcd8(year % 100);
                    data[7] = BcdUtil.toBcd8((year - (year % 100)) / 100);
                    data[8] = BcdUtil.toBcd8(month);
                    data[9] = BcdUtil.toBcd8(day + 1);
                }

                for(int i=2; i<data.length; i++) {
                    data[i] = (byte)~data[i];
                }

                if(Arrays.equals(oldData, data)) {
                    LOG.info("No configuration update required");
                    requestTargetSettings();
                } else {
                    // Target settings will be requested in write callback
                    try {
                        TransactionBuilder builder = performInitialized("setConfiguration");
                        builder.setGattCallback(this);
                        support.writeAllFeatures(builder, data);
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        LOG.info("Error writing configuration to Casio watch");
                    }
                }
                return true;
            } else if (data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_TARGET_VALUE")) {
                ActivityUser user = new ActivityUser();
                boolean all = (option == CasioConstants.ConfigurationOption.OPTION_ALL);

                if(option == CasioConstants.ConfigurationOption.OPTION_STEP_GOAL || all) {
                    int steps = user.getStepsGoal();
                    data[1] = (byte)(steps & 0xff);
                    data[2] = (byte)((steps >> 8) & 0xff);
                }

                if(option == CasioConstants.ConfigurationOption.OPTION_DISTANCE_GOAL || all) {
                    // The watch requires a monthly goal, so we multiply that with 30
                    // and divide it by 100 because the value is set in 100m units
                    int distance = user.getDistanceMeters() * 30;
                    distance = distance / 100;
                    data[6] = (byte)(distance & 0xff);
                    data[7] = (byte)((distance >> 8) & 0xff);
                }

                if(option == CasioConstants.ConfigurationOption.OPTION_ACTIVITY_GOAL || all) {
                    // The watch requires a monthly goal, so we multiply that with 30
                    int time = user.getActiveTimeMinutes() * 30;
                    data[9] = (byte)(time & 0xff);
                    data[10] = (byte)((time >> 8) & 0xff);
                }

                if(Arrays.equals(oldData, data)) {
                    LOG.info("No configuration update required");
                    operationFinished();
                } else {
                    // Operation will be finished in Gatt callback
                    try {
                        TransactionBuilder builder = performInitialized("setConfiguration");
                        builder.setGattCallback(this);
                        support.writeAllFeatures(builder, data);
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        LOG.info("Error writing configuration to Casio watch");
                    }
                }
                return true;
            }

        }
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    protected void operationFinished() {
        LOG.info("SetConfigurationOperation finished");

        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null) {
            unsetBusy();
            try {
                TransactionBuilder builder = performInitialized("finishe operation");
                builder.setGattCallback(null); // unset ourselves from being the queue's gatt callback
                builder.wait(0);
                builder.queue(getQueue());
            } catch (IOException ex) {
                LOG.info("Error resetting Gatt callback: " + ex.getMessage());
            }
        }
    }

    private void requestTargetSettings() {
        byte[] command = new byte[1];
        command[0] = CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_TARGET_VALUE");
        try {
            TransactionBuilder builder = performInitialized("getConfiguration");
            builder.setGattCallback(this);
            support.writeAllFeaturesRequest(builder, command);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.info("Error requesting Casio configuration");
        }
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE")) {
                requestTargetSettings();
                return true;
            }
            if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_TARGET_VALUE")) {
                operationFinished();
                return true;
            }
        }
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }
}