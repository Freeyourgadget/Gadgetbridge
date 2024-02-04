package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.GpsAndTime;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SendGpsAndTimeToDeviceRequest extends Request {

    public SendGpsAndTimeToDeviceRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = GpsAndTime.id;
        this.commandId = GpsAndTime.CurrentGPSRequest.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // TODO: support multiple units

            Prefs prefs = GBApplication.getPrefs();
            // Backdating a bit seems to work better
            return new GpsAndTime.CurrentGPSRequest(
                    this.paramsProvider,
                    (int) (Calendar.getInstance().getTime().getTime() / 1000L) - 60,
                    prefs.getFloat("location_latitude", 0),
                    prefs.getFloat("location_longitude", 0)
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
