package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ButtonPreference extends EditTextPreference {

    private View.OnClickListener onClickListener;
    private Button button;

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View button = view.findViewById(R.id.tasker_button);
        if (button != null) {
            button.setOnClickListener(onClickListener);
            this.button = (Button) button;
        }
    }

    public void setOnClickListener(View.OnClickListener clickListener) {
        this.onClickListener = clickListener;
    }

    public void setButtonText(int resourceId) {
        if (button != null) {
            button.setText(resourceId);
        }
    }
}
