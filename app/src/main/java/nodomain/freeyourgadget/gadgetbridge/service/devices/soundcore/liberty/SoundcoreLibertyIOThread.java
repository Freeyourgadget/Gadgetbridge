package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty;

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
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;

public class SoundcoreLibertyIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreLibertyIOThread.class);
    private final SoundcoreLibertyProtocol mSoundcoreProtocol;
    private final UUID mUuidToConnect;

    public SoundcoreLibertyIOThread(final GBDevice gbDevice,
                                    final Context context,
                                    final SoundcoreLibertyProtocol deviceProtocol,
                                    final UUID uuidToConnect,
                                    final AbstractSerialDeviceSupport deviceSupport,
                                    final BluetoothAdapter btAdapter) {
        super(gbDevice, context, deviceProtocol, deviceSupport, btAdapter);
        mSoundcoreProtocol = deviceProtocol;
        mUuidToConnect = uuidToConnect;
    }

    @Override
    protected void initialize() {
        write(mSoundcoreProtocol.encodeDeviceInfoRequest());
        setUpdateState(GBDevice.State.INITIALIZED);
    }

    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return mUuidToConnect;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read {} bytes. {}", bytes, hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }
}
