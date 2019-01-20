package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import nodomain.freeyourgadget.gadgetbridge.R;

public class NumberPreference extends DialogPreference {

    private NumberPicker numberPicker;

    public NumberPreference(Context context) {
        super(context, null);
        numberPicker = new NumberPicker(getContext());
    }

    @Override
    protected View onCreateDialogView() {
        return numberPicker;
    }

    public NumberPicker getNumberPicker() {
        return numberPicker;
    }

}
