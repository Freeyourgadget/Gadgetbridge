/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CameraActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(CameraActivity.class);

    public static final String intentExtraEvent = "EVENT";

    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    private ImageCapture imageCapture;

    private boolean reportClosing = true;

    public static boolean supportsCamera() {
        return GBApplication.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    public static boolean hasCameraPermission() {
        return GBApplication.getContext().checkCallingOrSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (!supportsCamera()) {
            LOG.error("No camera support");
            GB.toast(getString(R.string.toast_camera_support_required), Toast.LENGTH_SHORT, GB.ERROR);
            GBApplication.deviceService().onCameraStatusChange(GBDeviceEventCameraRemote.Event.EXCEPTION, null);
            reportClosing = false;
            finish();
            return;
        }

        if (!hasCameraPermission()) {
            LOG.info("Requesting camera permission");

            ActivityResultLauncher<String> requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) {
                                initCamera();
                            } else {
                                LOG.error("Did not receive camera permission");
                                GB.toast(getString(R.string.toast_camera_permission_required), Toast.LENGTH_SHORT, GB.ERROR);
                                GBApplication.deviceService().onCameraStatusChange(GBDeviceEventCameraRemote.Event.EXCEPTION, null);
                                reportClosing = false;
                                finish();
                            }
                        }
                    });
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        initCamera();
    }

    private void initCamera() {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();

                    PreviewView previewView = findViewById(R.id.preview);

                    Preview preview = new Preview.Builder().build();

                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK) // TODO: make setting
                            .build();

                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    imageCapture = new ImageCapture.Builder()
                            .setTargetRotation(preview.getTargetRotation())
                            .build();

                    cameraProvider.bindToLifecycle(
                            CameraActivity.this,
                            cameraSelector,
                            imageCapture,
                            preview
                    );
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (!intent.hasExtra(intentExtraEvent)) {
            this.finish();
            return;
        }

        GBDeviceEventCameraRemote.Event event = GBDeviceEventCameraRemote.intToEvent(intent.getIntExtra(intentExtraEvent, 0));

        LOG.info("Camera received event: " + event.name());

        // Nothing to do for unknown events

        if (event == GBDeviceEventCameraRemote.Event.CLOSE_CAMERA) {
            finish();
        } else if (event == GBDeviceEventCameraRemote.Event.OPEN_CAMERA) {
             GBApplication.deviceService().onCameraStatusChange(GBDeviceEventCameraRemote.Event.OPEN_CAMERA, null);
        } else if (event == GBDeviceEventCameraRemote.Event.TAKE_PICTURE) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.TITLE, "Gadgetbridge photo");
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                    getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
            ).build();
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    if (outputFileResults.getSavedUri() == null) {
                        // Shouldn't ever happen
                        GBApplication.deviceService().onCameraStatusChange(GBDeviceEventCameraRemote.Event.EXCEPTION, null);
                        return;
                    }

                    // TODO: improve feedback that the photo has been taken
                    GB.toast(
                            String.format(getString(R.string.toast_camera_photo_taken),
                                    outputFileResults.getSavedUri().getPath()),
                            Toast.LENGTH_LONG,
                            GB.INFO
                    );

                    GBApplication.deviceService().onCameraStatusChange(
                            GBDeviceEventCameraRemote.Event.TAKE_PICTURE,
                            outputFileResults.getSavedUri().getPath()
                    );
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    LOG.error("Failed to save image", exception);
                    GBApplication.deviceService().onCameraStatusChange(GBDeviceEventCameraRemote.Event.EXCEPTION, null);
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (reportClosing)
            GBApplication.deviceService().onCameraStatusChange(GBDeviceEventCameraRemote.Event.CLOSE_CAMERA, null);
    }
}
