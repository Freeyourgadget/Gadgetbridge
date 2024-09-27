package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.AccountRelated;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.P2P;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendP2PCommand extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendAccountRequest.class);

    private byte cmdId;
    private short sequenceId;
    private String srcPackage;
    private String dstPackage;
    private String srcFingerprint = null;
    private String dstFingerprint = null;
    private byte[] sendData = null;
    private int sendCode = 0;

    public SendP2PCommand(HuaweiSupportProvider support,
                          byte cmdId,
                          short sequenceId,
                          String srcPackage,
                          String dstPackage,
                          String srcFingerprint,
                          String dstFingerprint,
                          byte[] sendData,
                          int sendCode) {
        super(support);
        this.serviceId = P2P.id;
        this.commandId = P2P.P2PCommand.id;

        this.cmdId = cmdId;
        this.sequenceId = sequenceId;
        this.srcPackage = srcPackage;
        this.dstPackage = dstPackage;
        this.srcFingerprint = srcFingerprint;
        this.dstFingerprint = dstFingerprint;
        this.sendData = sendData;
        this.sendCode = sendCode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new P2P.P2PCommand.Request(paramsProvider, this.cmdId, this.sequenceId, this.srcPackage, this.dstPackage, this.srcFingerprint, this.dstFingerprint, this.sendData, this.sendCode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}