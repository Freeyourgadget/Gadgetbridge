/*  Copyright (C) 2020-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file;

public enum FileHandle {
    OTA_FILE(0x00, 0x00),
    ACTIVITY_FILE(0x01, 0x00),
    HARDWARE_LOG_FILE(0x02, 0x00),
    FONT_FILE(0x03, 0x00),
    MUSIC_INFO(0x04, 0x00),
    UI_CONTROL(0x05, 0x00),
    HAND_ACTIONS(0x06, 0x00),
    SETTINGS_BUTTONS(0x06, 0x00),
    ASSET_BACKGROUND_IMAGES(0x07, 0x00),
    ASSET_NOTIFICATION_IMAGES(0x07, 0x01),
    ASSET_TRANSLATIONS(0x07, 0x02),
    ASSET_REPLY_IMAGES(0x07, 0x03),
    CONFIGURATION(0x08, 0x00),
    NOTIFICATION_PLAY(0x09, 0x00),
    ALARMS(0x0A, 0x00),
    DEVICE_INFO(0x0b, 0x00),
    NOTIFICATION_FILTER(0x0C, 0x00),
    WATCH_PARAMETERS(0x0E, 0x00),
    LOOK_UP_TABLE(0x0f, 0x00),
    RATE(0x10, 0x00),
    REPLY_MESSAGES(0x13, 0x00),
    APP_CODE(0x15, 0xFE),
    ;

    private int handle, subHandle;

    FileHandle(int handle, int subHandle) {
        this.handle = handle;
        this.subHandle = subHandle;
    }

    public static FileHandle fromName(String name){
        for(FileHandle handle : FileHandle.values()){
            if(handle.toString().equals(name)){
                return handle;
            }
        }
        return null;
    }

    public static FileHandle fromHandle(short handleBytes){
        for(FileHandle handle : FileHandle.values()){
            if(handle.getHandle() == handleBytes){
                return handle;
            }
        }
        return null;
    }

    public short getHandle(){
        return (short)((handle << 8) | (subHandle));
    }

    public byte getMinorHandle(){
        return (byte) subHandle;
    }

    public byte getMajorHandle() {
        return (byte) handle;
    }
}
