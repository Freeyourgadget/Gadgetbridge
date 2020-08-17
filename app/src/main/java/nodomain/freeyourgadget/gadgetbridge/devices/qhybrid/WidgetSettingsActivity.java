package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomBackgroundWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomTextWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidgetElement;

public class WidgetSettingsActivity extends AbstractGBActivity {
    private CustomWidget subject;
    private WidgetElementAdapter widgetElementAdapter;

    public static final int RESULT_CODE_WIDGET_CREATED = 0;
    public static final int RESULT_CODE_WIDGET_UPDATED = 1;
    public static final int RESULT_CODE_WIDGET_DELETED = 2;
    public static final int RESULT_CODE_CANCELED = 3;

    private int resultCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qhybrid_activity_widget_settings);

        setResult(RESULT_CODE_CANCELED);

        if(getIntent().hasExtra("EXTRA_WIDGET")){
            subject = (CustomWidget) getIntent().getExtras().get("EXTRA_WIDGET");
            ((EditText) findViewById(R.id.qhybrid_widget_name)).setText(subject.getName());
            resultCode = RESULT_CODE_WIDGET_UPDATED;
        }else{
            subject = new CustomWidget("", 0, 63, "default"); // FIXME: handle force white background
            resultCode = RESULT_CODE_WIDGET_CREATED;
            findViewById(R.id.qhybrid_widget_delete).setEnabled(false);
        }

        findViewById(R.id.qhybrid_widget_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subject.setName(((EditText) findViewById(R.id.qhybrid_widget_name)).getText().toString());

                Intent resultIntent = getIntent();
                resultIntent.putExtra("EXTRA_WIDGET", WidgetSettingsActivity.this.subject);
                setResult(resultCode, resultIntent);

                finish();
            }
        });

        findViewById(R.id.qhybrid_widget_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CODE_WIDGET_DELETED, getIntent());

                finish();
            }
        });

        widgetElementAdapter = new WidgetElementAdapter(subject.getElements());
        ListView elementList = findViewById(R.id.qhybrid_widget_elements_list);
        elementList.setAdapter(widgetElementAdapter);
        elementList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showElementDialog(widgetElementAdapter.getItem(position));
            }
        });

        findViewById(R.id.qhybrid_widget_elements_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showElementDialog(null);
            }
        });
    }

    private void showElementDialog(@Nullable final CustomWidgetElement element){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(WidgetSettingsActivity.this)
                .setView(R.layout.qhybrid_element_popup_view);

        if(element == null) {
            dialogBuilder
                    .setTitle("create element")
                    .setNegativeButton("cancel", null)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(((RadioButton)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_elements_type_text)).isChecked()){
                                subject.addElement(new CustomTextWidgetElement(
                                        ((EditText)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_id)).getText().toString(),
                                        ((EditText)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_value)).getText().toString(),
                                        CustomWidgetElement.X_CENTER,
                                        ((RadioButton)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_elements_position_uppper)).isChecked() ? CustomTextWidgetElement.Y_UPPER_HALF : CustomTextWidgetElement.Y_LOWER_HALF
                                ));
                            }else{
                                subject.addElement(new CustomBackgroundWidgetElement(
                                        ((EditText)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_id)).getText().toString(),
                                        ((EditText)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_value)).getText().toString()
                                ));
                            }
                            refreshElementsList();
                        }
                    });
        }else{
            dialogBuilder
                    .setTitle("edit element")
                    .setNegativeButton("delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WidgetSettingsActivity.this.subject.getElements().remove(element);

                            refreshElementsList();
                        }
                    })
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            element.setId(((EditText)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_id)).getText().toString());
                            element.setValue(((EditText)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_value)).getText().toString());
                            element.setY(((RadioButton)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_elements_position_uppper)).isChecked() ? CustomTextWidgetElement.Y_UPPER_HALF : CustomTextWidgetElement.Y_LOWER_HALF);
                            element.setWidgetElementType(CustomWidgetElement.WidgetElementType.fromRadioButtonRessource(((RadioGroup)((AlertDialog)dialog).findViewById(R.id.qhybrid_widget_element_type)).getCheckedRadioButtonId()));

                            refreshElementsList();
                        }
                    });
        }

        AlertDialog dialog = dialogBuilder.show();


        if(element != null){
            String elementId = element.getId();
            String elementValue = element.getValue();
            CustomWidgetElement.WidgetElementType type = element.getWidgetElementType();

            ((EditText)dialog.findViewById(R.id.qhybrid_widget_element_id)).setText(elementId);
            ((EditText)dialog.findViewById(R.id.qhybrid_widget_element_value)).setText(elementValue);
            ((RadioGroup)dialog.findViewById(R.id.qhybrid_widget_element_type)).check(type.getRadioButtonResource());
            ((RadioGroup)dialog.findViewById(R.id.qhybrid_widget_element_position)).check(element.getY() == CustomWidgetElement.Y_UPPER_HALF ? R.id.qhybrid_widget_elements_position_uppper : R.id.qhybrid_widget_elements_position_lower);
        }
    }

    private void refreshElementsList(){
        this.widgetElementAdapter.notifyDataSetChanged();
    }

    class WidgetElementAdapter extends ArrayAdapter<CustomWidgetElement>{
        public WidgetElementAdapter(@NonNull List<CustomWidgetElement> objects) {
            super(WidgetSettingsActivity.this, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            RelativeLayout layout = new RelativeLayout(WidgetSettingsActivity.this);
            layout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


            TextView idView = new TextView(WidgetSettingsActivity.this);

            idView.setText(getItem(position).getId());
            // view.setTextColor(Color.WHITE);
            idView.setTextSize(25);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
            idView.setLayoutParams(params);

            TextView contentView = new TextView(WidgetSettingsActivity.this);
            contentView.setText(getItem(position).getValue());
            contentView.setTextSize(25);
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
            contentView.setLayoutParams(params);

            layout.addView(idView);
            layout.addView(contentView);

            return layout;
        }
    }
}
