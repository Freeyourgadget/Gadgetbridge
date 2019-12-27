package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import android.os.Build;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.VerifyPrivateKeyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.information.GetDeviceInformationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationFilterPutHRRequest;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    private byte[] secretKey = new byte[]{(byte) 0x60, (byte) 0x26, (byte) 0xB7, (byte) 0xFD, (byte) 0xB2, (byte) 0x6D, (byte) 0x05, (byte) 0x5E, (byte) 0xDA, (byte) 0xF7, (byte) 0x4B, (byte) 0x49, (byte) 0x98, (byte) 0x78, (byte) 0x02, (byte) 0x38};
    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    @Override
    public void initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512));
        }

        queueWrite(new VerifyPrivateKeyRequest(
                this.getSecretKey(),
                this
        ));

        /*try {
            FileInputStream fis = new FileInputStream("/sdcard/Q/images/icWhatsapp.icon");
            byte[] whatsappData = new byte[fis.available()];
            fis.read(whatsappData);
            fis.close();

            fis = new FileInputStream("/sdcard/Q/images/icTwitter.icon");
            byte[] twitterData = new byte[fis.available()];
            fis.read(twitterData);
            fis.close();

            queueWrite(new NotificationImagePutRequest(
                    new String[]{
                            "twitter",
                            "com.whatsapp",
                    },
                    new byte[][]{
                            twitterData,
                            whatsappData,
                    },
                    this));
        } catch (IOException e) {
            e.printStackTrace();
        }*/ // icons

        queueWrite(new NotificationFilterPutHRRequest(new NotificationHRConfiguration[]{
                new NotificationHRConfiguration("com.whatsapp", -1),
                new NotificationHRConfiguration("generic", -1),
                // new NotificationHRConfiguration("twitter", -1),
        }, this));

        queueWrite(new PlayNotificationRequest("com.whatsapp", "WhatsAp", "wHATSaPP", this));
        queueWrite(new PlayNotificationRequest("twitter", "Twitter", "tWITTER", this));

        queueWrite(new GetDeviceInformationRequest(this));

        // syncConfiguration();

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    @Override
    public void setActivityHand(double progress) {
        // super.setActivityHand(progress);
    }

    public boolean playRawNotification(NotificationSpec notificationSpec) {
        String sender = notificationSpec.sender;
        if(sender == null) sender = notificationSpec.sourceName;
        queueWrite(new PlayNotificationRequest("generic", notificationSpec.sourceName, notificationSpec.body, this));
        return true;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = secretKey;
    }

    public void setPhoneRandomNumber(byte[] phoneRandomNumber) {
        this.phoneRandomNumber = phoneRandomNumber;
    }

    public byte[] getPhoneRandomNumber() {
        return phoneRandomNumber;
    }

    public void setWatchRandomNumber(byte[] watchRandomNumber) {
        this.watchRandomNumber = watchRandomNumber;
    }

    public byte[] getWatchRandomNumber() {
        return watchRandomNumber;
    }
}
