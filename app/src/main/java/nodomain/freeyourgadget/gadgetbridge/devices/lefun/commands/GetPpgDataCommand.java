package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class GetPpgDataCommand extends BaseCommand {
    private byte ppgType;
    private short totalRecords;
    private short currentRecord;
    private byte year;
    private byte month;
    private byte day;
    private byte hour;
    private byte minute;
    private byte second;
    private byte[] ppgData;

    public int getPpgType() {
        return getLowestSetBitIndex(ppgType);
    }

    public void setPpgType(int type) {
        if (type < 0 || type > 2)
            throw new IllegalArgumentException("Invalid PPG type");
        this.ppgType = (byte)(1 << type);
    }

    public short getTotalRecords() {
        return totalRecords;
    }

    public short getCurrentRecord() {
        return currentRecord;
    }

    public byte getYear() {
        return year;
    }

    public byte getMonth() {
        return month;
    }

    public byte getDay() {
        return day;
    }

    public byte getHour() {
        return hour;
    }

    public byte getMinute() {
        return minute;
    }

    public byte getSecond() {
        return second;
    }

    public byte[] getPpgData() {
        return ppgData;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_PPG_DATA);

        if (params.limit() < 9)
            throwUnexpectedLength();

        ppgType = params.get();
        totalRecords = params.get();
        currentRecord = params.get();
        year = params.get();
        month = params.get();
        day = params.get();
        hour = params.get();
        minute = params.get();
        second = params.get();

        int typeIndex = getPpgType();
        int dataLength;
        switch (typeIndex) {
            case LefunConstants.PPG_TYPE_HEART_RATE:
            case LefunConstants.PPG_TYPE_BLOOD_OXYGEN:
                dataLength = 1;
                break;
            case LefunConstants.PPG_TYPE_BLOOD_PRESSURE:
                dataLength = 2;
                break;
            default:
                throw new IllegalArgumentException("Unknown PPG type");
        }

        if (params.limit() < dataLength + 9)
            throwUnexpectedLength();

        ppgData = new byte[dataLength];
        params.get(ppgData);

        // Extended count/index
        if (params.limit() == dataLength + 11)
        {
            totalRecords |= params.get() << 8;
            currentRecord |= params.get() << 8;
        }
        else if (params.limit() > dataLength + 11) {
            throwUnexpectedLength();
        }
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(ppgType);
        return LefunConstants.CMD_PPG_DATA;
    }
}
