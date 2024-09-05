package nodomain.freeyourgadget.gadgetbridge.activities.calibration;

import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandCalibrationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HandCalibrationHandler.class);

    public static final String ACTION_CALIBRATION_START = "qhybrid_command_save_calibration1";
    public static final String ACTION_CALIBRATION_END = "qhybrid_command_save_calibration2";
    public static final String ACTION_CALIBRATION_MOVE = "qhybrid_command_save_calibration3";

    private final Callback mCallback;

    public HandCalibrationHandler(final Callback callback) {
        this.mCallback = callback;
    }

    public void onGenericCommand(final Bundle bundle) {
        final String action = bundle.getString("asd");
        if (action == null) {
            LOG.warn("Got null action");
            return;
        }

        switch (action) {
            case ACTION_CALIBRATION_START: {
                mCallback.onHandCalibrationStart();
                break;
            }
            case ACTION_CALIBRATION_END: {
                //mCallback.onHandCalibrationEnd();
                break;
            }
            case ACTION_CALIBRATION_MOVE: {
                //mCallback.onHandCalibrationMove();
                break;
            }
        }
    }

    public void register() {

    }

    public interface Callback {
        void onHandCalibrationStart();
        void onHandCalibrationEnd(boolean save);
        void onHandCalibrationMove(int hand, int direction, int step);
    }
}
