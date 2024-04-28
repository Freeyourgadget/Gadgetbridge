package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitRecordDataFactory {
    private FitRecordDataFactory() {
        // use create
    }

    public static RecordData create(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        switch (recordDefinition.getGlobalFITMessage().getNumber()) {
            case 0:
                return new FitFileId(recordDefinition, recordHeader);
            case 2:
                return new FitDeviceSettings(recordDefinition, recordHeader);
            case 3:
                return new FitUserProfile(recordDefinition, recordHeader);
            case 7:
                return new FitZonesTarget(recordDefinition, recordHeader);
            case 12:
                return new FitSport(recordDefinition, recordHeader);
            case 15:
                return new FitGoals(recordDefinition, recordHeader);
            case 20:
                return new FitRecord(recordDefinition, recordHeader);
            case 23:
                return new FitDeviceInfo(recordDefinition, recordHeader);
            case 49:
                return new FitFileCreator(recordDefinition, recordHeader);
            case 55:
                return new FitMonitoring(recordDefinition, recordHeader);
            case 127:
                return new FitConnectivity(recordDefinition, recordHeader);
            case 128:
                return new FitWeather(recordDefinition, recordHeader);
            case 159:
                return new FitWatchfaceSettings(recordDefinition, recordHeader);
            case 206:
                return new FitFieldDescription(recordDefinition, recordHeader);
            case 207:
                return new FitDeveloperData(recordDefinition, recordHeader);
            case 222:
                return new FitAlarmSettings(recordDefinition, recordHeader);
            case 227:
                return new FitStressLevel(recordDefinition, recordHeader);
            case 275:
                return new FitSleepStage(recordDefinition, recordHeader);
            case 346:
                return new FitSleepStats(recordDefinition, recordHeader);
        }

        return new RecordData(recordDefinition, recordHeader);
    }
}
