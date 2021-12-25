/*  Copyright (C) 2021 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime;

import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;
import java.util.List;

public class InfiniTimeDFUPackage {
    @SerializedName("manifest")
    InfiniTimeDFUPackageManifest manifest;
}

class InfiniTimeDFUPackageManifest {
    @SerializedName("application")
    InfiniTimeDFUPackageApplication application;
    @SerializedName("dfu_version")
    Float dfu_version;
}

class InfiniTimeDFUPackageApplication {
    @SerializedName("bin_file")
    String bin_file;
    @SerializedName("dat_file")
    String dat_file;
    @SerializedName("init_packet_data")
    InfiniTimeDFUPackagePacketData init_packet_data;
}

class InfiniTimeDFUPackagePacketData {
    @SerializedName("application_version")
    BigInteger application_version;
    @SerializedName("device_revision")
    BigInteger device_revision;
    @SerializedName("device_type")
    BigInteger device_type;
    @SerializedName("firmware_crc16")
    BigInteger firmware_crc16;
    @SerializedName("softdevice_req")
    List<Integer> softdevice_req;
}
