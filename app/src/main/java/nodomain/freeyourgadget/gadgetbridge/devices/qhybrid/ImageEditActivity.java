package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImageFactory;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ImageEditActivity extends AbstractGBActivity implements View.OnTouchListener {
    static final public int RESULT_CODE_EDIT_SUCCESS = 0;

    ImageView overlay;
    Canvas overlayCanvas;
    Paint overlayPaint;
    Bitmap overlayBitmap, mainBitmap;

    float x = 0, y = 0, diameter = 0, imageDimension = 0;
    int imageWidth, imageHeight;

    private enum MovementState {
        MOVE_UPPER_LEFT,
        MOVE_LOWER_RIGHT,
        MOVE_FRAME
    }
    private MovementState movementState;
    float movementStartX, movementStartY, movementStartFrameX, movementStartFrameY, movementStartDiameter, leftUpperDeltaX, leftUpperDeltaY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_image_edit);

        final RelativeLayout mainLayout = findViewById(R.id.qhybrid_image_edit_container);
        overlay = findViewById(R.id.qhybrid_image_edit_image_overlay);
        overlay.setOnTouchListener(this);

        findViewById(R.id.qhybrid_image_edit_okay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finalizeImage();
            }
        });

        mainLayout.post(new Runnable() {
            @Override
            public void run() {
                try {
                    fitImageToLayout(mainLayout);
                } catch (IOException | RuntimeException e) {
                    GB.log("Error formatting image", GB.ERROR, e);
                    GB.toast("Error formatting image", Toast.LENGTH_LONG, GB.ERROR);
                    finish();
                }
            }
        });
    }

    private void finalizeImage(){
        Bitmap cropped = Bitmap.createBitmap(this.mainBitmap, (int) this.x, (int) this.y, (int) this.diameter, (int) this.diameter);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, 400, 400, false);
        cropped.recycle();

        try {
            AssetImage image = AssetImageFactory.createAssetImage(scaled, false, 0, 0, 0);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("EXTRA_PIXELS_ENCODED", image.getFileData());
            setResult(RESULT_CODE_EDIT_SUCCESS, resultIntent);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            scaled.recycle();
        }
    }

    private void fitImageToLayout(RelativeLayout mainLayout) throws IOException, RuntimeException {
        float containerHeight = mainLayout.getHeight();
        float containerWidth = mainLayout.getWidth();
        float containerRelation = containerHeight / containerWidth;

        Bitmap bitmap = this.createImageFromURI();
        float imageHeight = bitmap.getHeight();
        float imageWidth = bitmap.getWidth();
        float imageRelation = imageHeight / imageWidth;

        float scaleRatio;
        if(imageRelation > containerRelation){
            scaleRatio = containerHeight / imageHeight;
        }else{
            scaleRatio = containerWidth / imageWidth;
        }

        int scaledHeight = (int)(imageHeight * scaleRatio);
        int scaledWidth = (int)(imageWidth * scaleRatio);

        this.imageHeight = scaledHeight;
        this.imageWidth = scaledWidth;

        this.imageDimension = this.diameter = Math.min(scaledHeight, scaledWidth);

        mainBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false);

        ImageView mainImageView = findViewById(R.id.qhybrid_image_edit_image);
        mainImageView.setImageBitmap(mainBitmap);
        createOverlay(scaledWidth, scaledHeight);
    }

    private void createOverlay(int width, int height){
        this.overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        this.overlayCanvas = new Canvas(this.overlayBitmap);

        this.overlayPaint = new Paint();
        this.overlayPaint.setColor(Color.BLACK);
        this.overlayPaint.setStyle(Paint.Style.STROKE);
        this.overlayPaint.setStrokeWidth(imageDimension / 100);

        renderOverlay();
    }

    private Bitmap createImageFromURI() throws IOException, RuntimeException {
        Uri imageURI = getIntent().getData();
        if (imageURI == null) {
            throw new RuntimeException("no image attached");
        }

        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(imageURI, new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
        c.moveToFirst();
        int orientation = c.getInt(c.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION));
        c.close();
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
        if (orientation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    private void renderOverlay() {
        overlayCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

        overlayCanvas.drawCircle(x, y, imageDimension / 15, overlayPaint);
        overlayCanvas.drawCircle(x + diameter, y + diameter, imageDimension / 15, overlayPaint);

        overlayCanvas.drawCircle(x + diameter / 2, y + diameter / 2, diameter / 2, overlayPaint);

        overlay.setImageBitmap(overlayBitmap);
    }

    private void forceImageBoundaries(){
        this.x = Math.max(this.x, 0);
        this.y = Math.max(this.y, 0);

        this.x = Math.min(this.x, this.imageWidth - diameter);
        this.y = Math.min(this.y, this.imageHeight - diameter);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
            handleTouchDown(motionEvent);
        }else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE){
            handleTouchMove(motionEvent);
        }
        return true;
    }

    private void handleTouchMove(MotionEvent motionEvent){
        if(movementState == MovementState.MOVE_UPPER_LEFT){
            float moveDeltaX = motionEvent.getX() - this.movementStartX;
            float moveDeltaY = motionEvent.getY() - this.movementStartY;

            float mid = (moveDeltaX + moveDeltaY) / 2;

            this.diameter = this.movementStartDiameter - mid;
            this.x = this.movementStartX + mid + this.leftUpperDeltaX;
            this.y = this.movementStartY + mid + this.leftUpperDeltaY;
        }else if(movementState == MovementState.MOVE_LOWER_RIGHT) {
            float moveDeltaX = motionEvent.getX() - this.movementStartX;
            float moveDeltaY = motionEvent.getY() - this.movementStartY;

            float mid = (moveDeltaX + moveDeltaY) / 2;

            this.diameter = this.movementStartDiameter + mid;
        }else if(movementState == MovementState.MOVE_FRAME){
            this.x = this.movementStartFrameX + (motionEvent.getX() - this.movementStartX);
            this.y = this.movementStartFrameY + (motionEvent.getY() - this.movementStartY);
        }

        this.forceImageBoundaries();
        renderOverlay();
    }

    private void handleTouchDown(MotionEvent motionEvent) {
        this.movementStartX = motionEvent.getX();
        this.movementStartY = motionEvent.getY();
        this.movementStartFrameX = this.x;
        this.movementStartFrameY = this.y;
        this.movementStartDiameter = this.diameter;

        final float threshold = imageDimension / 15;

        float upperLeftDeltaX = this.x - motionEvent.getX();
        float upperLeftDeltaY = this.y - motionEvent.getY();

        float upperLeftDistance = (float) Math.sqrt(upperLeftDeltaX * upperLeftDeltaX + upperLeftDeltaY * upperLeftDeltaY);

        if(upperLeftDistance < threshold){
            // Toast.makeText(this, "upper left", 0).show();
            this.leftUpperDeltaX = upperLeftDeltaX;
            this.leftUpperDeltaY = upperLeftDeltaY;
            this.movementState = MovementState.MOVE_UPPER_LEFT;
            return;
        }

        float lowerLeftX = this.x + diameter;
        float lowerLeftY = this.y + diameter;

        float lowerRightDeltaX = lowerLeftX - motionEvent.getX();
        float lowerRightDeltaY = lowerLeftY - motionEvent.getY();

        float lowerRightDistance = (float) Math.sqrt(lowerRightDeltaX * lowerRightDeltaX + lowerRightDeltaY * lowerRightDeltaY);

        if(lowerRightDistance < threshold){
            // Toast.makeText(this, "lower right", 0).show();
            this.movementState = MovementState.MOVE_LOWER_RIGHT;
            return;
        }

        // Toast.makeText(this, "anywhere else", 0).show();
        this.movementState = MovementState.MOVE_FRAME;
    }
}
