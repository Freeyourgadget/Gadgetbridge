package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusic;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicViewHolder> {

    public static int MAX_MULTISELECT_COUNT = 50;
    public interface onDataAction {
        void onItemClick(View view, GBDeviceMusic music);
        void onMultiSelect(int count, boolean limit);
    }

    private final List<GBDeviceMusic> musicList;
    private final onDataAction callback;

    private final List<GBDeviceMusic> selectedItems = new ArrayList<>();

    public MusicListAdapter(List<GBDeviceMusic> list, onDataAction callback) {
        this.musicList = list;
        this.callback = callback;
    }

    @Override
    public long getItemId(int position) {
        return musicList.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    @Override
    public MusicListAdapter.MusicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_musicmanager_song, parent, false);
        return new MusicViewHolder(view);
    }

    private void toggleSelection(GBDeviceMusic music) {
        if(selectedItems.size() >= MAX_MULTISELECT_COUNT) {
            callback.onMultiSelect(selectedItems.size(), true);
            return;
        }
        if(selectedItems.contains(music)) {
            selectedItems.remove(music);
        } else {
            selectedItems.add(music);
        }
        notifyItemChanged(musicList.indexOf(music));
        callback.onMultiSelect(selectedItems.size(), false);
    }

    @Override
    public void onBindViewHolder(final MusicListAdapter.MusicViewHolder holder, int position) {
        final GBDeviceMusic music = musicList.get(position);

        holder.musicTitle.setText(music.getTitle());
        holder.musicArtist.setText(music.getArtist());

        holder.icon.setSelected(selectedItems.contains(music));

        if(callback != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!selectedItems.isEmpty()) {
                        toggleSelection(music);
                    } else {
                        callback.onItemClick(view, music);
                    }
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    toggleSelection(music);
                    return true;
                }
            });
        }
    }

    public void clearSelectedItems() {
        selectedItems.clear();
        notifyDataSetChanged();
        callback.onMultiSelect(selectedItems.size(), false);
    }

    public List<GBDeviceMusic> getSelectedItems() {
        return selectedItems;
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        final TextView musicArtist;
        final TextView musicTitle;
        final ImageView icon;

        MusicViewHolder(View itemView) {
            super(itemView);
            musicArtist = itemView.findViewById(R.id.item_details);
            musicTitle = itemView.findViewById(R.id.item_name);
            icon = itemView.findViewById(R.id.item_image);
        }

    }
}
