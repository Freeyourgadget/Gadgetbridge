/*  Copyright (C) 2021 Arjan Schrijver

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

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;

public class CommuteActionsActivity extends AbstractGBActivity implements CommuteActionsListAdapter.ItemClickListener, DialogInterface.OnClickListener, View.OnClickListener {
    protected final List<String> actionsList = new ArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(CommuteActionsActivity.class);
    private SharedPreferences sharedPreferences;
    private ItemTouchHelper actionTouchHelper;
    private CommuteActionsListAdapter actionsListAdapter;
    static public final String CONFIG_KEY_Q_ACTIONS = "Q_ACTIONS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commute_actions);
        sharedPreferences = GBApplication.getPrefs().getPreferences();

        findViewById(R.id.actionAddFab).setOnClickListener(this);

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.actionsListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        actionsListAdapter = new CommuteActionsListAdapter(this, actionsList);
        actionsListAdapter.setClickListener(this);
        recyclerView.setAdapter(actionsListAdapter);
        refreshActions();

        // set up touch helper for reordering items
        ItemTouchHelper.Callback actionTouchHelperCallback = new ActionTouchHelperCallback(actionsListAdapter);
        actionTouchHelper = new ItemTouchHelper(actionTouchHelperCallback);
        actionTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void refreshActions() {
        JSONArray actionArray = null;
        try {
            actionArray = new JSONArray(sharedPreferences.getString(CONFIG_KEY_Q_ACTIONS, "[]"));
            actionsList.clear();
            for (int i = 0; i < actionArray.length(); i++)
                actionsList.add(actionArray.getString(i));

            actionsListAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            LOG.error("Error retrieving commute actions", e);
        }
    }

    private void putActionItems(List<String> actions) {
        JSONArray array = new JSONArray();
        for (String action : actions) array.put(action);

        sharedPreferences.edit().putString(CONFIG_KEY_Q_ACTIONS, array.toString()).apply();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.actionAddFab) {
            final EditText input = new EditText(this);
            input.setId(0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);

            new AlertDialog.Builder(this)
                    .setView(input)
                    .setNegativeButton(R.string.fossil_hr_new_action_cancel, null)
                    .setPositiveButton(R.string.ok, this)
                    .setTitle(R.string.fossil_hr_new_action)
                    .show();
        }
    }

    @Override
    public void onItemClick(View view, final int position) {
        final EditText input = new EditText(this);
        input.setId(0);
        input.setText(actionsListAdapter.getItem(position));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        new AlertDialog.Builder(this)
                .setView(input)
                .setNegativeButton(R.string.fossil_hr_edit_action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionsList.remove(position);
                        putActionItems(actionsList);
                        refreshActions();

                        LocalBroadcastManager.getInstance(CommuteActionsActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionsList.set(position, input.getText().toString());
                        putActionItems(actionsList);
                        refreshActions();

                        LocalBroadcastManager.getInstance(CommuteActionsActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
                    }
                })
                .setTitle(R.string.fossil_hr_edit_action)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        EditText actionEditText = ((AlertDialog) dialog).findViewById(0);

        String action = actionEditText.getText().toString();
        try {
            JSONArray actionArray = new JSONArray(sharedPreferences.getString(CONFIG_KEY_Q_ACTIONS, "[]"));
            actionArray.put(action);
            sharedPreferences.edit().putString(CONFIG_KEY_Q_ACTIONS, actionArray.toString()).apply();
            refreshActions();

            LocalBroadcastManager.getInstance(CommuteActionsActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
        } catch (JSONException e) {
            LOG.error("Error adding new commute action", e);
        }
    }

    public void startDragging(RecyclerView.ViewHolder viewHolder) {
        actionTouchHelper.startDrag(viewHolder);
    }

    public class ActionTouchHelperCallback extends ItemTouchHelper.Callback {

        private final CommuteActionsListAdapter actionsListAdapter;

        public ActionTouchHelperCallback(CommuteActionsListAdapter actionsListAdapter) {
            this.actionsListAdapter = actionsListAdapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            //we only support up and down movement and only for moving, not for swiping apps away
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            actionsListAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            putActionItems(actionsList);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            //nothing to do
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            putActionItems(actionsList);
            LocalBroadcastManager.getInstance(CommuteActionsActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
        }
    }
}