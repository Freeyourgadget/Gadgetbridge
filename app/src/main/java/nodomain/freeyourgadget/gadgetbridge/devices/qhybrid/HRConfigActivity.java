package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomBackgroundWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomTextWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.Widget;

import static nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.WidgetSettingsActivity.RESULT_CODE_WIDGET_DELETED;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_COMMAND_UPDATE_WIDGETS;

public class HRConfigActivity extends AbstractGBActivity implements View.OnClickListener, DialogInterface.OnClickListener, AdapterView.OnItemClickListener {
    private SharedPreferences sharedPreferences;
    private ActionListAdapter actionListAdapter;
    private WidgetListAdapter widgetListAdapter;
    private ArrayList<MenuAction> menuActions = new ArrayList<>();
    private ArrayList<CustomWidget> customWidgets = new ArrayList<>();

    SparseArray<String> widgetButtonsMapping = new SparseArray<>(4);

    static public final String CONFIG_KEY_Q_ACTIONS = "Q_ACTIONS";
    private static final int REQUEST_CODE_WIDGET_EDIT = 0;
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    private static final int REQUEST_CODE_IMAGE_EDIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_hr_settings);

        findViewById(R.id.qhybrid_action_add).setOnClickListener(this);

        sharedPreferences = GBApplication.getPrefs().getPreferences();

        initMappings();
        loadWidgetConfigs();


        ListView actionListView = findViewById(R.id.qhybrid_action_list);
        actionListAdapter = new ActionListAdapter(menuActions);
        actionListView.setAdapter(actionListAdapter);
        actionListView.setOnItemClickListener(this);

        final ListView widgetListView = findViewById(R.id.qhybrid_widget_list);
        widgetListAdapter = new WidgetListAdapter(customWidgets);
        widgetListView.setAdapter(widgetListAdapter);
        widgetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Widget widget = widgetListAdapter.getItem(position);

                Intent startIntent = new Intent(HRConfigActivity.this, WidgetSettingsActivity.class);
                startIntent.putExtra("EXTRA_WIDGET", widget);
                startIntent.putExtra("EXTRA_WIDGET_IDNEX", position);
                startIntent.putExtra("EXTRA_WIDGET_INITIAL_NAME", ((CustomWidget) widget).getName());

                startActivityForResult(startIntent, REQUEST_CODE_WIDGET_EDIT);
            }
        });
        loadCustomWidgetList();

        findViewById(R.id.qhybrid_widget_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(HRConfigActivity.this, WidgetSettingsActivity.class);

                startActivityForResult(startIntent, REQUEST_CODE_WIDGET_EDIT);
            }
        });

        findViewById(R.id.qhybrid_set_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(HRConfigActivity.this)
                        .setTitle("whoop whoop")
                        .setMessage("background has to be pushed every time a custom widget changes, causing traffic and battery drain. Consider that when using custom widgets.")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent pickIntent = new Intent(Intent.ACTION_PICK);
                                pickIntent.setType("image/*");

                                startActivityForResult(pickIntent, REQUEST_CODE_IMAGE_PICK);
                            }
                        })
                        .setNegativeButton("nah", null)
                        .show();
            }
        });

        findViewById(R.id.qhybrid_unset_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_SET_BACKGROUND_IMAGE);
                intent.putIntegerArrayListExtra("EXTRA_PIXELS", null);
                LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(intent);
            }
        });

        for (int i = 0; i < widgetButtonsMapping.size(); i++) {
            final int widgetButtonId = widgetButtonsMapping.keyAt(i);
            findViewById(widgetButtonId).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Widget.WidgetType[] types = Widget.WidgetType.values();
                    final ArrayList<String> names = new ArrayList<>(types.length);

                    for (Widget.WidgetType type : types) {
                        names.add(getResources().getString(type.getStringResource()));
                    }

                    for(CustomWidget customWidget : customWidgets){
                        names.add(customWidget.getName());
                    }

                    final String[] nameStrings = names.toArray(new String[0]);
                    new AlertDialog.Builder(HRConfigActivity.this)
                            .setItems(
                                    nameStrings,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            saveWidgetSetting(widgetButtonId, which, nameStrings);
                                        }
                                    }
                            )
                            .show();

                }
            });
        }

        updateSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null) return;
        if(requestCode == REQUEST_CODE_WIDGET_EDIT) {
            if (resultCode == WidgetSettingsActivity.RESULT_CODE_WIDGET_CREATED) {
                CustomWidget widget = (CustomWidget) data.getExtras().get("EXTRA_WIDGET");
                this.customWidgets.add(widget);
                refreshWidgetList();
                saveCustomWidgetList();

                LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHYBRID_COMMAND_UPDATE_WIDGETS));
            } else if (resultCode == WidgetSettingsActivity.RESULT_CODE_WIDGET_UPDATED) {
                CustomWidget widget = (CustomWidget) data.getExtras().get("EXTRA_WIDGET");
                int updateIndex = data.getIntExtra("EXTRA_WIDGET_IDNEX", -1);

                String initialName = data.getStringExtra("EXTRA_WIDGET_INITIAL_NAME");
                String newName = widget.getName();

                String widgetJSON = sharedPreferences.getString("FOSSIL_HR_WIDGETS", "{}");
                widgetJSON = widgetJSON.replace("custom_" + initialName, "custom_" + newName);
                sharedPreferences.edit().putString("FOSSIL_HR_WIDGETS", widgetJSON).apply();

                this.customWidgets.set(updateIndex, widget);

                loadWidgetConfigs();
                refreshWidgetList();
                saveCustomWidgetList();

                LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHYBRID_COMMAND_UPDATE_WIDGETS));
            } else if (resultCode == WidgetSettingsActivity.RESULT_CODE_WIDGET_DELETED) {
                int updateIndex = data.getIntExtra("EXTRA_WIDGET_IDNEX", -1);

                this.customWidgets.remove(updateIndex);

                refreshWidgetList();
                saveCustomWidgetList();

                LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHYBRID_COMMAND_UPDATE_WIDGETS));
            }
        }else if(requestCode == REQUEST_CODE_IMAGE_PICK){
            if (resultCode == RESULT_OK)
            {
                Uri imageUri = data.getData();
                Intent activityIntent = new Intent();
                activityIntent.setClass(this, ImageEditActivity.class);
                activityIntent.setData(imageUri);

                startActivityForResult(activityIntent, REQUEST_CODE_IMAGE_EDIT);
            }
        }else if(requestCode == REQUEST_CODE_IMAGE_EDIT){
            if(resultCode == ImageEditActivity.RESULT_CODE_EDIT_SUCCESS){
                data.setAction(QHybridSupport.QHYBRID_COMMAND_SET_BACKGROUND_IMAGE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(data);
            }
        }
    }

    private void saveCustomWidgetList() {
        try {
            JSONArray widgetArray = new JSONArray();
            for(CustomWidget widget : customWidgets){
                JSONArray elementArray = new JSONArray();

                for(CustomWidgetElement element : widget.getElements()){
                    JSONObject elementObject = new JSONObject();
                    elementObject
                            .put("type", element.getWidgetElementType().getJsonIdentifier())
                            .put("id", element.getId())
                            .put("value", element.getValue())
                            .put("x", element.getX())
                            .put("y", element.getY());
                    elementArray.put(elementObject);
                }

                JSONObject widgetObject = new JSONObject();
                widgetObject
                        .put("name", widget.getName())
                        .put("elements", elementArray);

                widgetArray.put(widgetObject);
            }
            sharedPreferences.edit().putString("QHYBRID_CUSTOM_WIDGETS", widgetArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadCustomWidgetList() {
        String customWidgetJson = sharedPreferences.getString("QHYBRID_CUSTOM_WIDGETS", "[]");

        try {
            JSONArray customWidgets = new JSONArray(customWidgetJson);
            this.customWidgets.clear();

            for (int i = 0; i < customWidgets.length(); i++) {
                JSONObject customWidgetObject = customWidgets.getJSONObject(i);
                CustomWidget widget = new CustomWidget(
                        customWidgetObject.getString("name"), 0, 0, "default" // FIXME: handle force white background
                );
                JSONArray elements = customWidgetObject.getJSONArray("elements");

                for (int i2 = 0; i2 < elements.length(); i2++) {
                    JSONObject element = elements.getJSONObject(i2);
                    if (element.getString("type").equals("text")) {
                        widget.addElement(new CustomTextWidgetElement(
                                element.getString("id"),
                                element.getString("value"),
                                element.getInt("x"),
                                element.getInt("y")
                        ));
                    } else if (element.getString("type").equals("background")) {
                        widget.addElement(new CustomBackgroundWidgetElement(
                                element.getString("id"),
                                element.getString("value")
                        ));
                    }
                }

                this.customWidgets.add(widget);
            }

            refreshWidgetList();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void refreshWidgetList() {
        widgetListAdapter.notifyDataSetChanged();
    }

    private void saveWidgetSetting(int buttonId, int option, String[] names) {
        String jsonKey = widgetButtonsMapping.get(buttonId);
        Widget.WidgetType[] types = Widget.WidgetType.values();
        String identifier = null;
        if(option < types.length){
            Widget.WidgetType type = types[option];
            identifier = type.getIdentifier();
        }else{
            identifier = "custom_" + names[option];
        }

        try {
            JSONObject keyConfig = new JSONObject(sharedPreferences.getString("FOSSIL_HR_WIDGETS", "{}"));
            if (identifier != null) {
                keyConfig.put(jsonKey, identifier);
            } else {
                keyConfig.remove(jsonKey);
            }
            sharedPreferences.edit().putString("FOSSIL_HR_WIDGETS", keyConfig.toString()).apply();
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(QHYBRID_COMMAND_UPDATE_WIDGETS));

            loadWidgetConfigs();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void loadWidgetConfigs() {
        try {
            for (int i = 0; i < widgetButtonsMapping.size(); i++) {
                ((TextView) findViewById(widgetButtonsMapping.keyAt(i))).setText(widgetButtonsMapping.valueAt(i) + " widget");
            }

            JSONObject keyConfig = new JSONObject(sharedPreferences.getString("FOSSIL_HR_WIDGETS", "{}"));
            Iterator<String> keyIterator = keyConfig.keys();

            loop:
            while (keyIterator.hasNext()) {
                String position = keyIterator.next();

                for (int widgetButtonIndex = 0; widgetButtonIndex < widgetButtonsMapping.size(); widgetButtonIndex++) {
                    if (position.equals(widgetButtonsMapping.valueAt(widgetButtonIndex))) {
                        int buttonId = widgetButtonsMapping.keyAt(widgetButtonIndex);
                        String function = keyConfig.getString(position);

                        Widget.WidgetType[] types = Widget.WidgetType.values();
                        if(function.startsWith("custom_")){
                            ((TextView) findViewById(buttonId)).setText(
                                    position + " widget: " + function.substring(7)
                            );
                            continue loop;
                        }
                        for (int widgetIdIndex = 0; widgetIdIndex < types.length; widgetIdIndex++) {
                            String widgetIdMappingValue = types[widgetIdIndex].getIdentifier();
                            if (widgetIdMappingValue != null && widgetIdMappingValue.equals(function)) {
                                ((TextView) findViewById(buttonId)).setText(
                                        position + " widget: "
                                                + getResources().getText(
                                                types[widgetIdIndex].getStringResource()
                                        )
                                );
                                break;
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initMappings() {
        widgetButtonsMapping.put(R.id.qhybrid_button_widget_top, "top");
        widgetButtonsMapping.put(R.id.qhybrid_button_widget_right, "right");
        widgetButtonsMapping.put(R.id.qhybrid_button_widget_bottom, "bottom");
        widgetButtonsMapping.put(R.id.qhybrid_button_widget_left, "left");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.qhybrid_action_add) {
            final EditText input = new EditText(this);
            input.setId(0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);

            new AlertDialog.Builder(this)
                    .setView(input)
                    .setNegativeButton("cancel", null)
                    .setPositiveButton("ok", this)
                    .setTitle("create action")
                    .show();
        }
    }

    private void updateSettings() {
        JSONArray actionArray = null;
        try {
            actionArray = new JSONArray(sharedPreferences.getString(CONFIG_KEY_Q_ACTIONS, "[]"));
            menuActions.clear();
            for (int i = 0; i < actionArray.length(); i++)
                menuActions.add(new MenuAction(actionArray.getString(i)));

            actionListAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        EditText actionEditText = ((AlertDialog) dialog).findViewById(0);

        String action = actionEditText.getText().toString();
        try {
            JSONArray actionArray = new JSONArray(sharedPreferences.getString(CONFIG_KEY_Q_ACTIONS, "[]"));
            actionArray.put(action);
            sharedPreferences.edit().putString(CONFIG_KEY_Q_ACTIONS, actionArray.toString()).apply();
            updateSettings();

            LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        final EditText input = new EditText(this);
        input.setId(0);
        TextView subject = findViewById(0);
        input.setText(subject.getText());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        new AlertDialog.Builder(this)
                .setView(input)
                .setNegativeButton("delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        menuActions.remove(position);
                        putActionItems(menuActions);
                        updateSettings();

                        LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        menuActions.get(position).setAction(input.getText().toString());
                        putActionItems(menuActions);
                        updateSettings();

                        LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
                    }
                })
                .setTitle("edit action")
                .show();
    }

    private void moveActionUp(int position){
        this.menuActions.add(position - 1, this.menuActions.remove(position));
        this.actionListAdapter.notifyDataSetChanged();
        putActionItems(menuActions);

        LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
    }

    private void moveActionDown(int position){
        this.menuActions.add(position + 1, this.menuActions.remove(position));
        this.actionListAdapter.notifyDataSetChanged();
        putActionItems(menuActions);

        LocalBroadcastManager.getInstance(HRConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
    }

    private void putActionItems(List<MenuAction> actions) {
        JSONArray array = new JSONArray();
        for (MenuAction action : actions) array.put(action.getAction());

        sharedPreferences.edit().putString(CONFIG_KEY_Q_ACTIONS, array.toString()).apply();
    }

    class MenuAction {
        private String action;

        public MenuAction(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    class WidgetListAdapter extends ArrayAdapter<CustomWidget> {
        public WidgetListAdapter(@NonNull List<CustomWidget> objects) {
            super(HRConfigActivity.this, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = new TextView(getContext());
            TextView view = (TextView) convertView;

            view.setText(getItem(position).getName());
            // view.setTextColor(Color.WHITE);
            view.setTextSize(25);

            return view;
        }
    }

    class ActionListAdapter extends ArrayAdapter<MenuAction> {
        public ActionListAdapter(@NonNull ArrayList<MenuAction> objects) {
            super(HRConfigActivity.this, 0, objects);
        }

        @SuppressLint("ResourceType")
        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            RelativeLayout layout = new RelativeLayout(getContext());

            TextView text = new TextView(getContext());
            text.setId(0);

            text.setText(getItem(position).getAction());
            // view.setTextColor(Color.WHITE);
            text.setTextSize(25);
            RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
            layout.addView(text);

            try {
                getItem(position + 1);
                ImageView downView = new ImageView(getContext());
                downView.setImageResource(R.drawable.ic_arrow_upward);
                downView.setRotation(180);
                RelativeLayout.LayoutParams downParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                downParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                downView.setLayoutParams(downParams);
                downView.setId(2);
                downView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveActionDown(position);
                    }
                });
                layout.addView(downView);
            }catch (IndexOutOfBoundsException e){
                // no following item
            }

            if (position != 0) {
                ImageView upView = new ImageView(getContext());
                upView.setImageResource(R.drawable.ic_arrow_upward);
                RelativeLayout.LayoutParams upParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                upParams.setMarginEnd(100);
                upParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                upView.setLayoutParams(upParams);
                upView.setId(1);
                upView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveActionUp(position);
                    }
                });
                layout.addView(upView);
            }

            return layout;
        }
    }
}
