package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

public class NotificationImage {
    private String packageName;
    private byte[] imageData;

    public NotificationImage(String packageName, byte[] imageData) {
        this.packageName = packageName;
        this.imageData = imageData;
    }

    public String getPackageName() {
        return packageName;
    }

    public byte[] getImageData() {
        return imageData;
    }
}
