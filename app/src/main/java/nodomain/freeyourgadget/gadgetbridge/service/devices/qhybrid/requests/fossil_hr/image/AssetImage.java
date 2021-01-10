/*  Copyright (C) 2019-2021 Daniel Dakhno

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
