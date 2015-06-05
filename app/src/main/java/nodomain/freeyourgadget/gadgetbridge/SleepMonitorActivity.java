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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class SleepMonitorActivity extends Activity implements SurfaceHolder.Callback {
    public static final String ACTION_REFRESH
            = "nodomain.freeyourgadget.gadgetbride.sleepmonitor.action.refresh";
    private static final Logger LOG = LoggerFactory.getLogger(SleepMonitorActivity.class);

    private SurfaceView surfaceView;
    private TextView textView;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;
    private GBDevice mGBDevice = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(ACTION_REFRESH)) {
                mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
                mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
                mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
                mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
                refresh();
            }
        }
    };

    private void refresh() {
        if (mGBDevice == null) {
            return;
        }

        if (mTimestampFrom == -1) {
            Long ts = System.currentTimeMillis();
            mTimestampFrom = (int) ((ts / 1000) - (24 * 60 * 60) & 0xffffffff); // -24 hours
        }

        byte provider = -1;
        switch (mGBDevice.getType()) {
            case MIBAND:
                provider = GBActivitySample.PROVIDER_MIBAND;
                break;
            case PEBBLE:
                provider = GBActivitySample.PROVIDER_PEBBLE_MORPHEUZ; // FIXME
                break;
        }
        ArrayList<GBActivitySample> samples = GBApplication.getActivityDatabaseHandler().getGBActivitySamples(mTimestampFrom, -1, provider);
        Calendar cal = Calendar.getInstance();
        Date date;
        String dateStringFrom = "";
        String dateStringTo = "";

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        LOG.info("number of samples:" + samples.size());
        if (surfaceHolder.getSurface().isValid() && samples.size() > 1) {
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
            float movement_divisor;
            boolean annotate;
            boolean use_steps_as_movement;
            switch (provider) {
                case GBActivitySample.PROVIDER_MIBAND:
                    movement_divisor = 256.0f;
                    annotate = false; // sample density to high?
                    use_steps_as_movement = true;
                    break;
                default: // Morpheuz
                    movement_divisor = 5000.0f;
                    annotate = true;
                    use_steps_as_movement = false;
                    break;
            }

            byte last_type = GBActivitySample.TYPE_UNKNOWN;

            for (int i = 0; i < samples.size(); i++) {
                GBActivitySample sample = samples.get(i);
                byte type = sample.getType();

                if (i == 0) {
                    cal.setTimeInMillis((long) sample.getTimestamp() * 1000L);
                    date = cal.getTime();
                    dateStringFrom = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(date);
                } else if (i == samples.size() - 1) {
                    cal.setTimeInMillis((long) sample.getTimestamp() * 1000L);
                    date = cal.getTime();
                    dateStringTo = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(date);
                }

                short movement = sample.getIntensity();
                r.left = r.right;
                r.right = (float) (i + 1) / (samples.size()) * width;
                if (type == GBActivitySample.TYPE_DEEP_SLEEP) {
                    paint.setColor(Color.BLUE);
                    r.top = 0.98f * height;
                } else {
                    if (type == GBActivitySample.TYPE_LIGHT_SLEEP) {
                        paint.setColor(Color.CYAN);
                    } else {
                        if (use_steps_as_movement) {
                            movement = sample.getSteps();
                        }
                        paint.setColor(Color.YELLOW);
                    }
                    r.top = (1.0f - (float) movement / movement_divisor) * height;
                }

                canvas.drawRect(r, paint);
                boolean annotate_this = false;
                if (annotate) {
                    if (type != GBActivitySample.TYPE_DEEP_SLEEP && type != GBActivitySample.TYPE_LIGHT_SLEEP &&
                            (last_type == GBActivitySample.TYPE_DEEP_SLEEP || last_type == GBActivitySample.TYPE_LIGHT_SLEEP)) {
                        // seems that we woke up
                        annotate_this = true;
                    }
                    if (annotate_this) {
                        cal.setTimeInMillis((long) (sample.getTimestamp()) * 1000L);
                        date = cal.getTime();
                        String dateString = new SimpleDateFormat("HH:mm").format(date);
                        paint.setColor(Color.WHITE);
                        canvas.drawText(dateString, r.left - 20, r.top - 20, paint);
                    }
                    last_type = type;
                }
            }
            textView.setText(dateStringFrom + " to " + dateStringTo);

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable("device");
        }

        setContentView(R.layout.activity_sleepmonitor);

        textView = (TextView) findViewById(R.id.textView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(ACTION_REFRESH);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        surfaceView.getHolder().addCallback(this);
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        refresh();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refresh();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
