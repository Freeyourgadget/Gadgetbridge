package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.adapter.ActivitySummariesAdapter;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ActivitySummariesActivity extends AbstractListActivity<BaseActivitySummary> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setItemAdapter(new ActivitySummariesAdapter(this));

        getItemListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = getItemAdapter().getItem(position);
                if (item != null) {
                    ActivitySummary summary = (ActivitySummary) item;
                    String gpxTrack = summary.getGpxTrack();
                    if (gpxTrack != null) {
                        showTrack(gpxTrack);
                    }
                }
            }
        });


        getItemListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, final ContextMenu.ContextMenuInfo menuInfo) {
                MenuItem delete = menu.add("Delete");
                delete.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                        deleteItemAt(info.position);
                        return true;
                    }
                });
            }
        });

        getItemListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return getItemListView().showContextMenu();
            }
        });
    }

    private void deleteItemAt(int position) {
        BaseActivitySummary item = getItemAdapter().getItem(position);
        if (item != null) {
            item.delete();
            getItemAdapter().remove(item);
            refresh();
        }
    }

    private void showTrack(String gpxTrack) {
        try {
            AndroidUtils.viewFile(gpxTrack, Intent.ACTION_VIEW, this);
        } catch (IOException e) {
            GB.toast(this, "Unable to display GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }
}
