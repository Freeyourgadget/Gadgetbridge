package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class Mi1FirmwareInfo extends AbstractMi1FirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Mi1FirmwareInfo.class);

    public static Mi1FirmwareInfo getInstance(byte[] wholeFirmwareBytes) {
        Mi1FirmwareInfo info = new Mi1FirmwareInfo(wholeFirmwareBytes);
        if (info.isGenerallySupportedFirmware()) {
            return info;
        }
        LOG.info("firmware not supported");
        return null;
    }

    protected Mi1FirmwareInfo(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    protected int getSupportedMajorVersion() {
        return 1;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        String hwVersion = device.getHardwareVersion();
        return MiBandConst.MI_1.equals(hwVersion);
    }
}
