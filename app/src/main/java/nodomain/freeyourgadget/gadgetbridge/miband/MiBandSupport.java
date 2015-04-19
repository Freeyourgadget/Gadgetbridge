package nodomain.freeyourgadget.gadgetbridge.miband;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBCommand;
import nodomain.freeyourgadget.gadgetbridge.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.btle.TransactionBuilder;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class MiBandSupport extends AbstractBTLEDeviceSupport {

    private static final String TAG = MiBandSupport.class.getSimpleName();

    public MiBandSupport() {
        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        pair(builder).sendUserInfo(builder);
        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private byte[] getDefaultNotification() {
        final int vibrateTimes = 1;
        final long vibrateDuration = 250l;
        final int flashTimes = 1;
        final int flashColour = 0xFFFFFFFF;
        final int originalColour = 0xFFFFFFFF;
        final long flashDuration = 250l;

        return getNotification(vibrateDuration, vibrateTimes, flashTimes, flashColour, originalColour, flashDuration);
    }

    private void sendDefaultNotification(TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        Log.i(TAG, "Sending notification to MiBand: " + characteristic);
        builder.write(characteristic, getDefaultNotification()).queue(getQueue());
    }

    private byte[] getNotification(long vibrateDuration, int vibrateTimes, int flashTimes, int flashColour, int originalColour, long flashDuration) {
        byte[] vibrate = new byte[]{(byte) 8, (byte) 1};
        byte r = 6;
        byte g = 0;
        byte b = 6;
        boolean display = true;
        //      byte[] flashColor = new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 };
        return vibrate;
    }

    private UserInfo getUserInfo() {
        //      SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
        //      UserInfo mInfo = new UserInfo(
        //              mSharedPreferences.getString(MiBandConstants.PREFERENCE_MAC_ADDRESS, ""),
        //              "1550050550",
        //              (mSharedPreferences.getString(MiBandConstants.PREFERENCE_GENDER, "Male") == "Male") ? 1 : 0,
        //              Integer.parseInt(mSharedPreferences.getString(MiBandConstants.PREFERENCE_AGE, "25")),
        //              Integer.parseInt(mSharedPreferences.getString(MiBandConstants.PREFERENCE_HEIGHT, "175")),
        //              Integer.parseInt(mSharedPreferences.getString(MiBandConstants.PREFERENCE_WEIGHT, "60")),
        //              0
        //      );
        return UserInfo.getDefault(getDevice().getAddress());
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private MiBandSupport sendUserInfo(TransactionBuilder builder) {
        Log.d(TAG, "Writing User Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_USER_INFO);
        builder.write(characteristic, getUserInfo().getData());
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private MiBandSupport pair(TransactionBuilder transaction) {
        Log.i(TAG, "Attempting to pair MI device...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_PAIR);
        if (characteristic != null) {
            transaction.write(characteristic, new byte[]{2});
        } else {
            Log.i(TAG, "Unable to pair MI device -- characteristic not available");
        }
        return this;
    }

    private void performDefaultNotification(String task) {
        try {
            TransactionBuilder builder = performInitialized(task);
            sendDefaultNotification(builder);
        } catch (IOException ex) {
            Log.e(TAG, "Unable to send notification to MI device", ex);
        }
    }

    @Override
    public void onSMS(String from, String body) {
        performDefaultNotification("sms received");
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        performDefaultNotification("email received");
    }

    @Override
    public void onSetTime(long ts) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetCallState(String number, String name, GBCommand command) {
        if (GBCommand.CALL_INCOMING.equals(command)) {
            performDefaultNotification("incoming call");
        }
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onFirmwareVersionReq() {
        try {
            TransactionBuilder builder = performInitialized("Get MI Band Device Info");
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO);
            builder.read(characteristic).queue(getQueue());
        } catch (IOException ex) {
            Log.e(TAG, "Unable to read device info from MI", ex);
        }
    }

    @Override
    public void onAppInfoReq() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAppDelete(int id, int index) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPhoneVersion(byte os) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO.equals(characteristic.getUuid())) {
            handleDeviceInfo(characteristic.getValue(), status);
        }
    }

    private void handleDeviceInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            DeviceInfo info = new DeviceInfo(value);
            getDevice().setFirmwareVersion(info.getFirmwareVersion());
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_PAIR.equals(characteristicUUID)) {
            handlePairResult(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_USER_INFO.equals(characteristicUUID)) {
            handleUserInfoResult(characteristic.getValue(), status);
        }
    }

    private void handleUserInfoResult(byte[] value, int status) {
        // successfully transfered user info means we're initialized
        if (status == BluetoothGatt.GATT_SUCCESS) {
            setConnectionState(State.INITIALIZED);
        }
    }

    private void setConnectionState(State newState) {
        getDevice().setState(newState);
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    private void handlePairResult(byte[] pairResult, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "Pairing MI device failed: " + status);
            return;
        }

        Object value = null;
        if (pairResult != null) {
            if (pairResult.length == 1) {
                try {
                    byte b = pairResult[0];
                    Integer intValue = Integer.valueOf(b);
                    if (intValue.intValue() == 2) {
                        Log.i(TAG, "Successfully paired  MI device");
                        return;
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Error identifying pairing result", ex);
                    return;
                }
            }
            value = pairResult.toString();
        }
        Log.i(TAG, "MI Band pairing result: " + value);
    }
}
