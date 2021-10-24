package nodomain.freeyourgadget.gadgetbridge.service.devices.galaxy_buds;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

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

public class GalaxyBudsIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyBudsIOThread.class);

    private final GalaxyBudsProtocol galaxyBudsProtocol;

    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return galaxyBudsProtocol.UUID_DEVICE_CTRL;
    }

    public GalaxyBudsIOThread(GBDevice device, Context context, GalaxyBudsProtocol deviceProtocol,
                              GalaxyBudsDeviceSupport galaxyBudsDeviceSupport, BluetoothAdapter bluetoothAdapter) {
        super(device, context, deviceProtocol, galaxyBudsDeviceSupport, bluetoothAdapter);
        galaxyBudsProtocol = deviceProtocol;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }

}
