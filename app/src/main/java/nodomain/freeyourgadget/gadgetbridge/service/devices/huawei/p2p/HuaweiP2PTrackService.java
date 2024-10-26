package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiSportHRZones;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;

public class HuaweiP2PTrackService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PTrackService.class);

    public static final String MODULE = "hw.unitedevice.track";

    private static final int HEADER_LENGTH = 36;

    static class Sequence {
        int counter;

        private Sequence() {
            this.counter = 0;
        }

        public int getNext() {
            synchronized (this) {
                this.counter = (this.counter + 1) % 10000;
                return this.counter;
            }
        }
    }

    private final Sequence counter = new Sequence();

    public HuaweiP2PTrackService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PTrackService");
    }

    @Override
    public String getModule() {
        return HuaweiP2PTrackService.MODULE;
    }

    @Override
    public String getPackage() {
        return "hw.watch.health.p2p";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    public void sendHeartZoneConfig() {

        ActivityUser activityUser = new ActivityUser();

        HuaweiSportHRZones hrZones = new HuaweiSportHRZones(activityUser.getAge());

        byte[] data = hrZones.getHRZonesData();
        if (data == null) {
            LOG.error("Incorrect Heart Rate config");
            return;
        }

        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(2); // session id ??
        header.putInt(1); // version
        header.putInt(HEADER_LENGTH + data.length); // total length
        header.putInt(0); // unknown, sub header length??
        header.putInt(counter.getNext()); // message id
        header.flip();

        ByteBuffer packet = ByteBuffer.allocate(36 + data.length);
        packet.put(header.array());
        packet.put(data);
        packet.flip();

        LOG.info("HuaweiP2PTrackService sendHeartZoneConfig");

        sendCommand(packet.array(), null);
    }

    @Override
    public void registered() {
        sendHeartZoneConfig();
    }

    @Override
    public void unregister() {

    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("HuaweiP2PTrackService handleData: {}", data.length);
    }

    public static HuaweiP2PTrackService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PTrackService) manager.getRegisteredService(HuaweiP2PTrackService.MODULE);
    }
}
