package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * Simple {@link EditTextPreference} with an button.
 * <p>
 * Exposes only {@link Button#setOnClickListener(View.OnClickListener)} and {@link Button#setText(int)}
 */
public class ButtonPreference extends EditTextPreference {

    private View.OnClickListener onClickListener;
    private Button button;

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.button_preference_layout);
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

    /**
     * Sets an {@link View.OnClickListener} to the button.
     *
     * @param clickListener
     */
    public void setOnClickListener(View.OnClickListener clickListener) {
        this.onClickListener = clickListener;
    }

    /**
     * Set button text with resource id.
     *
     * @param resourceId {@link R.string}
     */
    public void setButtonText(int resourceId) {
        if (button != null) {
            button.setText(resourceId);
        }
    }
}
