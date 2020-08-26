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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm;

import androidx.annotation.NonNull;

public class Alarm {
    public final int WEEKDAY_SUNDAY = 0;
    public final int WEEKDAY_MONDAY = 1;
    public final int WEEKDAY_TUESDAY = 2;
    public final int WEEKDAY_THURSDAY = 3;
    public final int WEEKDAY_WEDNESDAY = 4;
    public final int WEEKDAY_FRIDAY = 5;
    public final int WEEKDAY_SATURDAY = 6;
    private byte days = 0;
    private final byte minute;
    private final byte hour;
    private final boolean repeat;
    private String title, message;

    public Alarm(byte minute, byte hour, String title, String message) {
        this.minute = minute;
        this.hour = hour;
        this.repeat = false;

        this.title = title;
        this.message = message;
    }

    public Alarm(byte minute, byte hour, byte days, String title, String message){
        this.minute = minute;
        this.hour = hour;
        this.repeat = true;
        this.days = days;

        this.title = title;
        this.message = message;
    }

    public void setDayEnabled(int day, boolean enabled){
        if(enabled) this.days |= 1 << day;
        else this.days &= ~(1 << day);
    }

    public byte[] getData(){
        byte first = (byte) 0xFF;
        if(repeat){
            first = (byte) (0x80 | this.days);
        }

        byte second = (byte) this.minute;

        if(repeat) second |= 0x80;

        byte third = this.hour;

        return new byte[]{first, second, third};
    }

    static public Alarm fromBytes(byte[] bytes){
        if(bytes.length != 3) throw new RuntimeException("alarm bytes length must be 3");

        byte days = bytes[0];

        byte minutes = (byte)(bytes[1] & 0b01111111);
        boolean repeat = (bytes[1] & 0x80) == 0x80;

        if(repeat) {
            return new Alarm(minutes, bytes[2], days, "some title", "i dunno this should't happen");
        }
        return new Alarm(minutes, bytes[2], "some title", "i dunno this should't happen");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder description = new StringBuilder(this.hour + ":" + this.minute + "  ");
        if(repeat){
            String[] dayNames = new String[]{"sunday", "monday", "tuesday", "thursday", "wednesday", "friday", "saturday"};
            for(int i = WEEKDAY_SUNDAY; i <= WEEKDAY_SATURDAY; i++){
                if((days & 1 << i) != 0){
                    description.append(dayNames[i]).append(" ");
                }
            }
        }else{
            description.append("not repeating");
        }

        return description.toString();
    }
}
