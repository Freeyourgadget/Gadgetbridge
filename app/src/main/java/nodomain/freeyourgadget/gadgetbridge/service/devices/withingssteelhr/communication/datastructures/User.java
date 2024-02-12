/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class User extends WithingsStructure {

    // This is just a dummy value as this seems to be the withings account id,
    // which we do not need, but the watch expects:
    private int userID = 123456;
    private int weight;
    private int height;
    //Seems to be 0x00 for male and 0x01 for female. Found no other in my tests.
    private byte gender;
    private Date birthdate;
    private String name;

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public Date getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(Date birthdate) {
        this.birthdate = birthdate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public short getLength() {
        return (short) ((name != null ? name.getBytes(StandardCharsets.UTF_8).length : 0) + 22);
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer rawDataBuffer) {
        rawDataBuffer.putInt(userID);
        rawDataBuffer.putInt(weight);
        rawDataBuffer.putInt(height);
        rawDataBuffer.put(gender);
        rawDataBuffer.putInt((int)(birthdate.getTime()/1000));
        addStringAsBytesWithLengthByte(rawDataBuffer, name);
    }

    @Override
    public short getType() {
        return WithingsStructureType.USER;
    }
}
