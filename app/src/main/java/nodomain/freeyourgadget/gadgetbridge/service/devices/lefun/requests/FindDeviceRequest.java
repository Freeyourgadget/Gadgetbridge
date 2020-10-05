package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.FindDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class FindDeviceRequest extends Request {
    public FindDeviceRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    @Override
    public byte[] createRequest() {
        FindDeviceCommand cmd = new FindDeviceCommand();
        return cmd.serialize();
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_FIND_DEVICE;
    }

    @Override
    public void handleResponse(byte[] data) {
        FindDeviceCommand cmd = new FindDeviceCommand();
        cmd.deserialize(data);

        if (!cmd.isSuccess())
            reportFailure("Could not initiate find device");

        operationStatus = OperationStatus.FINISHED;
    }
}
