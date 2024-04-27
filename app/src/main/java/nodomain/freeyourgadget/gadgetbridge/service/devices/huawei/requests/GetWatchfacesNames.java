/*  Copyright (C) 2024 Vitalii Tomin

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Watchface;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetWatchfacesNames extends Request{

    private static final Logger LOG = LoggerFactory.getLogger(GetWatchfacesNames.class);

    List<Watchface.InstalledWatchfaceInfo> watchfaceInfoList;
    public GetWatchfacesNames(HuaweiSupportProvider support, List<Watchface.InstalledWatchfaceInfo> list) {
        super(support);
        this.serviceId = Watchface.id;
        this.commandId = Watchface.WatchfaceNameInfo.id;
        this.watchfaceInfoList = list;

    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Watchface.WatchfaceNameInfo.Request(paramsProvider, this.watchfaceInfoList).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Watchface.WatchfaceNameInfo.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Watchface.WatchfaceNameInfo.Response.class);

        Watchface.WatchfaceNameInfo.Response resp = (Watchface.WatchfaceNameInfo.Response)(receivedPacket);
        supportProvider.getHuaweiWatchfaceManager().setWatchfacesNames(resp.watchFaceNames);
    }
}
