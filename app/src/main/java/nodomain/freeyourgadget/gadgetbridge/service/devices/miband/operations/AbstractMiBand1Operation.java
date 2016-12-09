package nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;

public abstract class AbstractMiBand1Operation extends AbstractMiBandOperation<MiBandSupport> {
    protected AbstractMiBand1Operation(MiBandSupport support) {
        super(support);
    }

    @Override
    protected void enableOtherNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA), enable);
    }
}
