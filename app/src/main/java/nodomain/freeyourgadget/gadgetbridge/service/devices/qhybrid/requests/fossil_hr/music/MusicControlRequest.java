/*  Copyright (C) 2019-2021 Andreas Shimokawa, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;

public class MusicControlRequest extends FossilRequest {
    private MUSIC_PHONE_REQUEST request;

    public MusicControlRequest(MUSIC_PHONE_REQUEST request) {
        this.request = request;

        this.data = new byte[]{
                (byte) 0x02,
                (byte) 0x05,
                this.request.getCommandByte(),
                (byte) 0x00
        };
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public byte[] getStartSequence() {
        return null;
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0006-957f-7d4a-34a6-74696673696d");
    }

    public enum MUSIC_WATCH_REQUEST {
        MUSIC_REQUEST_PLAY_PAUSE((byte) 0x02),
        MUSIC_REQUEST_NEXT((byte) 0x03),
        MUSIC_REQUEST_PREVIOUS((byte) 0x04),
        MUSIC_REQUEST_LOUDER((byte) 0x05),
        MUSIC_REQUEST_QUITER((byte) 0x06),
        ;
        private byte commandByte;

        MUSIC_WATCH_REQUEST(byte commandByte) {
            this.commandByte = commandByte;
        }

        public static MUSIC_WATCH_REQUEST fromCommandByte(byte commandByte){
            for(MUSIC_WATCH_REQUEST request : MUSIC_WATCH_REQUEST.values()){
                if(request.commandByte == commandByte) return request;
            }
            return null;
        }
    }

    public enum MUSIC_PHONE_REQUEST {
        MUSIC_REQUEST_SET_PLAYING((byte) 0x00),
        MUSIC_REQUEST_SET_PAUSED((byte) 0x01),
        MUSIC_REQUEST_PLAY_PAUSE((byte) 0x02),
        MUSIC_REQUEST_NEXT((byte) 0x03),
        MUSIC_REQUEST_PREVIOUS((byte) 0x04),
        MUSIC_REQUEST_LOUDER((byte) 0x05),
        MUSIC_REQUEST_QUITER((byte) 0x06),
        ;
        private byte commandByte;

        public byte getCommandByte() {
            return commandByte;
        }

        private MUSIC_PHONE_REQUEST(byte commandByte) {
            this.commandByte = commandByte;
        }
    }
}
