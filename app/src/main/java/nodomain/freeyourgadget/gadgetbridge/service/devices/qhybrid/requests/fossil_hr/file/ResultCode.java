package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public enum ResultCode {
    SUCCESS(0, true),
    INVALID_OPERATION_DATA(1, false),
    OPERATION_IN_PROGRESS(2, false),
    MISS_PACKET(3, false),
    SOCKET_BUSY(4, false),
    VERIFICATION_FAIL(5, false),
    OVERFLOW(6, false),
    SIZE_OVER_LIMIT(7, false),
    FIRMWARE_INTERNAL_ERROR(128, false),
    FIRMWARE_INTERNAL_ERROR_NOT_OPEN(129, false),
    FIRMWARE_INTERNAL_ERROR_ACCESS_ERROR(130, false),
    FIRMWARE_INTERNAL_ERROR_NOT_FOUND(131, false),
    FIRMWARE_INTERNAL_ERROR_NOT_VALID(132, false),
    FIRMWARE_INTERNAL_ERROR_ALREADY_CREATE(133, false),
    FIRMWARE_INTERNAL_ERROR_NOT_ENOUGH_MEMORY(134, false),
    FIRMWARE_INTERNAL_ERROR_NOT_IMPLEMENTED(135, false),
    FIRMWARE_INTERNAL_ERROR_NOT_SUPPORT(136, false),
    FIRMWARE_INTERNAL_ERROR_SOCKET_BUSY(137, false),
    FIRMWARE_INTERNAL_ERROR_SOCKET_ALREADY_OPEN(138, false),
    FIRMWARE_INTERNAL_ERROR_INPUT_DATA_INVALID(139, false),
    FIRMWARE_INTERNAL_NOT_AUTHENTICATE(140, false),
    FIRMWARE_INTERNAL_SIZE_OVER_LIMIT(141, false),
    UNKNOWN(-1, false),

    // no clue what there one mean
    UNKNOWN_1(-125, true);

    boolean success;
    int code;

    private ResultCode(int code, boolean success){
        this.code = code;
        this.success = success;
    }

    public boolean inidicatesSuccess(){
        return this.success;
    }

    public static ResultCode fromCode(int code){
        for (ResultCode resultCode : ResultCode.values()){
            if(resultCode.code == code) {
                if(resultCode == UNKNOWN_1){
                    GB.log("dunno what code this is: " + code, GB.INFO, null);
                }
                return resultCode;
            }
        }
        return UNKNOWN;
    }
}
