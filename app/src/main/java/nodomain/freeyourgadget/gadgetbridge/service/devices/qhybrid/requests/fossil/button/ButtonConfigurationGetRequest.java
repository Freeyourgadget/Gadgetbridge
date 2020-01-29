/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.button;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig.ConfigPayload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRequest;

public class ButtonConfigurationGetRequest extends FileGetRequest {
    public ButtonConfigurationGetRequest(FossilWatchAdapter adapter) {
        super((short) 0x0600, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        log("fileData");

        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        short fileHandle = buffer.getShort(0);
        // TODO check file handle
        // if(fileData != )

        byte count = buffer.get(15);

        ConfigPayload[] configs = new ConfigPayload[count];

        buffer.position(16);
        for(int i = 0; i < count; i++){
            int buttonIndex = buffer.get() >> 4;
            int entryCount = buffer.get();
            buffer.get();
            short appId = buffer.getShort();

            buffer.position(buffer.position() + entryCount * 5 - 3);

            try {
                configs[buttonIndex - 1] = ConfigPayload.fromId(appId);
            }catch (RuntimeException e){
                configs[buttonIndex - 1] =  null;
            }
        }

        this.onConfigurationsGet(configs);
    }

    public void onConfigurationsGet(ConfigPayload[] configs){}
}
