package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFile;

public class AssetImage extends AssetFile {
    private int angle, distance, indexZ;

    protected AssetImage(byte[] fileData, int angle, int distance, int indexZ) {
        super(fileData);
        this.angle = angle;
        this.distance = distance;
        this.indexZ = indexZ;
    }

    protected AssetImage(byte[] fileData, String fileName, int angle, int distance, int indexZ) {
        super(fileName, fileData);
        this.angle = angle;
        this.distance = distance;
        this.indexZ = indexZ;
    }

    @NonNull
    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    public JSONObject toJsonObject(){
        try {
            return new JSONObject()
                    .put("image_name", getFileName())
                    .put("pos",
                        new JSONObject()
                            .put("angle", angle)
                            .put("distance", distance)
                            .put("z_index", indexZ)
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getIndexZ() {
        return indexZ;
    }

    public void setIndexZ(int indexZ) {
        this.indexZ = indexZ;
    }


}
