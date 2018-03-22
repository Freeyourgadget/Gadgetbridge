package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractItemAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.ItemWithDetailsAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class AbstractListActivity<T> extends AbstractGBActivity {
    private AbstractItemAdapter<T> itemAdapter;
    private ListView itemListView;

    public void setItemAdapter(AbstractItemAdapter<T> itemAdapter) {
        this.itemAdapter = itemAdapter;
        itemListView.setAdapter(itemAdapter);
    }

    protected void refresh() {
        this.itemAdapter.loadItems();
    }

    public AbstractItemAdapter<T> getItemAdapter() {
        return itemAdapter;
    }

    public ListView getItemListView() {
        return itemListView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);
        itemListView = (ListView) findViewById(R.id.itemListView);
    }
}
