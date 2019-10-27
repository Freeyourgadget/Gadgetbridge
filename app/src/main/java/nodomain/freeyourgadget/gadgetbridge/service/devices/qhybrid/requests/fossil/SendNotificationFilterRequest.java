package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class SendNotificationFilterRequest extends FilePutRequest {
    public SendNotificationFilterRequest(FossilWatchAdapter adapter) {
        super((short) 0x0C00, createFile(), adapter);
    }

    private static byte[] createFile() {
        return null;
        /*ByteArrayOutputStream var3 = new ByteArrayOutputStream();
        byte var1 = NotificationEntryId.APP_BUNDLE_CRC32.getId$blesdk_productionRelease();
        byte[] var4 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)this.appBundleCrc).array();
        gwb.k(var4, "ByteBuffer.allocate(4)\n …                 .array()");
        var3.write(this.c(var1, var4));
        var3.write(this.c(NotificationEntryId.GROUP_ID.getId$blesdk_productionRelease(), new byte[]{this.groupId}));
        boolean var2;
        if (((CharSequence)this.sender).length() > 0) {
            var2 = true;
        } else {
            var2 = false;
        }

        if (var2) {
            var1 = NotificationEntryId.SENDER_NAME.getId$blesdk_productionRelease();
            String var7 = bcf.da(this.sender);
            Charset var5 = bdq.bqp.Qs();
            if (var7 == null) {
                throw new TypeCastException("null cannot be cast to non-null type java.lang.String");
            }

            var4 = var7.getBytes(var5);
            gwb.k(var4, "(this as java.lang.String).getBytes(charset)");
            var3.write(this.c(var1, var4));
        }

        if (this.priority != (short)-1) {
            var3.write(this.c(NotificationEntryId.PRIORITY.getId$blesdk_productionRelease(), new byte[]{(byte)this.priority}));
        }

        NotificationHandMovingConfig var8 = this.handMovingConfig;
        if (var8 != null) {
            var3.write(this.c(NotificationEntryId.HAND_MOVING.getId$blesdk_productionRelease(), var8.getData$blesdk_productionRelease()));
        }

        NotificationVibePattern var9 = this.vibePattern;
        if (var9 != null) {
            var3.write(this.c(NotificationEntryId.VIBE.getId$blesdk_productionRelease(), new byte[]{var9.getId$blesdk_productionRelease()}));
        }

        NotificationIconConfig var10 = this.iconConfig;
        if (var10 != null) {
            var3.write(this.c(NotificationEntryId.ICON_IMAGE.getId$blesdk_productionRelease(), var10.getNotificationFilterIconConfigData$blesdk_productionRelease()));
        }

        byte[] var6 = var3.toByteArray();
        var6 = ByteBuffer.allocate(var6.length + 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)var6.length).put(var6).array();
        gwb.k(var6, "ByteBuffer\n             …\n                .array()");
        return var6;*/
    }
}
