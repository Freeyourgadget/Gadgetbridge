package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.device_info;

import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public class DeviceSecurityVersionInfo implements DeviceInfo {
    private int versionMajor, versionMinor;

    @Override
    public void parsePayload(byte[] payload) {
        versionMajor = payload[0];
        versionMinor = payload[1];
    }

    @NonNull
    @Override
    public String toString() {
        return versionMajor + "." + versionMinor;
    }
}
