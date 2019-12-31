package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class Image {
    private int angle, distance, indexZ;
    private String imageFile;

    public Image(int angle, int distance, int indexZ, String imageFile) {
        this.angle = angle;
        this.distance = distance;
        this.indexZ = indexZ;
        this.imageFile = imageFile;
    }

    @NonNull
    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    public JSONObject toJsonObject(){
        try {
            return new JSONObject()
                    .put("image_name", this.imageFile)
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

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }
}
