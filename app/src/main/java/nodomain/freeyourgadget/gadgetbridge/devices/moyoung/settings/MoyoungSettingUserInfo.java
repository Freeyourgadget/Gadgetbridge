/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings;

import org.apache.commons.lang3.NotImplementedException;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public class MoyoungSettingUserInfo extends MoyoungSetting<ActivityUser> {
    public MoyoungSettingUserInfo(String name, byte cmdSet) {
        super(name, (byte)-1, cmdSet);
    }

    @Override
    public byte[] encode(ActivityUser value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put((byte)value.getHeightCm());
        buffer.put((byte)value.getWeightKg());
        buffer.put((byte)value.getAge());
        buffer.put((byte)value.getGender());
        return buffer.array();
    }

    @Override
    public ActivityUser decode(byte[] data) {
        throw new NotImplementedException("decode");
    }
}
