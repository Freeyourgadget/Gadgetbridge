package nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

public class BandWBLEProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BandWBLEProfile.class);

    private static final String ACTION_PREFIX = BandWBLEProfile.class.getName() + "_";

    public static final String ACTION_DEVICE_INFO = ACTION_PREFIX + "DEVICE_INFO";
    public static final String EXTRA_DEVICE_INFO = "DEVICE_INFO";

    public static final byte ANC_MODE_OFF = 0x01;
    public static final byte ANC_MODE_ON = 0x03;

    public static final UUID UUID_RPC_REQUEST_CHARACTERISTIC = UUID.fromString("ada50ce9-67b8-4a97-9d8e-37e1d083156c");

    public BandWBLEProfile(final T support) {
        super(support);
    }

    public void requestAncModeState(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x03, (byte) 0x01);
    }
    public void requestDeviceName(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x05, (byte) 0x01);
    }

    public void requestFirmware(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x02, (byte) 0x01);
    }

    public void requestBatteryLevels(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x08, (byte) 0x17);
    }

    public void requestVptEnabled(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x03, (byte) 0x05);
    }

    public void requestVptLevel(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x03, (byte) 0x03);
    }

    public void requestWearSensorEnabled(final TransactionBuilder builder) {
        sendRequest(builder, (byte) 0x0a, (byte) 0x01);
    }

    public void setAncModeState(final TransactionBuilder builder, final boolean mode) throws IOException {
        BandWPSeriesRequest req = new BandWPSeriesRequest((byte) 0x03, (byte) 0x02).addToPayload(mode ? ANC_MODE_ON : ANC_MODE_OFF);
        builder.write(getCharacteristic(UUID_RPC_REQUEST_CHARACTERISTIC), req.finishAndGetBytes());
    }

    public void setVptLevel(final TransactionBuilder builder, final int level) throws IOException {
        BandWPSeriesRequest req = new BandWPSeriesRequest((byte) 0x03, (byte) 0x04).addToPayload(level);
        builder.write(getCharacteristic(UUID_RPC_REQUEST_CHARACTERISTIC), req.finishAndGetBytes());
    }

    public void setVptEnabled(final TransactionBuilder builder, final boolean mode) throws IOException {
        BandWPSeriesRequest req = new BandWPSeriesRequest((byte) 0x03, (byte) 0x06).addToPayload(mode);
        builder.write(getCharacteristic(UUID_RPC_REQUEST_CHARACTERISTIC), req.finishAndGetBytes());
    }

    public void setWearSensorEnabled(final TransactionBuilder builder, final boolean mode) throws IOException {
        BandWPSeriesRequest req = new BandWPSeriesRequest((byte) 0x0a, (byte) 0x02).addToPayload(mode);
        builder.write(getCharacteristic(UUID_RPC_REQUEST_CHARACTERISTIC), req.finishAndGetBytes());
    }

    private void sendRequest(final TransactionBuilder builder, byte namespace, byte commandID) {
        BandWPSeriesRequest req;
        try {
            req = new BandWPSeriesRequest(namespace, commandID);
        } catch (IOException e) {
            LOG.error("Failed to send request: namespace {}, commandID {}", namespace, commandID);
            return;
        }
        builder.write(getCharacteristic(UUID_RPC_REQUEST_CHARACTERISTIC), req.finishAndGetBytes());
    }

}
