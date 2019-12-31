package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

public enum ResultCode {
    SUCCESS(0),
    INVALID_OPERATION_DATA(1),
    OPERATION_IN_PROGRESS(2),
    MISS_PACKET(3),
    SOCKET_BUSY(4),
    VERIFICATION_FAIL(5),
    OVERFLOW(6),
    SIZE_OVER_LIMIT(7),
    FIRMWARE_INTERNAL_ERROR(128),
    FIRMWARE_INTERNAL_ERROR_NOT_OPEN(129),
    FIRMWARE_INTERNAL_ERROR_ACCESS_ERROR(130),
    FIRMWARE_INTERNAL_ERROR_NOT_FOUND(131),
    FIRMWARE_INTERNAL_ERROR_NOT_VALID(132),
    FIRMWARE_INTERNAL_ERROR_ALREADY_CREATE(133),
    FIRMWARE_INTERNAL_ERROR_NOT_ENOUGH_MEMORY(134),
    FIRMWARE_INTERNAL_ERROR_NOT_IMPLEMENTED(135),
    FIRMWARE_INTERNAL_ERROR_NOT_SUPPORT(136),
    FIRMWARE_INTERNAL_ERROR_SOCKET_BUSY(137),
    FIRMWARE_INTERNAL_ERROR_SOCKET_ALREADY_OPEN(138),
    FIRMWARE_INTERNAL_ERROR_INPUT_DATA_INVALID(139),
    FIRMWARE_INTERNAL_NOT_AUTHENTICATE(140),
    FIRMWARE_INTERNAL_SIZE_OVER_LIMIT(141),
    UNKNOWN(-1);
    int code;

    ResultCode(int code) {
        this.code = code;
    }

    public static ResultCode fromCode(int code){
        for (ResultCode resultCode : ResultCode.values()){
            if(resultCode.code == code) return resultCode;
        }
        return UNKNOWN;
    }
}
