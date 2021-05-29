/*  Copyright (C) 2019-2021 Andreas Shimokawa, Daniel Dakhno

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_COMMAND_UPDATE_WIDGETS;

public class HRConfigActivity extends AbstractGBActivity {
    private SharedPreferences sharedPreferences;
    private WidgetListAdapter widgetListAdapter;
    private ArrayList<CustomWidget> customWidgets = new ArrayList<>();

    SparseArray<String> widgetButtonsMapping = new SparseArray<>(4);

    private static final int REQUEST_CODE_WIDGET_EDIT = 0;
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    private static final int REQUEST_CODE_IMAGE_EDIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_hr_settings);

        sharedPreferences = GBApplication.getPrefs().getPreferences();

        initMappings();
        loadWidgetConfigs();

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

        // Disable some functions on watches with too new firmware (from official app 4.6.0 and higher)
        String fwVersion_str = GBApplication.app().getDeviceManager().getSelectedDevice().getFirmwareVersion();
        fwVersion_str = fwVersion_str.replaceFirst("^DN", "").replaceFirst("r\\.v.*", "");
        Version fwVersion = new Version(fwVersion_str);
        if (fwVersion.compareTo(new Version("1.0.2.20")) >= 0) {
            findViewById(R.id.qhybrid_widget_add).setEnabled(false);
            for (int i = 0; i < widgetButtonsMapping.size(); i++) {
                final int widgetButtonId = widgetButtonsMapping.keyAt(i);
                findViewById(widgetButtonId).setEnabled(false);
            }
            findViewById(R.id.qhybrid_set_background).setEnabled(false);
            findViewById(R.id.qhybrid_unset_background).setEnabled(false);
            GB.toast(getString(R.string.fossil_hr_warning_firmware_too_new), Toast.LENGTH_LONG, GB.INFO);
        }
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
}
