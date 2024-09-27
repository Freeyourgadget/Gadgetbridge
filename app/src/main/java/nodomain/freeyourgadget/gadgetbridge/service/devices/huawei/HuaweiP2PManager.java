package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.P2P;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiBaseP2PService;

public class HuaweiP2PManager {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PManager.class);

    private final HuaweiSupportProvider support;

    private final List<HuaweiBaseP2PService> registeredServices;

    private Short sequence = 1;

    public synchronized Short getNextSequence() {
            return sequence++;
    }

    public HuaweiP2PManager(HuaweiSupportProvider support) {
        this.support = support;
        this.registeredServices = new ArrayList<>();
    }

    public HuaweiSupportProvider getSupportProvider() {
        return support;
    }

    public void registerService(HuaweiBaseP2PService service) {
        for (HuaweiBaseP2PService svr : registeredServices) {
            if (svr.getModule().equals(service.getModule())) {
                LOG.error("P2P Service already registered, unregister: {}", service.getModule());
                svr.unregister();
                registeredServices.remove(svr);
            }
        }
        registeredServices.add(service);
        service.registered();
    }

    public HuaweiBaseP2PService getRegisteredService(String module) {
        for (HuaweiBaseP2PService svr : registeredServices) {
            if (svr.getModule().equals(module)) {
                return svr;
            }
        }
        return null;
    }

    public void unregisterAllService() {
        for (HuaweiBaseP2PService svr : registeredServices) {
            svr.unregister();
        }
        registeredServices.clear();
    }


    public void handlePacket(P2P.P2PCommand.Response packet) {
        LOG.info("P2P Service message: Src: {} Dst: {} Seq: {}", packet.srcPackage, packet.dstPackage, packet.sequenceId);
        for (HuaweiBaseP2PService service : registeredServices) {
            if (service.getPackage().equals(packet.srcPackage)) {
                service.handlePacket(packet);
            }
        }
    }
}
