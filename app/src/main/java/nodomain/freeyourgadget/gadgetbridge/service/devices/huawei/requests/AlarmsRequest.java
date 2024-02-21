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

import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Alarms.EventAlarmsRequest;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Alarms.SmartAlarmRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Alarms;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class AlarmsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmsRequest.class);

    private EventAlarmsRequest eventAlarmsRequest = null;
    private SmartAlarmRequest smartAlarmRequest = null;

    public AlarmsRequest(HuaweiSupportProvider support, boolean smart) {
        super(support);
        this.serviceId = Alarms.id;
        this.commandId = smart ? SmartAlarmRequest.id : EventAlarmsRequest.id;
        if (!smart)
            eventAlarmsRequest = new EventAlarmsRequest(support.getParamsProvider());
    }

    public void addEventAlarm(Alarm alarm, boolean increasePosition) {
        if (!alarm.getUnused()) {
            byte position = (byte) alarm.getPosition();
            if (increasePosition)
                position += 1;
            eventAlarmsRequest.addEventAlarm(new Alarms.EventAlarm(
                    position,
                    alarm.getEnabled(),
                    (byte) alarm.getHour(),
                    (byte) alarm.getMinute(),
                    (byte) alarm.getRepetition(),
                    alarm.getTitle()
            ));
        }
    }

    public void buildSmartAlarm(Alarm alarm) {
        Integer smartWakeupInterval = alarm.getSmartWakeupInterval();
        this.smartAlarmRequest = new SmartAlarmRequest(
                paramsProvider,
                new Alarms.SmartAlarm(
                        alarm.getEnabled() && !alarm.getUnused(),
                        (byte) alarm.getHour(),
                        (byte) alarm.getMinute(),
                        (byte) alarm.getRepetition(),
                        smartWakeupInterval == null ? 5 : smartWakeupInterval.byteValue()
                )
        );
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            if (eventAlarmsRequest != null) {
                return eventAlarmsRequest.serialize();
            } else if (smartAlarmRequest != null) {
                return smartAlarmRequest.serialize();
            } else {
                throw new RequestCreationException("No alarms set");
            }
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Alarm");
    }
}
