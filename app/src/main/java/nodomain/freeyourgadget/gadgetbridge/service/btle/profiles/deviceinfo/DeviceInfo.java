/*  Copyright (C) 2016-2018 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo;

import android.os.Parcel;
import android.os.Parcelable;

public class DeviceInfo implements Parcelable{
    private String manufacturerName;
    private String modelNumber;
    private String serialNumber;
    private String hardwareRevision;
    private String firmwareRevision;
    private String softwareRevision;
    private String systemId;
    private String regulatoryCertificationDataList;
    private String pnpId;

    public DeviceInfo() {
    }

    protected DeviceInfo(Parcel in) {
        manufacturerName = in.readString();
        modelNumber = in.readString();
        serialNumber = in.readString();
        hardwareRevision = in.readString();
        firmwareRevision = in.readString();
        softwareRevision = in.readString();
        systemId = in.readString();
        regulatoryCertificationDataList = in.readString();
        pnpId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(manufacturerName);
        dest.writeString(modelNumber);
        dest.writeString(serialNumber);
        dest.writeString(hardwareRevision);
        dest.writeString(firmwareRevision);
        dest.writeString(softwareRevision);
        dest.writeString(systemId);
        dest.writeString(regulatoryCertificationDataList);
        dest.writeString(pnpId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
        @Override
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in);
        }

        @Override
        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getHardwareRevision() {
        return hardwareRevision;
    }

    public void setHardwareRevision(String hardwareRevision) {
        this.hardwareRevision = hardwareRevision;
    }

    public String getFirmwareRevision() {
        return firmwareRevision;
    }

    public void setFirmwareRevision(String firmwareRevision) {
        this.firmwareRevision = firmwareRevision;
    }

    public String getSoftwareRevision() {
        return softwareRevision;
    }

    public void setSoftwareRevision(String softwareRevision) {
        this.softwareRevision = softwareRevision;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getRegulatoryCertificationDataList() {
        return regulatoryCertificationDataList;
    }

    public void setRegulatoryCertificationDataList(String regulatoryCertificationDataList) {
        this.regulatoryCertificationDataList = regulatoryCertificationDataList;
    }

    public String getPnpId() {
        return pnpId;
    }

    public void setPnpId(String pnpId) {
        this.pnpId = pnpId;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "manufacturerName='" + manufacturerName + '\'' +
                ", modelNumber='" + modelNumber + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", hardwareRevision='" + hardwareRevision + '\'' +
                ", firmwareRevision='" + firmwareRevision + '\'' +
                ", softwareRevision='" + softwareRevision + '\'' +
                ", systemId='" + systemId + '\'' +
                ", regulatoryCertificationDataList='" + regulatoryCertificationDataList + '\'' +
                ", pnpId='" + pnpId + '\'' +
                '}';
    }
}
