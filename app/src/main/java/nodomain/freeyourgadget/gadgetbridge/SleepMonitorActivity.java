package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class SleepMonitorActivity extends Activity {
    public static final String ACTION_REFRESH
            = "nodomain.freeyourgadget.gadgetbride.sleepmonitor.action.refresh";
    private static final Logger LOG = LoggerFactory.getLogger(SleepMonitorActivity.class);

    private SurfaceView surfaceView;
    private TextView textView;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(ACTION_REFRESH)) {
                int smartalarm_from = intent.getIntExtra("smartalarm_from", -1);
                int smartalarm_to = intent.getIntExtra("smartalarm_to", -1);
                int recording_base_timestamp = intent.getIntExtra("recording_base_timestamp", -1);
                int alarm_gone_off = intent.getIntExtra("alarm_gone_off", -1);
                short[] points = intent.getShortArrayExtra("points");

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long) recording_base_timestamp * 1000L);
                Date date = cal.getTime();
                String dateString = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(date);
                cal.setTimeInMillis((long) (recording_base_timestamp + 600 * (points.length - 1)) * 1000L);
                date = cal.getTime();
                String dateStringTo = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(date);
                textView.setText(dateString + " to " + dateStringTo);

                SurfaceHolder surfaceHolder = surfaceView.getHolder();

                if (surfaceHolder.getSurface().isValid() && points.length > 1) {
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    Canvas canvas = surfaceHolder.lockCanvas();
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(2);
                    paint.setTextSize(20);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setAntiAlias(true);
                    canvas.drawRGB(100, 100, 100);
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();

                    RectF r = new RectF(0.0f, 0.0f, 0.0f, height);
                    short last_movement = 5000;
                    for (int i = 0; i < points.length; i++) {
                        boolean annotate = false;
                        short movement = points[i];
                        r.left = r.right;
                        r.right = (float) (i + 1) / (points.length) * width;
                        r.top = (1.0f - (float) movement / 5000.0f) * height;
                        if (movement > 1000) {
                            paint.setColor(Color.RED);
                            if (last_movement <= 1000) {
                                annotate = true;
                            }
                        } else if (movement > 120) {
                            paint.setColor(Color.YELLOW);
                        } else {
                            paint.setColor(Color.GREEN);
                        }
                        canvas.drawRect(r, paint);
                        if (annotate) {
                            cal.setTimeInMillis((long) (recording_base_timestamp + 600 * i) * 1000L);
                            date = cal.getTime();
                            dateString = new SimpleDateFormat("HH:mm").format(date);
                            paint.setColor(Color.WHITE);
                            canvas.drawText(dateString, r.left - 20, r.top - 20, paint);
                        }
                        last_movement = movement;
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleepmonitor);

        textView = (TextView) findViewById(R.id.textView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(ACTION_REFRESH);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
