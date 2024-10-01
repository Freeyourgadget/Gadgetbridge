package nodomain.freeyourgadget.gadgetbridge.devices.casio.ecbs100;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.Casio2C2DDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.ecbs100.CasioECBS100DeviceSupport;

public class CasioECBS100DeviceCoordinator extends Casio2C2DDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_BOND;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_dateformat_day_month_order,
                R.xml.devicesettings_operating_sounds,
                R.xml.devicesettings_hourly_chime_enable,
                R.xml.devicesettings_autolight,
                R.xml.devicesettings_light_duration_longer,
                R.xml.devicesettings_power_saving,
                R.xml.devicesettings_casio_connection_duration,
                R.xml.devicesettings_time_sync,

                // timer
                // reminder
                // world time
        };
    }


    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 5;
    }


    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return CasioECBS100DeviceSupport.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("CASIO ECB-S100");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_casioecbs100;
    }
}
