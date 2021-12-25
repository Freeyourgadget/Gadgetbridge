/*  Copyright (C) 2019-2021 Daniel Dakhno

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
    UNKNOWN_1(-125, false);

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
