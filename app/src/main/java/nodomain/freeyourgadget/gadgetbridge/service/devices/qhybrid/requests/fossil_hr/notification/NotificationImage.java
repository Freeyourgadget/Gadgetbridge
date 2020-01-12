package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFile;

public class NotificationImage extends AssetFile {
    private String packageName;
    private byte[] imageData;

    public NotificationImage(String packageName, byte[] imageData) {
        //TODO this is defo not functional
        super(packageName, imageData);
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
