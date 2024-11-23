package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Earphones;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetPauseWhenRemovedFromEarRequest extends Request {

    public SetPauseWhenRemovedFromEarRequest(HuaweiSupportProvider supportProvider) {
        super(supportProvider);
        this.serviceId = Earphones.id;
        this.commandId = Earphones.SetAudioModeRequest.id;
        this.addToResponse = false; // Response with different command ID
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            boolean newState = GBApplication
                    .getDeviceSpecificSharedPrefs(this.getDevice().getAddress())
                    .getBoolean(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_INEAR, false);
            return new Earphones.SetPauseWhenRemovedFromEar(this.paramsProvider, newState).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
