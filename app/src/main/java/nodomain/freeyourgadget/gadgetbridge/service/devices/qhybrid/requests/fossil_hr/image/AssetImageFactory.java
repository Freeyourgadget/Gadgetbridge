package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import android.graphics.Bitmap;

public class AssetImageFactory {
    public static AssetImage createAssetImage(String fileName, byte[] fileData, int angle, int distance, int indexZ){
        return new AssetImage(fileName, fileData, angle, distance, indexZ);
    }

    public static AssetImage createAssetImage(String fileName, Bitmap fileData, boolean RLEencode, int angle, int distance, int indexZ){
        return null;
    }
}
