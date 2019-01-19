package nodomain.freeyourgadget.gadgetbridge.service.devices.xm3;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Xm3IoThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(Xm3IoThread.class);

    public Xm3IoThread(GBDevice gbDevice, Context context, Xm3Protocol xm3protocol, Xm3Support xm3support, BluetoothAdapter roidmiBtAdapter) {
        super(gbDevice, context, xm3protocol, xm3support, roidmiBtAdapter);
    }

    @Override
    protected byte[] parseIncoming(InputStream inputStream) throws IOException {
        ByteArrayOutputStream msgStream = new ByteArrayOutputStream();

        boolean finished = false;
        byte[] incoming = new byte[1];

        while (!finished) {
            inputStream.read(incoming);
            msgStream.write(incoming);

            if (incoming[0] == 0x3c) {
                finished = true;
            }
        }

        byte[] msgArray = msgStream.toByteArray();
        LOG.debug("Received: " + GB.hexdump(msgArray, 0, msgArray.length));
        return msgArray;
    }

    @Override
    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA");
    }
}
