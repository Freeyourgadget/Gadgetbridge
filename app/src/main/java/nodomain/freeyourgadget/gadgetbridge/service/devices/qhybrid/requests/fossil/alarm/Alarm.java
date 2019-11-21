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
    private byte minute, hour;
    private boolean repeat;

    public Alarm(byte minute, byte hour){
        this.minute = minute;
        this.hour = hour;
        this.repeat = false;
    }

    public Alarm(byte minute, byte hour, boolean repeat){
        this.minute = minute;
        this.hour = hour;
        this.repeat = repeat;
    }

    public Alarm(byte minute, byte hour, byte days){
        this.minute = minute;
        this.hour = hour;
        this.repeat = true;
        this.days = days;
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
            return new Alarm(minutes, bytes[2], days);
        }
        return new Alarm(minutes, bytes[2]);
    }

    @NonNull
    @Override
    public String toString() {
        String description = this.hour + ":" + this.minute + "  ";
        if(repeat){
            String[] dayNames = new String[]{"sunday", "monday", "tuesday", "thursday", "wednesday", "friday", "saturday"};
            for(int i = WEEKDAY_SUNDAY; i <= WEEKDAY_SATURDAY; i++){
                if((days & 1 << i) != 0){
                    description += dayNames[i] + " ";
                }
            }
        }else{
            description += "not repeating";
        }

        return description;
    }
}
