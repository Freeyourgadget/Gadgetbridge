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
