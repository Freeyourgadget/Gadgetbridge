/*  Copyright (C) 2021-2024 Arjan Schrijver, Daniel Dakhno

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application;

public class ApplicationInformation implements Comparable<ApplicationInformation> {
    String appName, version;
    int hash;
    byte fileHandle;

    public ApplicationInformation(String appName, String version, int hash, byte fileHandle) {
        this.appName = appName;
        this.version = version;
        this.hash = hash;
        this.fileHandle = fileHandle;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return version;
    }

    public byte getFileHandle() {
        return fileHandle;
    }

    @Override
    public int compareTo(ApplicationInformation o) {
        return this.appName.toLowerCase().compareTo(o.getAppName().toLowerCase());
    }
}
