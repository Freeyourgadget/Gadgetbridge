/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public abstract class Request {
    protected byte[] data;
    private Logger logger = (Logger) LoggerFactory.getLogger(getName());
    //protected ByteBuffer buffer;

    public Request(){
        this.data = getStartSequence();
    }


    public ByteBuffer createBuffer(){
        return createBuffer(getPayloadLength());
    }

    public ByteBuffer createBuffer(int length){
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(getStartSequence());
        return buffer;
    }

    public byte[] getRequestData(){
        return data;
    }

    public UUID getRequestUUID(){
        return UUID.fromString("3dda0002-957f-7d4a-34a6-74696673696d");
    }

    public int getPayloadLength(){ return getStartSequence().length; }

    public abstract byte[] getStartSequence();

    public void handleResponse(BluetoothGattCharacteristic characteristic) {}

    public String getName(){
        Class thisClass = getClass();
        while(thisClass.isAnonymousClass()) thisClass = thisClass.getSuperclass();
        return thisClass.getSimpleName();
    }

    protected void log(String message){
        logger.debug(message);
    }

    public boolean isBasicRequest(){
        return this.getRequestUUID().toString().equals("3dda0002-957f-7d4a-34a6-74696673696d");
    }

    public boolean expectsResponse(){
        return this.data[0] == 1;
    }
}
