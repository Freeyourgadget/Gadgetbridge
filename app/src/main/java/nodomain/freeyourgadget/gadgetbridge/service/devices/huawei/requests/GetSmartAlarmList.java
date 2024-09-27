/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Alarms;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetSmartAlarmList extends Request {

    public GetSmartAlarmList(HuaweiSupportProvider support) {
        super(support);

        this.serviceId = Alarms.id;
        this.commandId = Alarms.SmartAlarmList.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsChangingAlarm() &&
                supportProvider.getHuaweiCoordinator().supportsSmartAlarm(supportProvider.getDevice());
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Alarms.SmartAlarmList.Request(supportProvider.getParamsProvider()).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Alarms.SmartAlarmList.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Alarms.SmartAlarmList.Response.class);

        Alarms.SmartAlarm smartAlarm = ((Alarms.SmartAlarmList.Response) receivedPacket).smartAlarm;

        if (smartAlarm != null) {
            supportProvider.saveAlarms(new Alarm[] {
                    new Alarm(
                            0,
                            0,
                            0,
                            smartAlarm.status,
                            true,
                            (int) smartAlarm.aheadTime,
                            false,
                            smartAlarm.repeat,
                            smartAlarm.startHour,
                            smartAlarm.startMinute,
                            false,
                            "Smart alarm",
                            "",
                            0,
                            true
                    )
            });
        } else {
            // Set empty smart alarm so index zero is always smart alarm
            supportProvider.saveAlarms(new Alarm[] {
                    new Alarm(
                            0,
                            0,
                            0,
                            false,
                            true,
                            null,
                            false,
                            0,
                            0,
                            0,
                            true,
                            "Smart alarm",
                            "",
                            0,
                            true
                    )
            });
        }
    }
}
