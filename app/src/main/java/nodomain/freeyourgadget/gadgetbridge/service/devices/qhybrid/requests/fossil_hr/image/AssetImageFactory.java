/*  Copyright (C) 2020-2021 Daniel Dakhno

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

import android.graphics.Bitmap;
import java.io.IOException;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter.encodeToRLEImage;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter.encodeToRawImage;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter.get2BitsRAWImageBytes;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter.get2BitsRLEImageBytes;

public class AssetImageFactory {
    public static AssetImage createAssetImage(byte[] fileData, int angle, int distance, int indexZ){
        return new AssetImage(fileData, angle, distance, indexZ);
    }

    public static AssetImage createAssetImage(Bitmap fileData, boolean RLEencode, int angle, int distance, int indexZ) throws IOException {
        if(RLEencode == (distance == 0)) throw new RuntimeException("when RLEencoding distance must be 0, image must be at center of screen");
        if(RLEencode){
            return new AssetImage(encodeToRLEImage(get2BitsRLEImageBytes(fileData), fileData.getHeight(), fileData.getWidth()), angle, distance, indexZ);
        }else{
            return new AssetImage(encodeToRawImage(get2BitsRAWImageBytes(fileData)), angle, distance, indexZ);
        }
    }
}
