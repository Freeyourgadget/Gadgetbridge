package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.FindDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.StartPpgSensingCommand;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class StartPpgRequest extends Request {
    public StartPpgRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    int ppgType;

    public int getPpgType() {
        return ppgType;
    }

    public void setPpgType(int ppgType) {
        this.ppgType = ppgType;
    }

    @Override
    public byte[] createRequest() {
        StartPpgSensingCommand cmd = new StartPpgSensingCommand();
        cmd.setPpgType(ppgType);
        return cmd.serialize();
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_PPG_START;
    }

    @Override
    public void handleResponse(byte[] data) {
        StartPpgSensingCommand cmd = new StartPpgSensingCommand();
        cmd.deserialize(data);

        if (!cmd.isSetSuccess() || cmd.getPpgType() != ppgType)
            reportFailure("Could not start PPG sensing");

        operationStatus = OperationStatus.FINISHED;
    }
}
