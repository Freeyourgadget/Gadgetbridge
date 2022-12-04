/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Dmytro
    Bielik, pangwalla

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts2;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts2.AmazfitGTS2MiniFWHelper;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiIcon;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AmazfitGTS2MiniSupport extends AmazfitGTS2Support {

    private static final Logger LOG = LoggerFactory.getLogger(AmazfitGTS2MiniSupport.class);

    @Override
    protected HuamiSupport setLanguage(TransactionBuilder builder) {
        return setLanguageByIdNew(builder);
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTS2MiniFWHelper(uri, context);
    }

    @Override
    public String getNotificationBody(NotificationSpec notificationSpec){
        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        byte customIconId = HuamiIcon.mapToIconId(notificationSpec.type);
        boolean acceptsSender = HuamiIcon.acceptsSender(customIconId);
        String message;

        /* The title will be displayed beside the icon depending on the icon ID sent to the
           device. If the icon ID does not admit a title, it will display the app's name, and
           we will repeat the subject as part of the notification body, but only if the app name
           is different from the subject. That way it's aesthetically pleasing.
         */
        if(!acceptsSender && !senderOrTitle.equals(notificationSpec.sourceName)) {
            message = "-\0"; //if the sender is not accepted, whatever goes in this field is ignored
            message += senderOrTitle + "\n";
        } else {
            message = senderOrTitle + "\0";
        }

        if(notificationSpec.subject != null) {
            message += StringUtils.truncate(notificationSpec.subject, 128) + "\n\n";
        }

        if(notificationSpec.body != null) {
            message += StringUtils.truncate(notificationSpec.body, 512);
        }

        if(notificationSpec.body == null && notificationSpec.subject == null) {
            message += " ";
        }

        return message;
    }
}
