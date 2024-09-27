package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.P2P;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendP2PCommand;

public abstract class HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiBaseP2PService.class);

    protected final HuaweiP2PManager manager;

    protected HuaweiBaseP2PService(HuaweiP2PManager manager) {
        this.manager = manager;
    }

    public void register() {
        manager.registerService(this);
    }

    public abstract String getModule();

    public abstract String getPackage();

    public abstract String getFingerprint();

    public abstract void registered();

    public abstract void unregister();

    public abstract void handleData(byte[] data);

    public String getLocalFingerprint() {
        return "UniteDeviceManagement";
    }

    public String getPingPackage() {
        return "com.huawei.health";
    }

    private final Map<Short, HuaweiP2PCallback> waitPackets = new ConcurrentHashMap<>();

    private Short getNextSequence() {
        return manager.getNextSequence();
    }

    public void sendCommand(byte[] sendData, HuaweiP2PCallback callback) {
        try {
            short seq = this.getNextSequence();
            SendP2PCommand test = new SendP2PCommand(this.manager.getSupportProvider(), (byte) 2, seq, this.getModule(), this.getPackage(), this.getLocalFingerprint(), this.getFingerprint(), sendData, 0);
            if (callback != null) {
                this.waitPackets.put(seq, callback);
            }
            test.doPerform();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendPing(HuaweiP2PCallback callback) {
        try {
            short seq = this.getNextSequence();
            SendP2PCommand test = new SendP2PCommand(this.manager.getSupportProvider(), (byte) 1, seq, this.getPingPackage(), this.getPackage(), null, null, null, 0);
            if (callback != null) {
                this.waitPackets.put(seq, callback);
            }
            test.doPerform();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendAck(short sequence, String srcPackage, String dstPackage, int code) {
        try {
            SendP2PCommand test = new SendP2PCommand(this.manager.getSupportProvider(), (byte) 3, sequence, srcPackage, dstPackage, null, null, null, code);
            test.doPerform();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePacket(P2P.P2PCommand.Response packet) {
        LOG.info("HuaweiP2PCalendarService handlePacket: {} Code: {}", packet.cmdId, packet.respCode);
        if (waitPackets.containsKey(packet.sequenceId)) {
            LOG.info("HuaweiP2PCalendarService handlePacket find handler");
            HuaweiP2PCallback handle = waitPackets.remove(packet.sequenceId);
            handle.onResponse(packet.respCode, packet.respData);
        } else {

            if (packet.cmdId == 1) { //Ping
                sendAck(packet.sequenceId, packet.dstPackage, packet.srcPackage, 0xca);
            } else if (packet.cmdId == 2) {
                handleData(packet.respData);
                sendAck(packet.sequenceId, packet.dstPackage, packet.srcPackage, 0xca);
            }
        }
    }

}
