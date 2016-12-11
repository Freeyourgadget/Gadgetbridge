package nodomain.freeyourgadget.gadgetbridge.devices.miband2;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.Mi2FirmwareInfo;

public class MiBand2FWHelper extends AbstractMiBandFWHelper {
    private Mi2FirmwareInfo firmwareInfo;

    public MiBand2FWHelper(Uri uri, Context context) throws IOException {
        super(uri, context);
    }

    @Override
    public String format(int version) {
        return Mi2FirmwareInfo.toVersion(version);
    }

    @Override
    public int getFirmwareVersion() {
        return firmwareInfo.getFirmwareVersion();
    }

    @Override
    public int getFirmware2Version() {
        return 0;
    }

    @Override
    public String getHumanFirmwareVersion2() {
        return "";
    }

    @Override
    protected int[] getWhitelistedFirmwareVersions() {
        return Mi2FirmwareInfo.getWhitelistedVersions();
    }

    @Override
    public boolean isFirmwareGenerallyCompatibleWith(GBDevice device) {
        return firmwareInfo.isGenerallyCompatibleWith(device);
    }

    @Override
    public boolean isSingleFirmware() {
        return true;
    }

    @NonNull
    @Override
    protected void determineFirmwareInfo(byte[] wholeFirmwareBytes) {
        firmwareInfo = new Mi2FirmwareInfo(wholeFirmwareBytes);
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        firmwareInfo.checkValid();
    }

    public Mi2FirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }

}
