package nodomain.freeyourgadget.gadgetbridge;

import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public abstract class AbstractBTDeviceSupport extends AbstractDeviceSupport {

    private GBDeviceProtocol gbDeviceProtocol;
    private GBDeviceIoThread gbDeviceIOThread;

    protected abstract GBDeviceProtocol createDeviceProtocol();

    protected abstract GBDeviceIoThread createDeviceIOThread();
    
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
        if (bytes != null) {
            gbDeviceIOThread.write(bytes);
        }
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
    public void onAppInfoReq() {
        byte[] bytes = gbDeviceProtocol.encodeAppInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppDelete(int id, int index) {
        byte[] bytes = gbDeviceProtocol.encodeAppDelete(id, index);
        sendToDevice(bytes);
    }

    @Override
    public void onPhoneVersion(byte os) {
        byte[] bytes = gbDeviceProtocol.encodePhoneVersion(os);
        sendToDevice(bytes);
    }
}
