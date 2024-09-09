/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.backup;

import java.util.Date;

public class ZipBackupMetadata {
    private final String appId;
    private final String appVersionName;
    private final int appVersionCode;

    private final int backupVersion;
    private final Date backupDate;

    public ZipBackupMetadata(final String appId,
                             final String appVersionName,
                             final int appVersionCode,
                             final int backupVersion,
                             final Date backupDate) {
        this.appId = appId;
        this.appVersionName = appVersionName;
        this.appVersionCode = appVersionCode;
        this.backupVersion = backupVersion;
        this.backupDate = backupDate;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppVersionName() {
        return appVersionName;
    }

    public int getAppVersionCode() {
        return appVersionCode;
    }

    public int getBackupVersion() {
        return backupVersion;
    }

    public Date getBackupDate() {
        return backupDate;
    }
}
