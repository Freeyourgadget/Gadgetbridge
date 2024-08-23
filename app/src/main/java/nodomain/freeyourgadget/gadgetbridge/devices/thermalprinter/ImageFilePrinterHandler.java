package nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class ImageFilePrinterHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ImageFilePrinterHandler.class);

    protected final Context mContext;
    private Bitmap incomingBitmap;

    public ImageFilePrinterHandler(final Uri uri, final Context context) {
        this.mContext = context;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }

        try {
            incomingBitmap = BitmapFactory.decodeStream(uriHelper.openInputStream());
        } catch (FileNotFoundException e) {
            LOG.error("Failed to create bitmap", e);
        }

    }

    public Bitmap getIncomingBitmap() {
        return incomingBitmap;
    }

    @Override
    public boolean isValid() {
        return incomingBitmap != null;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {

        installActivity.setPreview(incomingBitmap);
        installActivity.setInstallEnabled(true);

    }

    @Override
    public void onStartInstall(GBDevice device) {

    }
}
