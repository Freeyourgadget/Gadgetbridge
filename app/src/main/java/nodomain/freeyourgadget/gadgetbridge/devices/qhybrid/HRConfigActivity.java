package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;

public class HRConfigActivity extends AbstractGBActivity implements View.OnClickListener, DialogInterface.OnClickListener, AdapterView.OnItemClickListener {
    private SharedPreferences sharedPreferences;
    private ActionListAdapter actionListAdapter;
    private ArrayList<MenuAction> menuActions = new ArrayList<>();

    static public final String CONFIG_KEY_Q_ACTIONS = "Q_ACTIONS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_hr_settings);

        findViewById(R.id.qhybrid_action_add).setOnClickListener(this);

        sharedPreferences = GBApplication.getPrefs().getPreferences();

        ListView actionListView = findViewById(R.id.qhybrid_action_list);
        actionListAdapter = new ActionListAdapter(menuActions);
        actionListView.setAdapter(actionListAdapter);
        actionListView.setOnItemClickListener(this);

        updateSettings();
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
        input.setText(((TextView) view).getText());
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

    private void putActionItems(List<MenuAction> actions){
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

    class ActionListAdapter extends ArrayAdapter<MenuAction> {
        public ActionListAdapter(@NonNull ArrayList<MenuAction> objects) {
            super(HRConfigActivity.this, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = new TextView(getContext());
            TextView view = (TextView) convertView;

            view.setText(getItem(position).getAction());
            // view.setTextColor(Color.WHITE);
            view.setTextSize(30);

            return view;
        }
    }
}
