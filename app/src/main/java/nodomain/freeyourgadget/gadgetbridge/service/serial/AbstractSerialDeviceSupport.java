package nodomain.freeyourgadget.gadgetbridge.service.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

/**
 * An abstract base class for devices speaking a serial protocol, like via
 * an rfcomm bluetooth socket or a TCP socket.
 *
 * This class uses two helper classes to deal with that:
 * - GBDeviceIoThread, which creates and maintains the actual socket connection and implements the transport layer
 * - GBDeviceProtocol, which implements the encoding and decoding of messages, i.e. the actual device specific protocol
 *
 * Note that these two classes need to be implemented in a device specific way.
 *
 * This implementation implements all methods of {@link EventHandler}, calls the {@link GBDeviceProtocol device protocol}
 * to create the device specific message for the respective events and sends them to the device via {@link #sendToDevice(byte[])}.
 */
public abstract class AbstractSerialDeviceSupport extends AbstractDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);

    private GBDeviceProtocol gbDeviceProtocol;
    private GBDeviceIoThread gbDeviceIOThread;

    /**
     * Factory method to create the device specific GBDeviceProtocol instance to be used.
     */
    protected abstract GBDeviceProtocol createDeviceProtocol();

    /**
     * Factory method to create the device specific GBDeviceIoThread instance to be used.
     */
    protected abstract GBDeviceIoThread createDeviceIOThread();

    @Override
    public void dispose() {
        // currently only one thread allowed
        if (gbDeviceIOThread != null) {
            gbDeviceIOThread.quit();
            try {
                gbDeviceIOThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gbDeviceIOThread = null;
        }
    }

    @Override
    public void pair() {
        // Default implementation does no manual pairing, use the Android
        // pairing dialog instead.
    }

    /**
     * Lazily creates and returns the GBDeviceProtocol instance to be used.
     */
    public synchronized GBDeviceProtocol getDeviceProtocol() {
        if (gbDeviceProtocol == null) {
            gbDeviceProtocol = createDeviceProtocol();
        }
        return gbDeviceProtocol;
    }

    /**
     * Lazily creates and returns the GBDeviceIoThread instance to be used.
     */
    public synchronized GBDeviceIoThread getDeviceIOThread() {
        if (gbDeviceIOThread == null) {
            gbDeviceIOThread = createDeviceIOThread();
        }
        return gbDeviceIOThread;
    }

    /**
     * Sends the given message to the device. This implementation delegates the
     * writing to the {@link #getDeviceIOThread device io thread}
     * @param bytes the message to send to the device
     */
    protected void sendToDevice(byte[] bytes) {
        if (bytes != null && gbDeviceIOThread != null) {
            gbDeviceIOThread.write(bytes);
        }
    }

    public void handleGBDeviceEvent(GBDeviceEventSendBytes sendBytes) {
        sendToDevice(sendBytes.encodedBytes);
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {

        switch (deviceEvent.eventClass) {
            case SEND_BYTES:
                handleGBDeviceEvent((GBDeviceEventSendBytes) deviceEvent);
                return;
            default:
                break;
        }

        super.evaluateGBDeviceEvent(deviceEvent);
    }

    @Override
    public void onSMS(String from, String body) {
        byte[] bytes = gbDeviceProtocol.encodeSMS(from, body);
        sendToDevice(bytes);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        byte[] bytes = gbDeviceProtocol.encodeEmail(from, subject, body);
        sendToDevice(bytes);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        byte[] bytes = gbDeviceProtocol.encodeGenericNotification(title, details);
        sendToDevice(bytes);
    }

    @Override
    public void onSetTime(long ts) {
        byte[] bytes = gbDeviceProtocol.encodeSetTime(ts);
        sendToDevice(bytes);
    }

    @Override
    public void onSetCallState(String number, String name, ServiceCommand command) {
        byte[] bytes = gbDeviceProtocol.encodeSetCallState(number, name, command);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        byte[] bytes = gbDeviceProtocol.encodeSetMusicInfo(artist, album, track);
        sendToDevice(bytes);
    }

    @Override
    public void onBatteryInfoReq() {
        byte[] bytes = gbDeviceProtocol.encodeBatteryInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppInfoReq() {
        byte[] bytes = gbDeviceProtocol.encodeAppInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppStart(UUID uuid) {
        byte[] bytes = gbDeviceProtocol.encodeAppStart(uuid);
        sendToDevice(bytes);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        byte[] bytes = gbDeviceProtocol.encodeAppDelete(uuid);
        sendToDevice(bytes);
    }

    @Override
    public void onPhoneVersion(byte os) {
        byte[] bytes = gbDeviceProtocol.encodePhoneVersion(os);
        sendToDevice(bytes);
    }

    @Override
    public void onFetchActivityData() {
        byte[] bytes = gbDeviceProtocol.encodeSynchronizeActivityData();
        sendToDevice(bytes);
    }

    @Override
    public void onReboot() {
        byte[] bytes = gbDeviceProtocol.encodeReboot();
        sendToDevice(bytes);
    }

    @Override
    public void onFindDevice(boolean start) {
        byte[] bytes = gbDeviceProtocol.encodeFindDevice(start);
        sendToDevice(bytes);
    }

    @Override
    public void onScreenshotReq() {
        byte[] bytes = gbDeviceProtocol.encodeScreenshotReq();
        sendToDevice(bytes);
    }
}
