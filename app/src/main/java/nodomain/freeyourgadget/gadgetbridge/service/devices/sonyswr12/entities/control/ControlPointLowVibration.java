/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public final class ControlPointLowVibration extends ControlPoint {
    public boolean smartWakeUp;
    public boolean incomingCall;
    public boolean notification;

    public ControlPointLowVibration(boolean isEnabled){
        super(CommandCode.LOW_VIBRATION);
        this.smartWakeUp = isEnabled;
        this.incomingCall = isEnabled;
        this.notification = isEnabled;
    }

    public final byte[] toByteArray() {
        final UIntBitWriter uIntBitWriter = new UIntBitWriter(16);
        uIntBitWriter.append(13, 0);
        uIntBitWriter.appendBoolean(this.smartWakeUp);
        uIntBitWriter.appendBoolean(this.incomingCall);
        uIntBitWriter.appendBoolean(this.notification);
        final ByteArrayWriter byteArrayWriter = this.getValueWriter();
        byteArrayWriter.appendUint16((int) uIntBitWriter.getValue());
        return byteArrayWriter.getByteArray();
    }
}
