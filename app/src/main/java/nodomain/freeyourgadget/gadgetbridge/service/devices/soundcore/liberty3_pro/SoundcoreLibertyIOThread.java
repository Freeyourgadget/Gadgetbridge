package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore;

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

public class SoundcoreLibertyIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreLibertyIOThread.class);
    private final SoundcoreLibertyProtocol mSoundcoreProtocol;

    public SoundcoreLibertyIOThread(GBDevice gbDevice, Context context, SoundcoreLibertyProtocol deviceProtocol, SoundcoreLiberty3ProDeviceSupport deviceSupport, BluetoothAdapter btAdapter) {
        super(gbDevice, context, deviceProtocol, deviceSupport, btAdapter);
        mSoundcoreProtocol = deviceProtocol;
    }

    @Override
    protected void initialize() {
        write(mSoundcoreProtocol.encodeDeviceInfoRequest());
        setUpdateState(GBDevice.State.INITIALIZED);
    }

    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return mSoundcoreProtocol.UUID_DEVICE_CTRL;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }
}
