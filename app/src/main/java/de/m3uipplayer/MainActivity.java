package de.m3uipplayer;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UnstableApi
public class MainActivity extends AppCompatActivity {
    private static final int PICK_PLAYLIST = 41;
    private static final String PREFERENCES = "m3u_player";
    private static final String FAVORITE_URLS = "favorite_urls";
    private static final String PLAYER_MODE = "player_mode";
    private static final String PLAYLIST_IDS = "playlist_ids";
    private static final String ACTIVE_PLAYLIST_ID = "active_playlist_id";
    private static final String PLAYLIST_NAME_PREFIX = "playlist_name_";
    private final List<Channel> channels = new ArrayList<>();
    private final List<String> playlistIds = new ArrayList<>();
    private final Set<String> favoriteUrls = new HashSet<>();
    private ChannelAdapter adapter;
    private ExoPlayer player;
    private boolean playerVisible;
    private boolean favoritesOnly;
    private String filterQuery = "";
    private String selectedGroup;
    private TextView status;
    private TextView playlistName;
    private Button openPlaylistButton;
    private Button favoritesButton;
    private Button deletePlaylistButton;
    private Spinner groupFilter;
    private Spinner playerModeSelector;
    private Spinner playlistSelector;
    private String activePlaylistId;
    private boolean updatingPlaylistSelector;
    private Channel currentChannel;
    private static final Pattern GROUP = Pattern.compile("group-title\\s*=\\s*\\\"([^\\\"]*)\\\"", Pattern.CASE_INSENSITIVE);

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        Set<String> storedFavorites = preferences.getStringSet(FAVORITE_URLS, new HashSet<>());
        if (storedFavorites != null) favoriteUrls.addAll(storedFavorites);
        showLibrary();
        restoreActivePlaylist();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (playerVisible) { stopPlayer(); showLibrary(); }
                else finish();
            }
        });
    }

    private void showLibrary() {
        playerVisible = false;
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        playlistName = findViewById(R.id.playlist_name);
        adapter = new ChannelAdapter(this::playChannel, this::toggleFavorite);
        RecyclerView list = findViewById(R.id.channel_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        openPlaylistButton = findViewById(R.id.open_button);
        openPlaylistButton.setOnClickListener(v -> pickPlaylist());
        openPlaylistButton.requestFocus();
        playlistSelector = findViewById(R.id.playlist_selector);
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        activePlaylistId = preferences.getString(ACTIVE_PLAYLIST_ID, null);
        refreshPlaylistSelector();
        playlistSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (updatingPlaylistSelector || position >= playlistIds.size()) return;
                String selectedId = playlistIds.get(position);
                if (!selectedId.equals(activePlaylistId)) {
                    activePlaylistId = selectedId;
                    getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                            .putString(ACTIVE_PLAYLIST_ID, selectedId).apply();
                    loadSavedPlaylist(selectedId);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        deletePlaylistButton = findViewById(R.id.delete_playlist_button);
        deletePlaylistButton.setOnClickListener(v -> confirmDeletePlaylist());
        updateDeletePlaylistButton();
        favoritesButton = findViewById(R.id.favorites_button);
        favoritesButton.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            updateFavoritesButton();
            applyFilters();
        });
        groupFilter = findViewById(R.id.group_filter);
        playerModeSelector = findViewById(R.id.player_mode_selector);
        configureFilters();
        configurePlayerModeSelector();
        adapter.setFavorites(favoriteUrls);
        if (!channels.isEmpty()) adapter.replace(ChannelFilter.filter(channels, filterQuery, selectedGroup, favoriteUrls, favoritesOnly));
    }

    private void configureFilters() {
        EditText search = findViewById(R.id.channel_search);
        search.setText(filterQuery);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuery = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        groupFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroup = position == 0 ? null : parent.getItemAtPosition(position).toString();
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        updateGroupOptions();
        updateFavoritesButton();
    }

    private void configurePlayerModeSelector() {
        String[] modes = {
                ExternalPlayerSupport.EXOPLAYER,
                ExternalPlayerSupport.VLC,
                ExternalPlayerSupport.MX_PLAYER
        };
        ArrayAdapter<CharSequence> playerAdapter = ArrayAdapter.createFromResource(
                this, R.array.player_modes, android.R.layout.simple_spinner_item);
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerModeSelector.setAdapter(playerAdapter);
        String storedMode = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getString(PLAYER_MODE, ExternalPlayerSupport.EXOPLAYER);
        int selectedPosition = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(storedMode)) selectedPosition = i;
        }
        playerModeSelector.setSelection(selectedPosition, false);
        playerModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < modes.length) {
                    getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                            .putString(PLAYER_MODE, modes[position]).apply();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void updateGroupOptions() {
        List<String> groups = new ArrayList<>();
        groups.add(getString(R.string.all_groups));
        Set<String> uniqueGroups = new LinkedHashSet<>();
        for (Channel channel : channels) uniqueGroups.add(channel.group);
        groups.addAll(uniqueGroups);
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groups);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupFilter.setAdapter(groupAdapter);
        int selectedPosition = selectedGroup == null ? 0 : groups.indexOf(selectedGroup);
        if (selectedPosition < 0) {
            selectedGroup = null;
            selectedPosition = 0;
        }
        groupFilter.setSelection(selectedPosition, false);
    }

    private void updateFavoritesButton() {
        if (favoritesButton != null) {
            favoritesButton.setText(favoritesOnly ? R.string.show_all_channels : R.string.show_favorites);
        }
    }

    private void applyFilters() {
        if (adapter != null) {
            adapter.replace(ChannelFilter.filter(channels, filterQuery, selectedGroup, favoriteUrls, favoritesOnly));
        }
    }

    private void toggleFavorite(Channel channel) {
        FavoriteChannels.toggle(favoriteUrls, channel.url);
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putStringSet(FAVORITE_URLS, new HashSet<>(favoriteUrls))
                .apply();
        adapter.setFavorites(favoriteUrls);
        applyFilters();
    }

    private void pickPlaylist() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Some USB file providers expose M3U files as application/octet-stream.
        // Let the system picker show them, while the app validates the contents.
        intent.setType("*/*");
        startActivityForResult(intent, PICK_PLAYLIST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PLAYLIST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        loadPlaylist(data.getData());
    }

    private void loadPlaylist(Uri uri) {
        status.setText("Playlist wird gelesen …");
        openPlaylistButton.setEnabled(false);
        new Thread(() -> {
            File staged = null;
            String id = UUID.randomUUID().toString();
            File saved = playlistFile(id);
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                staged = PlaylistStorage.stage(input, saved);
                List<Channel> loaded = parse(staged);
                PlaylistStorage.commit(staged, saved);
                String name = uri.getLastPathSegment() == null ? "USB-Playlist" : uri.getLastPathSegment();
                savePlaylistReference(id, name);
                runOnUiThread(() -> {
                    activePlaylistId = id;
                    refreshPlaylistSelector();
                    showPlaylist(loaded, name);
                    openPlaylistButton.setEnabled(true);
                });
            } catch (Exception e) {
                if (staged != null) staged.delete();
                if (saved.exists()) saved.delete();
                runOnUiThread(() -> {
                    status.setText(R.string.playlist_load_failed);
                    Toast.makeText(this, R.string.playlist_load_failed, Toast.LENGTH_LONG).show();
                    openPlaylistButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void restoreActivePlaylist() {
        refreshPlaylistSelector();
        if (activePlaylistId == null) {
            clearPlaylistView();
            return;
        }
        loadSavedPlaylist(activePlaylistId);
    }

    private void loadSavedPlaylist(String id) {
        File saved = playlistFile(id);
        if (!saved.isFile()) {
            Toast.makeText(this, R.string.saved_playlist_load_failed, Toast.LENGTH_LONG).show();
            return;
        }
        status.setText(R.string.playlist_restoring);
        String name = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getString(PLAYLIST_NAME_PREFIX + id, getString(R.string.saved_playlist));
        new Thread(() -> {
            try {
                List<Channel> loaded = parse(saved);
                runOnUiThread(() -> {
                    if (id.equals(activePlaylistId)) showPlaylist(loaded, name);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (id.equals(activePlaylistId)) {
                        status.setText(R.string.saved_playlist_load_failed);
                        Toast.makeText(this, R.string.saved_playlist_load_failed, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private File playlistFile(String id) {
        return new File(getFilesDir(), "playlist_" + id + ".m3u");
    }

    private void savePlaylistReference(String id, String name) throws IOException {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        Set<String> ids = new HashSet<>(preferences.getStringSet(PLAYLIST_IDS, new HashSet<>()));
        ids.add(id);
        if (!preferences.edit()
                .putStringSet(PLAYLIST_IDS, ids)
                .putString(PLAYLIST_NAME_PREFIX + id, name)
                .putString(ACTIVE_PLAYLIST_ID, id)
                .commit()) {
            throw new IOException("Playlist-Auswahl konnte nicht gespeichert werden");
        }
    }

    private void refreshPlaylistSelector() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        Set<String> storedIds = preferences.getStringSet(PLAYLIST_IDS, new HashSet<>());
        playlistIds.clear();
        List<String> names = new ArrayList<>();
        if (storedIds != null) {
            for (String id : storedIds) {
                if (id.matches("[0-9a-fA-F-]{36}") && playlistFile(id).isFile()) {
                    playlistIds.add(id);
                    names.add(preferences.getString(PLAYLIST_NAME_PREFIX + id, getString(R.string.saved_playlist)));
                }
            }
        }
        if (activePlaylistId == null || !playlistIds.contains(activePlaylistId)) {
            activePlaylistId = playlistIds.isEmpty() ? null : playlistIds.get(0);
        }
        if (activePlaylistId == null) names.add(getString(R.string.no_playlist));
        ArrayAdapter<String> playlistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        playlistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        updatingPlaylistSelector = true;
        playlistSelector.setAdapter(playlistAdapter);
        int selectedPosition = activePlaylistId == null ? 0 : playlistIds.indexOf(activePlaylistId);
        playlistSelector.setSelection(selectedPosition, false);
        updatingPlaylistSelector = false;
        if (activePlaylistId == null) preferences.edit().remove(ACTIVE_PLAYLIST_ID).apply();
        updateDeletePlaylistButton();
    }

    private void clearPlaylistView() {
        channels.clear();
        adapter.replace(channels);
        playlistName.setText(R.string.no_playlist);
        status.setText(R.string.no_playlist);
        updateGroupOptions();
        updateDeletePlaylistButton();
    }

    private void showPlaylist(List<Channel> loaded, String name) {
        channels.clear();
        channels.addAll(loaded);
        filterQuery = "";
        selectedGroup = null;
        favoritesOnly = false;
        ((EditText) findViewById(R.id.channel_search)).setText("");
        updateGroupOptions();
        updateFavoritesButton();
        adapter.setFavorites(favoriteUrls);
        applyFilters();
        playlistName.setText(name);
        if (loaded.isEmpty()) status.setText(R.string.no_valid_channels);
        else status.setText(getString(R.string.channels_loaded, loaded.size()));
        updateDeletePlaylistButton();
        if (!loaded.isEmpty()) findViewById(R.id.channel_list).requestFocus();
    }

    private void updateDeletePlaylistButton() {
        if (deletePlaylistButton != null) {
            deletePlaylistButton.setEnabled(activePlaylistId != null && playlistFile(activePlaylistId).isFile());
        }
    }

    private void confirmDeletePlaylist() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_playlist_title)
                .setMessage(R.string.delete_playlist_confirmation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_playlist, (dialog, which) -> deletePlaylist())
                .show();
    }

    private void deletePlaylist() {
        if (activePlaylistId == null) return;
        String deletedId = activePlaylistId;
        File saved = playlistFile(deletedId);
        File backup = new File(saved.getParentFile(), saved.getName() + ".bak");
        if ((backup.exists() && !backup.delete()) || (saved.exists() && !saved.delete())) {
            Toast.makeText(this, R.string.playlist_delete_failed, Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        Set<String> ids = new HashSet<>(preferences.getStringSet(PLAYLIST_IDS, new HashSet<>()));
        ids.remove(deletedId);
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet(PLAYLIST_IDS, ids)
                .remove(PLAYLIST_NAME_PREFIX + deletedId);
        activePlaylistId = ids.isEmpty() ? null : ids.iterator().next();
        if (activePlaylistId == null) editor.remove(ACTIVE_PLAYLIST_ID);
        else editor.putString(ACTIVE_PLAYLIST_ID, activePlaylistId);
        if (!editor.commit()) {
            Toast.makeText(this, R.string.playlist_delete_failed, Toast.LENGTH_LONG).show();
        }
        refreshPlaylistSelector();
        if (activePlaylistId != null) {
            loadSavedPlaylist(activePlaylistId);
            return;
        }
        filterQuery = "";
        selectedGroup = null;
        favoritesOnly = false;
        ((EditText) findViewById(R.id.channel_search)).setText("");
        updateGroupOptions();
        updateFavoritesButton();
        adapter.setFavorites(favoriteUrls);
        applyFilters();
        playlistName.setText(R.string.no_playlist);
        status.setText(R.string.playlist_deleted);
        updateDeletePlaylistButton();
    }

    private List<Channel> parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }

    private List<Channel> parse(InputStream input) throws IOException {
        List<Channel> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line, pendingName = null, pendingGroup = null;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#EXTM3U") || line.startsWith("#EXTVLCOPT")) continue;
                if (line.startsWith("#EXTINF")) {
                    int comma = line.indexOf(',');
                    pendingName = comma >= 0 && comma + 1 < line.length() ? line.substring(comma + 1).trim() : "Unbenannter Sender";
                    Matcher matcher = GROUP.matcher(line);
                    pendingGroup = matcher.find() ? matcher.group(1) : "Weitere";
                } else if (!line.startsWith("#") && pendingName != null) {
                    result.add(new Channel(pendingName, line, pendingGroup));
                    pendingName = null; pendingGroup = null;
                }
            }
        }
        return result;
    }

    private void playChannel(Channel channel) {
        currentChannel = channel;
        String mode = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getString(PLAYER_MODE, ExternalPlayerSupport.EXOPLAYER);
        if (!ExternalPlayerSupport.EXOPLAYER.equals(mode) && playWithExternalPlayer(channel, mode)) return;
        playWithExoPlayer(channel);
    }

    private boolean playWithExternalPlayer(Channel channel, String mode) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(channel.url), "video/*");
        intent.putExtra(Intent.EXTRA_TITLE, channel.name);
        PackageManager packageManager = getPackageManager();
        for (String packageName : ExternalPlayerSupport.packageCandidates(mode)) {
            intent.setPackage(packageName);
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) continue;
            try {
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException | SecurityException e) {
                // Continue to the next installed candidate, if any.
            }
        }
        Toast.makeText(this, R.string.external_player_unavailable, Toast.LENGTH_LONG).show();
        return false;
    }

    private void playWithExoPlayer(Channel channel) {
        playerVisible = true;
        currentChannel = channel;
        if (findViewById(R.id.player_view) == null) setContentView(R.layout.player_screen);
        PlayerView playerView = findViewById(R.id.player_view);
        TextView title = findViewById(R.id.player_title);
        Button previous = findViewById(R.id.previous_channel);
        Button next = findViewById(R.id.next_channel);
        title.setText(channel.name);
        previous.setOnClickListener(v -> playAdjacentChannel(-1));
        next.setOnClickListener(v -> playAdjacentChannel(1));
        previous.setEnabled(ChannelNavigation.adjacent(adapterChannels(), channel, -1) != null);
        next.setEnabled(ChannelNavigation.adjacent(adapterChannels(), channel, 1) != null);
        if (player == null) {
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                            PlaybackBufferPolicy.MIN_BUFFER_MS,
                            PlaybackBufferPolicy.MAX_BUFFER_MS,
                            PlaybackBufferPolicy.BUFFER_FOR_PLAYBACK_MS,
                            PlaybackBufferPolicy.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    .build();
            player = new ExoPlayer.Builder(this)
                    .setLoadControl(loadControl)
                    .build();
        }
        playerView.setPlayer(player);
        MediaItem.LiveConfiguration liveConfiguration = new MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(PlaybackBufferPolicy.LIVE_TARGET_OFFSET_MS)
                .setMinOffsetMs(PlaybackBufferPolicy.LIVE_MIN_OFFSET_MS)
                .setMaxOffsetMs(PlaybackBufferPolicy.LIVE_MAX_OFFSET_MS)
                .build();
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(channel.url))
                .setLiveConfiguration(liveConfiguration)
                .build();
        player.setMediaItem(mediaItem);
        player.prepare(); player.play();
    }

    private List<Channel> adapterChannels() {
        return ChannelFilter.filter(channels, filterQuery, selectedGroup, favoriteUrls, favoritesOnly);
    }

    private void playAdjacentChannel(int direction) {
        Channel adjacent = ChannelNavigation.adjacent(adapterChannels(), currentChannel, direction);
        if (adjacent != null) playWithExoPlayer(adjacent);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (playerVisible && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                playAdjacentChannel(-1);
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {
                playAdjacentChannel(1);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void stopPlayer() {
        if (player != null) { player.stop(); player.clearMediaItems(); }
    }

    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
