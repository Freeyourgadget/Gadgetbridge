package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Context;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPreference extends DialogPreference {

    private long stepSize = 200;
    private String[] values;
    private NumberPicker currentNumberPicker;

    public NumberPreference(Context context, String[] values, long stepSize) {
        super(context, null);
        this.values = values;
        this.stepSize = stepSize;
    }

    @Override
    protected View onCreateDialogView() {
        NumberPicker numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(values.length - 1);
        numberPicker.setValue((int) getPersistedLong(stepSize));
        numberPicker.setDisplayedValues(values);
        currentNumberPicker = numberPicker;
        return numberPicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        persistLong((currentNumberPicker.getValue() * stepSize + stepSize));
    }

    public long getStepSize() {
        return stepSize;
    }

    public void setStepSize(long stepSize) {
        this.stepSize = stepSize;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }
}
