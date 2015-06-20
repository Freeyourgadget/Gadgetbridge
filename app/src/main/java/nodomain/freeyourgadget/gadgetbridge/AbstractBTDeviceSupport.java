package nodomain.freeyourgadget.gadgetbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSendBytes;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public abstract class AbstractBTDeviceSupport extends AbstractDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);

    private GBDeviceProtocol gbDeviceProtocol;
    private GBDeviceIoThread gbDeviceIOThread;

    protected abstract GBDeviceProtocol createDeviceProtocol();

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

    public synchronized GBDeviceProtocol getDeviceProtocol() {
        if (gbDeviceProtocol == null) {
            gbDeviceProtocol = createDeviceProtocol();
        }
        return gbDeviceProtocol;
    }

    public synchronized GBDeviceIoThread getDeviceIOThread() {
        if (gbDeviceIOThread == null) {
            gbDeviceIOThread = createDeviceIOThread();
        }
        return gbDeviceIOThread;
    }

    protected void sendToDevice(byte[] bytes) {
        if (bytes != null && gbDeviceIOThread != null) {
            gbDeviceIOThread.write(bytes);
        }
    }

    public void handleGBDeviceCommand(GBDeviceCommandSendBytes sendBytes) {
        sendToDevice(sendBytes.encodedBytes);
    }

    @Override
    public void evaluateGBDeviceCommand(GBDeviceCommand deviceCmd) {

        switch (deviceCmd.commandClass) {
            case SEND_BYTES:
                handleGBDeviceCommand((GBDeviceCommandSendBytes) deviceCmd);
                return;
            default:
                break;
        }

        super.evaluateGBDeviceCommand(deviceCmd);
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
    public void onSetCallState(String number, String name, GBCommand command) {
        byte[] bytes = gbDeviceProtocol.encodeSetCallState(number, name, command);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        byte[] bytes = gbDeviceProtocol.encodeSetMusicInfo(artist, album, track);
        sendToDevice(bytes);
    }

    @Override
    public void onFirmwareVersionReq() {
        byte[] bytes = gbDeviceProtocol.encodeFirmwareVersionReq();
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
        byte[] bytes = gbDeviceProtocol.encodeLocateDevice(start);
        sendToDevice(bytes);
    }
}
