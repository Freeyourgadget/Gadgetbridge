package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusic;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicViewHolder> {
    
    public interface onItemAction {
        void onItemClick(View view, GBDeviceMusic music);
        boolean onItemLongClick(View view, GBDeviceMusic music);
    }

    private final List<GBDeviceMusic> musicList;
    private final onItemAction callback;

    public MusicListAdapter(List<GBDeviceMusic> list, onItemAction callback) {
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

    @Override
    public void onBindViewHolder(final MusicListAdapter.MusicViewHolder holder, int position) {
        final GBDeviceMusic music = musicList.get(position);

        holder.musicTitle.setText(music.getTitle());
        holder.musicArtist.setText(music.getArtist());

        if(callback != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onItemClick(view, music);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return callback.onItemLongClick(view, music);
                }
            });
        }

    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        final TextView musicArtist;
        final TextView musicTitle;

        MusicViewHolder(View itemView) {
            super(itemView);
            musicArtist = itemView.findViewById(R.id.item_details);
            musicTitle = itemView.findViewById(R.id.item_name);
        }

    }
}
