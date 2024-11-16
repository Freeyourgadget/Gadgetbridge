package nodomain.freeyourgadget.gadgetbridge.activities.musicmanager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.MusicListAdapter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusic;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusicPlaylist;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GridAutoFitLayoutManager;

public class MusicManagerActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(MusicManagerActivity.class);

    public static final String ACTION_MUSIC_DATA
            = "nodomain.freeyourgadget.gadgetbridge.musicmanager.action.music_data";
    public static final String ACTION_MUSIC_UPDATE
            = "nodomain.freeyourgadget.gadgetbridge.musicmanager.action.music_update";

    protected GBDevice mGBDevice = null;

    private View loadingView = null;
    private TextView musicDeviceInfo = null;

    private final List<GBDeviceMusic> allMusic = new ArrayList<>();

    private final List<GBDeviceMusic> musicList = new ArrayList<>();
    private MusicListAdapter musicAdapter;

    private final List<GBDeviceMusicPlaylist> playlists = new ArrayList<>();
    private ArrayAdapter<GBDeviceMusicPlaylist> playlistAdapter;

    private View playlistSpinnerLayout;
    private Spinner playlistsSpinner;

    private FloatingActionButton fabMusicUpload;
    private FloatingActionButton fabMusicPlaylistAdd;

    private int maxMusicCount = 0;
    private int maxPlaylistCount = 0;
    
    public GBDevice getGBDevice() {
        return mGBDevice;
    }

    Handler loadingTimeout = new Handler();
    Runnable loadingRunnable = new Runnable() {
        @Override
        public void run() {
            GB.toast(getString(R.string.music_error), Toast.LENGTH_SHORT, GB.ERROR);
            stopLoading();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_musicmanager);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        if (mGBDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        fabMusicUpload = findViewById(R.id.fab_music_upload);
        assert fabMusicUpload != null;
        fabMusicUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                openAudioActivityResultLauncher.launch(intent);
            }
        });

        fabMusicPlaylistAdd = findViewById(R.id.fab_music_playlist_add);
        assert fabMusicPlaylistAdd != null;
        fabMusicPlaylistAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMusicPlaylist();
            }
        });

        hideActionButtons();

        RecyclerView musicListView = findViewById(R.id.music_songs_list);
        loadingView = findViewById(R.id.music_loading);

        musicDeviceInfo = findViewById(R.id.music_device_info);

        musicListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    hideActionButtons();
                } else if (dy < 0) {
                    showActionButtons();
                }
            }
        });
        musicListView.setLayoutManager(new GridAutoFitLayoutManager(this, 300));

        musicAdapter = new MusicListAdapter(
                musicList,
                new MusicListAdapter.onItemAction() {
                    @Override
                    public void onItemClick(View view, GBDeviceMusic music) {
                        openPopupMenu(view, music);
                    }

                    @Override
                    public boolean onItemLongClick(View view, GBDeviceMusic music) {
                        return false;
                    }
                }
        );
        musicListView.setAdapter(musicAdapter);

        playlistSpinnerLayout = findViewById(R.id.music_playlists_layout);

        playlistsSpinner = findViewById(R.id.music_playlists);

        ImageButton renamePlaylist = findViewById(R.id.music_playlist_rename);
        assert renamePlaylist != null;
        renamePlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renameMusicPlaylist((GBDeviceMusicPlaylist) playlistsSpinner.getSelectedItem());
            }
        });

        ImageButton deletePlaylist = findViewById(R.id.music_playlist_delete);
        assert deletePlaylist != null;
        deletePlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteMusicPlaylist((GBDeviceMusicPlaylist) playlistsSpinner.getSelectedItem());
            }
        });


        playlistsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                GBDeviceMusicPlaylist item = (GBDeviceMusicPlaylist) adapterView.getItemAtPosition(i);
                if (item.getId() == 0) {
                    deletePlaylist.setVisibility(View.GONE);
                    renamePlaylist.setVisibility(View.GONE);

                } else {
                    deletePlaylist.setVisibility(View.VISIBLE);
                    renamePlaylist.setVisibility(View.VISIBLE);
                }
                updateCurrentMusicList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        playlistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, playlists);
        initPlaylists();

        playlistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playlistsSpinner.setAdapter(playlistAdapter);
    }

    private void hideActionButtons() {
        fabMusicUpload.hide();
        fabMusicPlaylistAdd.hide();

    }

    private void showActionButtons() {
        fabMusicUpload.show();
        if(maxPlaylistCount > 0) {
            fabMusicPlaylistAdd.show();
        }
    }


    private void startLoading(long timeout) {
        hideActionButtons();
        loadingView.setVisibility(View.VISIBLE);
        if(timeout > 0) {
            loadingTimeout.postDelayed(loadingRunnable, timeout);
        }
    }
    private void startLoading() {
        startLoading(4000);
    }

    private void stopLoading() {
        loadingTimeout.removeCallbacks(loadingRunnable);
        loadingView.setVisibility(View.GONE);
        showActionButtons();
    }

    private void updateCurrentMusicList() {
        GBDeviceMusicPlaylist current = (GBDeviceMusicPlaylist) playlistsSpinner.getSelectedItem();
        musicList.clear();
        if (current.getId() == 0) {
            musicList.addAll(allMusic);
        } else {
            List<GBDeviceMusic> filtered = allMusic.stream().filter(m -> current.getMusicIds().contains(m.getId())).collect(Collectors.toList());
            musicList.addAll(filtered);
        }
        musicAdapter.notifyDataSetChanged();
    }

    private void initPlaylists() {
        playlists.clear();
        playlists.add(new GBDeviceMusicPlaylist(0,this.getString(R.string.music_all_songs),null));
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MUSIC_DATA);
        filter.addAction(ACTION_MUSIC_UPDATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        // Load music data without timeout
        startLoading(0);
        GBApplication.deviceService(mGBDevice).onMusicListReq();
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onStop();
    }

    ActivityResultLauncher<Intent> openAudioActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent startIntent = new Intent(MusicManagerActivity.this, FwAppInstallerActivity.class);
                        startIntent.setAction(Intent.ACTION_VIEW);
                        startIntent.setDataAndType(result.getData().getData(), null);
                        startActivity(startIntent);
                    }
                }
            });


    public boolean openPopupMenu(View view, GBDeviceMusic music) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.musicmanager_context, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();

        if (playlists.size() <= 1) {
            menu.removeItem(R.id.musicmanager_add_to_playlist);
        }

        GBDeviceMusicPlaylist current = (GBDeviceMusicPlaylist) playlistsSpinner.getSelectedItem();
        musicList.clear();
        if (current.getId() == 0) {
            menu.removeItem(R.id.musicmanager_delete_from_playlist);
        } else {
            menu.removeItem(R.id.musicmanager_delete);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                 public boolean onMenuItemClick(MenuItem item) {
                                                     return onPopupItemSelected(item, music);
                                                 }
                                             }
        );

        popupMenu.show();
        return true;
    }

    private boolean onPopupItemSelected(final MenuItem item, final GBDeviceMusic music) {
        final int itemId = item.getItemId();
        if (itemId == R.id.musicmanager_delete || itemId == R.id.musicmanager_delete_from_playlist) {
            deleteMusicConfirm(music);
            return true;
        } else if (itemId == R.id.musicmanager_add_to_playlist) {
            addMusicSongToPlaylist(music);
            return true;
        }
        return false;
    }

    private void deleteMusicConfirm(final GBDeviceMusic music) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.Delete)
                .setMessage(this.getString(R.string.music_delete_confirm_description, music.getTitle()))
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    deleteMusicFromDevice((GBDeviceMusicPlaylist) playlistsSpinner.getSelectedItem(), music);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void addPlaylistToDevice(final String playlistName) {
        startLoading();
        GBApplication.deviceService(mGBDevice).onMusicOperation(0, -1, playlistName, null);
    }

    private void deletePlaylistFromDevice(final GBDeviceMusicPlaylist playlist) {
        startLoading();
        GBApplication.deviceService(mGBDevice).onMusicOperation(1, playlist.getId(), null, null);
    }

    private void renamePlaylistOnDevice(final GBDeviceMusicPlaylist playlist, String newPlaylistName) {
        startLoading();
        GBApplication.deviceService(mGBDevice).onMusicOperation(2, playlist.getId(), newPlaylistName, null);
    }

    private void addMusicToDevicePlaylist(GBDeviceMusicPlaylist playlist, final GBDeviceMusic music) {
        startLoading();
        ArrayList<Integer> list = new ArrayList<>();
        list.add(music.getId());
        GBApplication.deviceService(mGBDevice).onMusicOperation(3, playlist.getId(), null, list);
    }

    private void deleteMusicFromDevice(GBDeviceMusicPlaylist playlist, final GBDeviceMusic music) {
        startLoading();
        ArrayList<Integer> list = new ArrayList<>();
        list.add(music.getId());
        GBApplication.deviceService(mGBDevice).onMusicOperation(4, playlist.getId(), null, list);
    }

    private void addMusicPlaylist() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.music_new_playlist)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    String playlistName = input.getText().toString();
                    addPlaylistToDevice(playlistName);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void renameMusicPlaylist(GBDeviceMusicPlaylist playlist) {
        if(playlist.getId() == 0)
            return;
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(playlist.getName());

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.music_rename_playlist)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    String playlistName = input.getText().toString();
                    renamePlaylistOnDevice(playlist, playlistName);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteMusicPlaylist(GBDeviceMusicPlaylist playlist) {
        if(playlist.getId() == 0)
            return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.Delete)
                .setMessage(this.getString(R.string.music_delete_confirm_description, playlist.getName()))
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    deletePlaylistFromDevice(playlist);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void addMusicSongToPlaylist(final GBDeviceMusic music) {
        final Spinner dPlaylists = new Spinner(this);

        List<GBDeviceMusicPlaylist> dialogPlaylists = new ArrayList<>();
        for (GBDeviceMusicPlaylist playlist : playlists) {
            if (playlist.getId() != 0) {
                dialogPlaylists.add(playlist);
            }
        }

        ArrayAdapter<GBDeviceMusicPlaylist> dialogPlaylistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dialogPlaylists);
        dialogPlaylistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dPlaylists.setAdapter(dialogPlaylistAdapter);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        dPlaylists.setLayoutParams(params);
        container.addView(dPlaylists);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.music_add_to_playlist)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    GBDeviceMusicPlaylist playlist = (GBDeviceMusicPlaylist) dPlaylists.getSelectedItem();
                    addMusicToDevicePlaylist(playlist, music);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startSyncFromDevice(Intent intent) {
        String info = intent.getStringExtra("deviceInfo");
        if (!TextUtils.isEmpty(info)) {
            musicDeviceInfo.setText(info);
        } else {
            musicDeviceInfo.setVisibility(View.GONE);
        }

        maxMusicCount = intent.getIntExtra("maxMusicCount", 0);
        maxPlaylistCount = intent.getIntExtra("maxPlaylistCount", 0);

        // Hide playlist if device does not support it.
        playlistSpinnerLayout.setVisibility(maxPlaylistCount>0?View.VISIBLE:View.GONE);

        allMusic.clear();
        musicList.clear();
        initPlaylists();
    }

    private void musicListFromDevice(Intent intent) {
        ArrayList<GBDeviceMusic> list = (ArrayList<GBDeviceMusic>) intent.getSerializableExtra("musicList");
        if (list != null && !list.isEmpty()) {
            allMusic.addAll(list);
        }

        ArrayList<GBDeviceMusicPlaylist> devicePlaylist = (ArrayList<GBDeviceMusicPlaylist>) intent.getSerializableExtra("musicPlaylist");
        if (devicePlaylist != null && !devicePlaylist.isEmpty()) {
            playlists.addAll(devicePlaylist);
            playlistAdapter.notifyDataSetChanged();
        }
    }

    private void musicOperationResponse(Intent intent) {
        int operation = intent.getIntExtra("operation", -1);
        if (operation == 0) {
            int playlistIndex = intent.getIntExtra("playlistIndex", -1);
            String playlistName = intent.getStringExtra("playlistName");

            if (playlistIndex != -1 && !TextUtils.isEmpty(playlistName)) {
                playlists.add(new GBDeviceMusicPlaylist(playlistIndex, playlistName, new ArrayList<>()));
                playlistAdapter.notifyDataSetChanged();
            }
        } else if (operation == 1) {
            int playlistIndex = intent.getIntExtra("playlistIndex", -1);
            if (playlistIndex != -1) {
                for (Iterator<GBDeviceMusicPlaylist> iterator = playlists.iterator(); iterator.hasNext(); ) {
                    GBDeviceMusicPlaylist playlist = iterator.next();
                    if (playlist.getId() == playlistIndex) {
                        iterator.remove();
                    }
                }
                playlistAdapter.notifyDataSetChanged();
            }
        } else if (operation == 2) {
            int playlistIndex = intent.getIntExtra("playlistIndex", -1);
            String playlistName = intent.getStringExtra("playlistName");
            if (playlistIndex != -1 && !TextUtils.isEmpty(playlistName)) {
                for (GBDeviceMusicPlaylist playlist : playlists) {
                    if (playlist.getId() == playlistIndex) {
                        playlist.setName(playlistName);
                        break;
                    }
                }
                playlistAdapter.notifyDataSetChanged();
            }
        } else if (operation == 3) {
            int playlistIndex = intent.getIntExtra("playlistIndex", -1);
            ArrayList<Integer> ids = (ArrayList<Integer>) intent.getSerializableExtra("musicIds");
            if (playlistIndex != -1 && ids != null && !ids.isEmpty()) {
                for (GBDeviceMusicPlaylist playlist : playlists) {
                    if (playlist.getId() == playlistIndex) {
                        ArrayList<Integer> currentList = playlist.getMusicIds();
                        for (Integer id : ids) {
                            if (!currentList.contains(id))
                                currentList.add(id);
                        }
                        playlist.setMusicIds(currentList);
                        break;
                    }
                }
                playlistAdapter.notifyDataSetChanged();
                updateCurrentMusicList();
            }

        } else if (operation == 4) {
            ArrayList<Integer> ids = (ArrayList<Integer>) intent.getSerializableExtra("musicIds");
            int playlistIndex = intent.getIntExtra("playlistIndex", 0);
            if (ids != null && !ids.isEmpty()) {
                if (playlistIndex == 0) {
                    for (Iterator<GBDeviceMusic> iterator = musicList.iterator(); iterator.hasNext(); ) {
                        GBDeviceMusic music = iterator.next();
                        if (ids.contains(music.getId())) {
                            iterator.remove();
                        }
                    }
                    for (Iterator<GBDeviceMusic> iterator = allMusic.iterator(); iterator.hasNext(); ) {
                        GBDeviceMusic music = iterator.next();
                        if (ids.contains(music.getId())) {
                            iterator.remove();
                        }
                    }
                } else {
                    for (GBDeviceMusicPlaylist playlist : playlists) {
                        if (playlist.getId() == playlistIndex) {
                            ArrayList<Integer> currentList = playlist.getMusicIds();
                            for (Integer id : ids) {
                                currentList.remove(id);
                            }
                            playlist.setMusicIds(currentList);
                            break;
                        }
                    }
                }
                playlistAdapter.notifyDataSetChanged();
                updateCurrentMusicList();
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_MUSIC_DATA: {
                    if (!intent.hasExtra("type"))
                        break;
                    int type = intent.getIntExtra("type", -1);

                    LOG.info("UPDATE type: {}", type);
                    if (type == 1) {
                        startSyncFromDevice(intent);
                    } else if (type == 2) {
                        LOG.info("got music list or playlist from device");
                        musicListFromDevice(intent);
                    } else if (type == 10) {
                        updateCurrentMusicList();
                        stopLoading();
                    }
                    break;
                }
                case ACTION_MUSIC_UPDATE: {
                    boolean success = intent.getBooleanExtra("success", false);
                    if (intent.hasExtra("operation") && success) {
                        musicOperationResponse(intent);
                    }
                    stopLoading();
                    break;
                }
            }
        }
    };


}
