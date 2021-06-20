/*  Copyright (C) 2021 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class HybridHRWatchfaceDesignerActivity extends AppCompatActivity implements View.OnClickListener {
    private final Logger LOG = LoggerFactory.getLogger(HybridHRWatchfaceDesignerActivity.class);
    private GBDevice mGBDevice;
    private DeviceCoordinator mCoordinator;
    private int displayImageSize = 0;
    private float scaleFactor = 0;
    private ImageView backgroundImageView;
    private Bitmap selectedBackgroundImage, processedBackgroundImage;
    private String watchfaceName = "NewWatchface";
    final private ArrayList<HybridHRWatchfaceWidget> widgets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybridhr_watchface_designer);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mGBDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        mCoordinator = DeviceHelper.getInstance().getCoordinator(mGBDevice);

        calculateDisplayImageSize();
        backgroundImageView = findViewById(R.id.hybridhr_background_image);
        renderWatchfacePreview();

        findViewById(R.id.button_edit_name).setOnClickListener(this);
        findViewById(R.id.button_set_background).setOnClickListener(this);
        findViewById(R.id.button_add_widget).setOnClickListener(this);
        findViewById(R.id.button_watchface_settings).setOnClickListener(this);
        findViewById(R.id.button_preview_watchface).setOnClickListener(this);
        findViewById(R.id.button_save_watchface).setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
            Uri imageUri = resultData.getData();
            if (imageUri == null) {
                LOG.warn("No image selected");
                return;
            }
            try {
                selectedBackgroundImage = createImageFromURI(imageUri);
            } catch (IOException e) {
                LOG.warn("Error converting selected image to Bitmap", e);
                return;
            }
            renderWatchfacePreview();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_edit_name) {
            final EditText input = new EditText(this);
            input.setText(watchfaceName);
            input.setId(0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            new AlertDialog.Builder(this)
                    .setView(input)
                    .setNegativeButton(R.string.fossil_hr_new_action_cancel, null)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            watchfaceName = input.getText().toString();
                            TextView watchfaceNameView = findViewById(R.id.watchface_name);
                            watchfaceNameView.setText(watchfaceName);
                        }
                    })
                    .setTitle("Set watchface name")
                    .show();
        } else if (v.getId() == R.id.button_set_background) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 42);
        } else if (v.getId() == R.id.button_add_widget) {
            showWidgetEditPopup(-1);
        } else if (v.getId() == R.id.button_preview_watchface) {
            sendToWatch(true);
        } else if (v.getId() == R.id.button_save_watchface) {
            sendToWatch(false);
        }
    }

    private void renderWatchfacePreview() {
        int widgetSize = 50;
        if (selectedBackgroundImage == null) {
            processedBackgroundImage = Bitmap.createBitmap(displayImageSize, displayImageSize, Bitmap.Config.ARGB_8888);
            // Paint a gray circle around the watchface
            Canvas backgroundImageCanvas = new Canvas(processedBackgroundImage);
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.GRAY);
            circlePaint.setAntiAlias(true);
            circlePaint.setStrokeWidth(3);
            circlePaint.setStyle(Paint.Style.STROKE);
            backgroundImageCanvas.drawCircle(displayImageSize/2f + 2, displayImageSize/2f + 2, displayImageSize/2f - 5, circlePaint);
        } else {
            processedBackgroundImage = Bitmap.createScaledBitmap(selectedBackgroundImage, displayImageSize, displayImageSize, false);
        }
        // Remove existing widget ImageViews
        RelativeLayout imageContainer = this.findViewById(R.id.watchface_preview_image);
        boolean onlyPreviewIsRemaining = false;
        while (!onlyPreviewIsRemaining) {
            int childCount = imageContainer.getChildCount();
            int i;
            for(i=0; i<childCount; i++) {
                View currentChild = imageContainer.getChildAt(i);
                if (currentChild.getId() != R.id.hybridhr_background_image) {
                    imageContainer.removeView(currentChild);
                    break;
                }
            }
            if (i == childCount) {
                onlyPreviewIsRemaining = true;
            }
        }
        // Dynamically add an ImageView for each widget
        Paint widgetPaint = new Paint();
        widgetPaint.setColor(Color.RED);
        widgetPaint.setStyle(Paint.Style.STROKE);
        widgetPaint.setStrokeWidth(5);
        Bitmap widgetBitmap = Bitmap.createBitmap((int)(widgetSize * scaleFactor), (int)(widgetSize * scaleFactor), Bitmap.Config.ARGB_8888);
        Canvas widgetCanvas = new Canvas(widgetBitmap);
        widgetCanvas.drawRect(0, 0, widgetSize * scaleFactor, widgetSize * scaleFactor, widgetPaint);
        for (int i=0; i<widgets.size(); i++) {
            HybridHRWatchfaceWidget widget = widgets.get(i);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT, backgroundImageView.getId());
            layoutParams.addRule(RelativeLayout.ALIGN_TOP, backgroundImageView.getId());
            layoutParams.setMargins((int) ((widget.getPosX() - widgetSize/2) * scaleFactor), (int) ((widget.getPosY() - widgetSize/2) * scaleFactor), 0, 0);
            ImageView widgetView = new ImageView(this);
            widgetView.setId(i);
            widgetView.setImageBitmap(widgetBitmap);
            widgetView.setLayoutParams(layoutParams);
            widgetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showWidgetEditPopup(v.getId());
                }
            });
            imageContainer.addView(widgetView);
        }
        backgroundImageView.setImageBitmap(processedBackgroundImage);
    }

    private void showWidgetEditPopup(final int index) {
        LinearLayout layout = new LinearLayout(this);
        HybridHRWatchfaceWidget widget = null;
        if (index >= 0) {
            widget = widgets.get(index);
        }
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView desc = new TextView(this);
        desc.setText("Type:");
        layout.addView(desc);
        final RadioGroup typeSelector = new RadioGroup(this);
        RadioButton typeDate = new RadioButton(this);
        typeDate.setText("Date");
        typeDate.setId(0);
        if ((widget != null) && (widget.getWidgetType().equals("widgetDate"))) {
            typeDate.setChecked(true);
        }
        typeSelector.addView(typeDate);
        RadioButton typeWeather = new RadioButton(this);
        typeWeather.setText("Weather");
        typeWeather.setId(0+1);
        if ((widget != null) && (widget.getWidgetType().equals("widgetWeather"))) {
            typeWeather.setChecked(true);
        }
        typeSelector.addView(typeWeather);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        typeSelector.setLayoutParams(lp);
        layout.addView(typeSelector);
        desc = new TextView(this);
        desc.setText("X coordinate:");
        layout.addView(desc);
        final NumberPicker posX = new NumberPicker(this);
        posX.setMinValue(1);
        posX.setMaxValue(240);
        if (widget != null) {
            posX.setValue(widget.getPosX());
        }
        layout.addView(posX);
        desc = new TextView(this);
        desc.setText("Y coordinate:");
        layout.addView(desc);
        final NumberPicker posY = new NumberPicker(this);
        posY.setMinValue(1);
        posY.setMaxValue(240);
        if (widget != null) {
            posY.setValue(widget.getPosY());
        }
        layout.addView(posY);
        new AlertDialog.Builder(this)
                .setView(layout)
                .setNegativeButton(R.string.fossil_hr_edit_action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (index >= 0) {
                            widgets.remove(index);
                            renderWatchfacePreview();
                        }
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedRadioId = typeSelector.getCheckedRadioButtonId();
                        int selectedPosX = posX.getValue();
                        int selectedPosY = posY.getValue();
                        switch (selectedRadioId) {
                            case 0:
                                if (index >= 0) {
                                    widgets.set(index, new HybridHRWatchfaceWidget("widgetDate", selectedPosX, selectedPosY));
                                } else {
                                    widgets.add(new HybridHRWatchfaceWidget("widgetDate", selectedPosX, selectedPosY));
                                }
                                break;
                            case 1:
                                if (index >= 0) {
                                    widgets.set(index, new HybridHRWatchfaceWidget("widgetWeather", selectedPosX, selectedPosY));
                                } else {
                                    widgets.add(new HybridHRWatchfaceWidget("widgetWeather", selectedPosX, selectedPosY));
                                }
                                break;
                        }
                        renderWatchfacePreview();
                    }
                })
                .setTitle("Add widget")
                .show();
    }

    private void calculateDisplayImageSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayImageSize = (int) Math.round(displayMetrics.widthPixels * 0.75);
        scaleFactor = displayImageSize / 240f;
    }

    private Bitmap createImageFromURI(Uri imageUri) throws IOException, RuntimeException {
        if (imageUri == null) {
            throw new RuntimeException("No image selected");
        }

//        UriHelper uriHelper = UriHelper.get(imageUri, this);
//        InputStream in = new BufferedInputStream(uriHelper.openInputStream());
//        Bitmap bitmap = BitmapFactory.decodeStream(in);

        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(imageUri, new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
        c.moveToFirst();
        int orientation = c.getInt(c.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION));
        c.close();
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        if (orientation != 0) {  // FIXME: doesn't seem to work
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return BitmapUtil.convertToGrayscale(BitmapUtil.getCircularBitmap(bitmap));
    }

    private void sendToWatch(boolean preview) {
        HybridHRWatchfaceFactory wfFactory;
        if (preview) {
            wfFactory = new HybridHRWatchfaceFactory("previewWatchface");
        } else {
            wfFactory = new HybridHRWatchfaceFactory(watchfaceName);
        }
        wfFactory.setBackground(processedBackgroundImage);
        wfFactory.addWidgets(widgets);
        try {
            File tempFile = File.createTempFile("tmpWatchfaceFile", null);
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(wfFactory.getWapp(this));
            bos.close();
            fos.close();
            Uri tempAppFileUri = Uri.fromFile(tempFile);
            GBApplication.deviceService().onInstallApp(tempAppFileUri);
            if (preview) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onAppDelete(UUID.nameUUIDFromBytes("previewWatchface".getBytes(StandardCharsets.UTF_8)));
                    }
                }, 10000);
            } else {
                FossilFileReader fossilFile = new FossilFileReader(tempAppFileUri, this);
                FossilHRInstallHandler.saveAppInCache(fossilFile, processedBackgroundImage, mCoordinator, this);
            }
        } catch (IOException e) {
            LOG.warn("Error while creating and uploading watchface", e);
        }
    }
}