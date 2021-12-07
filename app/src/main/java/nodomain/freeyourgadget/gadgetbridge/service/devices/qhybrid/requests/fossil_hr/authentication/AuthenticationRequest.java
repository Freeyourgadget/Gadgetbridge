package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;

public abstract class AuthenticationRequest extends FossilRequest {
    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0005-957f-7d4a-34a6-74696673696d");
    }
}
