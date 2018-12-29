/*      Copyright (C) 2018 Andreas Böhler
        based on code from BlueWatcher, https://github.com/masterjc/bluewatcher

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casiogb6900;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasioGATTThread extends Thread {
    CasioGATTServer mServer = null;
    private static final Logger LOG = LoggerFactory.getLogger(CasioGATTThread.class);
    private boolean mStopFlag = false;

    public CasioGATTThread(Context context, CasioGB6900DeviceSupport deviceSupport)
    {
        mServer = new CasioGATTServer(context, deviceSupport);
    }

    public void setContext(Context ctx) {
        mServer.setContext(ctx);
    }

    @Override
    public void run()
    {
        if(!mServer.initialize()) {
            LOG.error("Error initializing CasioGATTServer. Has the context been set?");
            return;
        }
        while(!mStopFlag) {
            try {
                wait(100);
            } catch(Exception e)
            {

            }
        }
        mServer.close();
    }

    public void quit() {
        mStopFlag = true;
    }


}
