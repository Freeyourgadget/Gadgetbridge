package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class NothingIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(NothingIOThread.class);

    private final NothingProtocol mNothingProtocol;

    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return mNothingProtocol.UUID_DEVICE_CTRL;
    }

    public NothingIOThread(GBDevice device, Context context, NothingProtocol deviceProtocol, Ear1Support ear1Support, BluetoothAdapter bluetoothAdapter) {
        super(device, context, deviceProtocol, ear1Support, bluetoothAdapter);
        mNothingProtocol = deviceProtocol;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }

}
