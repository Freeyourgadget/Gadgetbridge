package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

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
        // virtual/undocumented
        DIRECTORY(0, 0),

        // fit files
        ACTIVITY(128, 4),
        WORKOUTS(128, 5),
        SCHEDULES(128, 7),
        LOCATION(128, 8),
        TOTALS(128, 10),
        GOALS(128, 11),
        SUMMARY(128, 20),
        RECORDS(128, 29),
        MONITOR(128, 32),
        CLUBS(128, 37),
        SCORE(128, 38),
        ADJUSTMENTS(128, 39),
        CHANGELOG(128, 41),
        METRICS(128, 44),
        SLEEP(128, 49),
        MUSCLE_MAP(128, 59),
        ECG(128, 61),
        BENCHMARK(128, 62),
        HRV_STATUS(128, 68),
        HSA(128, 70),
        FBT_BACKUP(128, 72),
        SKIN_TEMP(128, 73),
        FBT_PTD_BACKUP(128, 74),

        // Other files
        ERROR_SHUTDOWN_REPORTS(255, 245),
        IQ_ERROR_REPORTS(255, 244),
        ULF_LOGS(255, 247),
        ;

        private final int type;
        private final int subtype;

        FILETYPE(final int type, final int subtype) {
            this.type = type;
            this.subtype = subtype;
        }

        @Nullable
        public static FILETYPE fromDataTypeSubType(int dataType, int subType) {
            for (FILETYPE ft :
                    FILETYPE.values()) {
                if (ft.type == dataType && ft.subtype == subType)
                    return ft;
            }
            return null;
        }

        public int getType() {
            return type;
        }

        public int getSubType() {
            return subtype;
        }

        public boolean isFitFile() {
            return type == 128;
        }
    }
}
