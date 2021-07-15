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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HybridHRWatchfaceDesignerActivity extends AbstractGBActivity implements View.OnClickListener {
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

        if (bundle.containsKey(GBDevice.EXTRA_UUID)) {
            String appUUID = bundle.getString(GBDevice.EXTRA_UUID);
            loadConfigurationFromApp(appUUID);
        }

        renderWatchfacePreview();

        findViewById(R.id.button_edit_name).setOnClickListener(this);
        findViewById(R.id.button_set_background).setOnClickListener(this);
        findViewById(R.id.button_add_widget).setOnClickListener(this);
        findViewById(R.id.button_watchface_settings).setOnClickListener(this);
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

    // Add action bar buttons
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_hybridhr_watchface_designer_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Handle action bar button presses
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.button_save_watchface) {
            sendToWatch(false);
            finish();
        } else if (id == R.id.button_preview_watchface) {
            sendToWatch(true);
        }
        return super.onOptionsItemSelected(item);
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
                            setWatchfaceName(input.getText().toString());
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
        }
    }

    private void loadConfigurationFromApp(String appUUID) {
        File appCacheDir;
        try {
            appCacheDir = mCoordinator.getAppCacheDir();
        } catch (IOException e) {
            LOG.warn("Could not get external dir while trying to access app cache.", e);
            return;
        }
        File backgroundFile = new File(appCacheDir, appUUID + ".png");
        try {
            Bitmap cachedBackground = BitmapFactory.decodeStream(new FileInputStream(backgroundFile));
            selectedBackgroundImage = BitmapUtil.convertToGrayscale(BitmapUtil.getCircularBitmap(cachedBackground));
        } catch (IOException e) {
            LOG.warn("Error loading cached background image", e);
        }
        File cachedFile = new File(appCacheDir, appUUID + mCoordinator.getAppFileExtension());
        FossilFileReader fileReader;
        try {
            fileReader = new FossilFileReader(Uri.fromFile(cachedFile), this);
        } catch (IOException e) {
            LOG.warn("Could not open cached app file", e);
            return;
        }
        setWatchfaceName(fileReader.getName());
        JSONObject configJSON;
        try {
            configJSON = fileReader.getConfigJSON("customWatchFace");
        } catch (IOException e) {
            LOG.warn("Could not read config from cached app file", e);
            return;
        } catch (JSONException e) {
            LOG.warn("JSON parsing error", e);
            return;
        }
        if (configJSON == null) {
            return;
        }
        for (Iterator<String> it = configJSON.keys(); it.hasNext(); ) {
            String key = it.next();
            if (key.equals("layout")) {
                try {
                    JSONArray layout = configJSON.getJSONArray(key);
                    for (int i = 0; i < layout.length(); i++) {
                        JSONObject layoutItem = layout.getJSONObject(i);
                        if (layoutItem.getString("type").equals("comp")) {
                            String widgetName = layoutItem.getString("name");
                            switch (widgetName) {
                                case "dateSSE":
                                    widgetName = "widgetDate";
                                    break;
                                case "weatherSSE":
                                    widgetName = "widgetWeather";
                                    break;
                                case "stepsSSE":
                                    widgetName = "widgetSteps";
                                    break;
                                case "hrSSE":
                                    widgetName = "widgetHR";
                                    break;

                            }
                            widgets.add(new HybridHRWatchfaceWidget(widgetName,
                                                                    layoutItem.getJSONObject("pos").getInt("x"),
                                                                    layoutItem.getJSONObject("pos").getInt("y")));
                        }
                    }
                } catch (JSONException e) {
                    LOG.warn("JSON parsing error", e);
                }
            }
        }
    }

    private void setWatchfaceName(String name) {
        watchfaceName = name;
        TextView watchfaceNameView = findViewById(R.id.watchface_name);
        watchfaceNameView.setText(watchfaceName);
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
            processedBackgroundImage = Bitmap.createScaledBitmap(selectedBackgroundImage, displayImageSize, displayImageSize, true);
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
        Bitmap widgetNoPreviewBitmap = Bitmap.createBitmap((int)(widgetSize * scaleFactor), (int)(widgetSize * scaleFactor), Bitmap.Config.ARGB_8888);
        Canvas widgetCanvas = new Canvas(widgetNoPreviewBitmap);
        widgetCanvas.drawRect(0, 0, widgetSize * scaleFactor, widgetSize * scaleFactor, widgetPaint);
        for (int i=0; i<widgets.size(); i++) {
            HybridHRWatchfaceWidget widget = widgets.get(i);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT, backgroundImageView.getId());
            layoutParams.addRule(RelativeLayout.ALIGN_TOP, backgroundImageView.getId());
            layoutParams.setMargins((int) ((widget.getPosX() - widgetSize/2) * scaleFactor), (int) ((widget.getPosY() - widgetSize/2) * scaleFactor), 0, 0);
            ImageView widgetView = new ImageView(this);
            widgetView.setId(i);
            try {
                widgetView.setImageBitmap(Bitmap.createScaledBitmap(widget.getPreviewImage(this), (int)(widgetSize * scaleFactor), (int)(widgetSize * scaleFactor), true));
            } catch (IOException e) {
                widgetView.setImageBitmap(widgetNoPreviewBitmap);
            }
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
        View layout = getLayoutInflater().inflate(R.layout.dialog_hybridhr_watchface_widget, null);
        HybridHRWatchfaceWidget widget = null;
        if (index >= 0) {
            widget = widgets.get(index);
        }
        // Configure widget type dropdown
        final Spinner typeSpinner = layout.findViewById(R.id.watchface_widget_type_spinner);
        LinkedHashMap<String, String> widgetTypes = HybridHRWatchfaceWidget.getAvailableWidgetTypes(this);
        ArrayAdapter<String> widgetTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, widgetTypes.values().toArray(new String[0]));
        typeSpinner.setAdapter(widgetTypeAdapter);
        final ArrayList<String> widgetTypesArray = new ArrayList<>(widgetTypes.keySet());
        if ((widget != null) && (widgetTypesArray.contains(widget.getWidgetType()))) {
            typeSpinner.setSelection(widgetTypesArray.indexOf(widget.getWidgetType()));
        }
        // Set X coordinate
        final EditText posX = layout.findViewById(R.id.watchface_widget_pos_x);
        if (widget != null) {
            posX.setText(Integer.toString(widget.getPosX()));
        }
        // Set Y coordinate
        final EditText posY = layout.findViewById(R.id.watchface_widget_pos_y);
        if (widget != null) {
            posY.setText(Integer.toString(widget.getPosY()));
        }
        // Configure position preset buttons
        Button btnTop = layout.findViewById(R.id.watchface_widget_preset_top);
        btnTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posX.setText("120");
                posY.setText("58");
            }
        });
        Button btnBottom = layout.findViewById(R.id.watchface_widget_preset_bottom);
        btnBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posX.setText("120");
                posY.setText("182");
            }
        });
        Button btnLeft = layout.findViewById(R.id.watchface_widget_preset_left);
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posX.setText("58");
                posY.setText("120");
            }
        });
        Button btnRight = layout.findViewById(R.id.watchface_widget_preset_right);
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posX.setText("182");
                posY.setText("120");
            }
        });
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
                        int selectedPosX;
                        int selectedPosY;
                        try {
                            selectedPosX = Integer.parseInt(posX.getText().toString());
                            selectedPosY = Integer.parseInt(posY.getText().toString());
                        } catch (NumberFormatException e) {
                            GB.toast("Settings incomplete, widget not added", Toast.LENGTH_SHORT, GB.WARN);
                            LOG.warn("Error parsing input", e);
                            return;
                        }
                        if (selectedPosX < 1) selectedPosX = 1;
                        if (selectedPosX > 240) selectedPosX = 240;
                        if (selectedPosY < 1) selectedPosY = 1;
                        if (selectedPosY > 240) selectedPosY = 240;
                        String selectedType = widgetTypesArray.get(typeSpinner.getSelectedItemPosition());
                        if (index >= 0) {
                            widgets.set(index, new HybridHRWatchfaceWidget(selectedType, selectedPosX, selectedPosY));
                        } else {
                            widgets.add(new HybridHRWatchfaceWidget(selectedType, selectedPosX, selectedPosY));
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