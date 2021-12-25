/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class ProfileCommand extends BaseCommand {
    public static final byte GENDER_FEMALE = 0;
    public static final byte GENDER_MALE = 1;

    private byte op;
    private byte gender;
    private byte height; // cm
    private byte weight; // kg
    private byte age; // years

    private boolean setSuccess;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        if (op != OP_GET && op != OP_SET)
            throw new IllegalArgumentException("Operation must be get or set");
        this.op = op;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        if (gender != GENDER_FEMALE && gender != GENDER_MALE)
            throw new IllegalArgumentException("Invalid gender");
        this.gender = gender;
    }

    public byte getHeight() {
        return height;
    }

    public void setHeight(byte height) {
        int intHeight = (int)height & 0xff;
        if (intHeight < 40 || intHeight > 210)
            throw new IllegalArgumentException("Height must be between 40 and 210 cm inclusive");
        this.height = height;
    }

    public byte getWeight() {
        return weight;
    }

    public void setWeight(byte weight) {
        int intWeight = (int)weight & 0xff;
        if (intWeight < 5 || intWeight > 200)
            throw new IllegalArgumentException("Weight must be between 5 and 200 kg inclusive");
        this.weight = weight;
    }

    public byte getAge() {
        return age;
    }

    public void setAge(byte age) {
        if (age < 0 || age > 110)
            throw new IllegalArgumentException("Age must be between 0 and 110 years inclusive");
        this.age = age;
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_PROFILE);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 1)
            throwUnexpectedLength();

        op = params.get();
        if (op == OP_GET) {
            if (paramsLength != 5)
                throwUnexpectedLength();

            gender = params.get();
            height = params.get();
            weight = params.get();
            age = params.get();
        } else if (op == OP_SET) {
            if (paramsLength != 2)
                throwUnexpectedLength();

            setSuccess = params.get() == 1;
        } else {
            throw new IllegalArgumentException("Invalid operation type received");
        }
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(op);
        if (op == OP_SET) {
            params.put(gender);
            params.put(height);
            params.put(weight);
            params.put(age);
        }
        return LefunConstants.CMD_PROFILE;
    }
}
