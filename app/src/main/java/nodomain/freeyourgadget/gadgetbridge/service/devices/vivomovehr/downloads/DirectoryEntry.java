/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads;

import java.util.Date;

public class DirectoryEntry {
    public final int fileIndex;
    public final int fileDataType;
    public final int fileSubType;
    public final int fileNumber;
    public final int specificFlags;
    public final int fileFlags;
    public final int fileSize;
    public final Date fileDate;

    public DirectoryEntry(int fileIndex, int fileDataType, int fileSubType, int fileNumber, int specificFlags, int fileFlags, int fileSize, Date fileDate) {
        this.fileIndex = fileIndex;
        this.fileDataType = fileDataType;
        this.fileSubType = fileSubType;
        this.fileNumber = fileNumber;
        this.specificFlags = specificFlags;
        this.fileFlags = fileFlags;
        this.fileSize = fileSize;
        this.fileDate = fileDate;
    }
}
