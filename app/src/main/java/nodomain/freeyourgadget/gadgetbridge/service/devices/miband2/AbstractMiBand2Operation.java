package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.AbstractMiBandOperation;

public abstract class AbstractMiBand2Operation extends AbstractMiBandOperation<MiBand2Support> {
    protected AbstractMiBand2Operation(MiBand2Support support) {
        super(support);
    }

    @Override
    protected void enableOtherNotifications(TransactionBuilder builder, boolean enable) {
        // TODO: check which notifications we should disable and re-enable here
//        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS), enable)
//                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA), enable);
    }
}
