package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.util.Pair;

import androidx.annotation.Nullable;

public class FileType {
    //common
    //128/4: FIT_TYPE_4, -> garmin/activity
    //128/32: FIT_TYPE_32,  -> garmin/monitor
    //128/44: FIT_TYPE_44, ->garmin/metrics
    //128/41: FIT_TYPE_41, ->garmin/chnglog
    //128/49: FIT_TYPE_49, -> garmin/sleep
    //255/245: ErrorShutdownReports,

    //Specific Instinct 2S:
    //128/38: FIT_TYPE_38, -> garmin/SCORCRDS
    //255/248: KPI,
    //128/58: FIT_TYPE_58, -> outputFromUnit garmin/device????
    //255/247: ULFLogs,
    //128/68: FIT_TYPE_68, -> garmin/HRVSTATUS
    //128/70: FIT_TYPE_70, -> garmin/HSA
    //128/72: FIT_TYPE_72, -> garmin/FBTBACKUP
    //128/74: FIT_TYPE_74


    private final FILETYPE fileType;
    private final String garminDeviceFileType;

    public FileType(int fileDataType, int fileSubType, String garminDeviceFileType) {
        this.fileType = FILETYPE.fromDataTypeSubType(fileDataType, fileSubType);
        this.garminDeviceFileType = garminDeviceFileType;
    }

    public FILETYPE getFileType() {
        return fileType;
    }

    public enum FILETYPE { //TODO: add specialized method to parse each file type to the enum?
        ACTIVITY(Pair.create(128, 4)),
        MONITOR(Pair.create(128, 32)),
        CHANGELOG(Pair.create(128, 41)),
        METRICS(Pair.create(128, 44)),
        SLEEP(Pair.create(128, 49)),

        //"virtual" and/or undocumented file types
        DIRECTORY(Pair.create(0, 0)),
//        SETTINGS(Pair.create(128,2)),
        ;

        private final Pair<Integer, Integer> type;

        FILETYPE(Pair<Integer, Integer> pair) {
            this.type = pair;
        }

        @Nullable
        public static FILETYPE fromDataTypeSubType(int dataType, int subType) {
            for (FILETYPE ft :
                    FILETYPE.values()) {
                if (ft.type.first == dataType && ft.type.second == subType)
                    return ft;
            }
            return null;
        }

        public int getType() {
            return type.first;
        }

        public int getSubType() {
            return type.second;
        }

        public boolean isFitFile() {
            return type.first == 128;
        }
    }
}
