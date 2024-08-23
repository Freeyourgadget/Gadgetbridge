package nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.BitmapToBitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.GenericThermalPrinterSupport;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;


public class SendToPrinterActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(SendToPrinterActivity.class);

    private Bitmap bitmap;
    private ImageView previewImage;
    private MaterialSwitch dithering;

    private BitmapToBitSet bitmapToBitSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_print_image);
        ImageView incomingImage = findViewById(R.id.incomingImage);
        previewImage = findViewById(R.id.convertedImage);
        Button sendToPrinter = findViewById(R.id.sendToPrinterButton);
        dithering = findViewById(R.id.switchDithering);
        final TextView warning = findViewById(R.id.warning_devices);

        final List<GBDevice> devices = ((GBApplication) getApplicationContext()).getDeviceManager().getSelectedDevices();
        GBDevice device = devices.get(0);

        switch (devices.size()) {
            case 0:
                warning.setText(R.string.open_fw_installer_connect_minimum_one_device);
                sendToPrinter.setEnabled(false);
                break;
            case 1:
                warning.setText(String.format(getString(R.string.open_fw_installer_select_file), device.getAliasOrName()));
                sendToPrinter.setEnabled(true);
                break;
            default:
                warning.setText(R.string.open_fw_installer_connect_maximum_one_device);
                sendToPrinter.setEnabled(false);
        }

        dithering.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOG.info("dithering is : {}", dithering.isChecked());
                bitmapToBitSet.toBlackAndWhite(dithering.isChecked());

                previewImage.setImageBitmap(bitmapToBitSet.getPreview());
            }
        });


        Uri uri = getIntent().getData();
        if (uri == null) { // For "share" intent
            uri = getIntent().getParcelableExtra(GenericThermalPrinterSupport.INTENT_EXTRA_URI);
        }

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, getApplicationContext());
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }

        try {
            final Bitmap incoming = BitmapFactory.decodeStream(uriHelper.openInputStream());
            if (incoming.getWidth() > 384) {
                float aspectRatio = (float) incoming.getHeight() / incoming.getWidth();
                bitmap = Bitmap.createScaledBitmap(incoming, 384, (int) (384 * aspectRatio), true);
            } else {
                bitmap = incoming;
            }
        } catch (FileNotFoundException e) {
            LOG.error("Failed to create bitmap", e);
        }
        incomingImage.setImageBitmap(bitmap);

        bitmapToBitSet = new BitmapToBitSet(bitmap);
        bitmapToBitSet.toBlackAndWhite(dithering.isChecked());

        previewImage.setImageBitmap(bitmapToBitSet.getPreview());

        sendToPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToPrinter();
            }
        });
    }

    private void sendToPrinter() {
        Intent intent = new Intent(GenericThermalPrinterSupport.INTENT_ACTION_PRINT_BITMAP);
        //TODO: this is horrible
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        intent.putExtra(GenericThermalPrinterSupport.INTENT_EXTRA_BITMAP, stream.toByteArray());

        intent.putExtra(GenericThermalPrinterSupport.INTENT_EXTRA_APPLY_DITHERING, dithering.isChecked());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
