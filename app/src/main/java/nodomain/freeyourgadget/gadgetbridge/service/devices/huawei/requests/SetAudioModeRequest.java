package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Earphones;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetAudioModeRequest extends Request {

    public SetAudioModeRequest(HuaweiSupportProvider supportProvider) {
        super(supportProvider);
        this.serviceId = Earphones.id;
        this.commandId = Earphones.SetAudioModeRequest.id;
        this.addToResponse = false; // Response with different command ID
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            String audioMode = GBApplication
                    .getDeviceSpecificSharedPrefs(this.getDevice().getAddress())
                    .getString(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_AUDIOMODE, "off");
            byte mode = 0; // Off by default
            switch (audioMode) {
                case "anc":
                    mode = 1;
                    break;
                case "transparency":
                    mode = 2;
                    break;
                default:
            }
            return new Earphones.SetAudioModeRequest(this.paramsProvider, mode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
