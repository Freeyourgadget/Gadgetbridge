package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.ResultCode;

public class VerifyPrivateKeyRequest extends FossilRequest {
    private final FossilHRWatchAdapter adapter;
    private byte[] key, randomPhoneNumber;
    private boolean isFinished = false;

    public VerifyPrivateKeyRequest(byte[] key, FossilHRWatchAdapter adapter) {
        this.adapter = adapter;
        this.key = key;

    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
        byte[] value = characteristic.getValue();

        ByteBuffer buffer = ByteBuffer.wrap(value);

        if (value[1] == 1) {
            try {
                byte[] bytesToDecrypt = new byte[16];

                buffer.position(4);

                buffer.get(bytesToDecrypt, 0, 16);

                SecretKeySpec keySpec = new SecretKeySpec(this.key, "AES");
                Cipher cipher = null;
                cipher = Cipher.getInstance("AES/CBC/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
                byte[] result = cipher.doFinal(bytesToDecrypt);

                byte[] bytesToEncrypt = new byte[16];

                System.arraycopy(result, 0, bytesToEncrypt, 8, 8);
                System.arraycopy(result, 8, bytesToEncrypt, 0, 8);

                byte[] watchRandomNumber = new byte[8];
                System.arraycopy(result, 0, watchRandomNumber, 0, 8);

                adapter.setWatchRandomNumber(watchRandomNumber);
                adapter.setPhoneRandomNumber(randomPhoneNumber);

                cipher = Cipher.getInstance("AES/CBC/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
                result = cipher.doFinal(bytesToEncrypt);

                byte[] payload = new byte[19];
                payload[0] = 2;
                payload[1] = 2;
                payload[2] = 1;

                System.arraycopy(result, 0, payload, 3, 16);

                new TransactionBuilder("send encrypted random numbers")
                        .write(characteristic, payload)
                        .queue(this.adapter.getDeviceSupport().getQueue());
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
        } else if (value[1] == 2) {
            ResultCode code = ResultCode.fromCode(value[2]);

            if (!code.inidicatesSuccess()) throw new RuntimeException("Authentication error: " + code + "   (" + value[2] + ")");


            this.isFinished = true;
        }
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public byte[] getStartSequence() {
        ByteBuffer buffer = ByteBuffer.allocate(11);

        buffer.put((byte) 0x02);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x01);

        this.randomPhoneNumber = new byte[8];

        new Random().nextBytes(randomPhoneNumber);

        buffer.put(randomPhoneNumber);

        return buffer.array();
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0005-957f-7d4a-34a6-74696673696d");
    }
}
