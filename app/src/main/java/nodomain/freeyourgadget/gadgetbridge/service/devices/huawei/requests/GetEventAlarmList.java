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

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Alarms;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetEventAlarmList extends Request {

    public GetEventAlarmList(HuaweiSupportProvider support) {
        super(support);

        this.serviceId = Alarms.id;
        this.commandId = Alarms.EventAlarmsList.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsChangingAlarm();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Alarms.EventAlarmsList.Request(supportProvider.getParamsProvider()).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Alarms.EventAlarmsList.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Alarms.EventAlarmsList.Response.class);

        List<Alarm> alarms = new ArrayList<>();

        // Correct for position of smart alarm
        // Note that the band uses 1 as the first index for event alarms
        int positionOffset;
        if (supportProvider.getCoordinator().getHuaweiCoordinator().supportsSmartAlarm(supportProvider.getDevice()))
            positionOffset = 0;
        else
            positionOffset = -1;

        byte usedBitmap = 0;

        for (Alarms.EventAlarm eventAlarm : ((Alarms.EventAlarmsList.Response) receivedPacket).eventAlarms) {
            alarms.add(new Alarm(
                    0,
                    0,
                    eventAlarm.index + positionOffset,
                    eventAlarm.status,
                    false,
                    null,
                    false,
                    eventAlarm.repeat,
                    eventAlarm.startHour,
                    eventAlarm.startMinute,
                    false,
                    eventAlarm.name,
                    "",
                    0,
                    true
            ));
            usedBitmap |= 1 << eventAlarm.index;
        }

        // Add all unused alarms as unused
        for (int i = 1; i < 6; i++) {
            if ((usedBitmap & (1 << i)) == 0) {
                alarms.add(new Alarm(
                        0,
                        0,
                        i + positionOffset,
                        false,
                        false,
                        null,
                        false,
                        0,
                        0,
                        0,
                        true,
                        "",
                        "",
                        0,
                        true
                ));
            }
        }

        supportProvider.saveAlarms(alarms.toArray(new Alarm[]{}));
    }
}
