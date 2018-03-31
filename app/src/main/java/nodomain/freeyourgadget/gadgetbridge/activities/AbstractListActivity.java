package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.widget.ListView;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractItemAdapter;

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
        itemListView = findViewById(R.id.itemListView);
    }
}
