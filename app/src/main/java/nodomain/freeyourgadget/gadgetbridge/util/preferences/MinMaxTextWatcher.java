package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

import nodomain.freeyourgadget.gadgetbridge.R;

public class MinMaxTextWatcher implements TextWatcher {
    private final EditText editText;
    private final int min;
    private final int max;
    private final boolean allowEmpty;

    public MinMaxTextWatcher(final EditText editText, final int min, final int max) {
        this(editText, min, max, false);
    }

    public MinMaxTextWatcher(final EditText editText, final int min, final int max, final boolean allowEmpty) {
        this.editText = editText;
        this.min = min;
        this.max = max;
        this.allowEmpty = allowEmpty;
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        if (TextUtils.isEmpty(editable.toString()) && allowEmpty) {
            editText.getRootView().findViewById(android.R.id.button1)
                    .setEnabled(true);
            return;
        }

        try {
            final int val = Integer.parseInt(editable.toString());
            editText.getRootView().findViewById(android.R.id.button1)
                    .setEnabled(val >= min && val <= max);
            if (val < min) {
                editText.setError(editText.getContext().getString(R.string.min_val, min));
            } else if (val > max) {
                editText.setError(editText.getContext().getString(R.string.max_val, max));
            } else {
                editText.setError(null);
            }
        } catch (final NumberFormatException e) {
            editText.getRootView().findViewById(android.R.id.button1)
                    .setEnabled(false);
        }
    }
}
