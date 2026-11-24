package de.m3uipplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UnstableApi
public class MainActivity extends AppCompatActivity {
    private static final int PICK_PLAYLIST = 41;
    private final List<Channel> channels = new ArrayList<>();
    private ChannelAdapter adapter;
    private ExoPlayer player;
    private boolean playerVisible;
    private TextView status;
    private TextView playlistName;
    private String currentPlaylistName;
    private String currentStatusText;
    private int currentChannelIndex = -1;
    private TextView playerTitle;
    private TextView playerGroup;
    private static final Pattern GROUP = Pattern.compile("group-title\\s*=\\s*\\\"([^\\\"]*)\\\"", Pattern.CASE_INSENSITIVE);

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        showLibrary();
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
        adapter = new ChannelAdapter(this::playChannel);
        RecyclerView list = findViewById(R.id.channel_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        Button open = findViewById(R.id.open_button);
        open.setOnClickListener(v -> pickPlaylist());
        open.requestFocus();
        if (!channels.isEmpty()) {
            adapter.replace(channels);
            if (currentPlaylistName != null) playlistName.setText(currentPlaylistName);
            if (currentStatusText != null) status.setText(currentStatusText);
            if (currentChannelIndex >= 0 && currentChannelIndex < channels.size()) {
                list.scrollToPosition(currentChannelIndex);
            }
        }
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
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) { }
        loadPlaylist(uri);
    }

    private void loadPlaylist(Uri uri) {
        status.setText("Playlist wird gelesen …");
        new Thread(() -> {
            List<Channel> loaded = parse(uri);
            runOnUiThread(() -> {
                channels.clear(); channels.addAll(loaded); adapter.replace(channels);
                currentPlaylistName = uri.getLastPathSegment() == null ? "USB-Playlist" : uri.getLastPathSegment();
                playlistName.setText(currentPlaylistName);
                currentStatusText = loaded.isEmpty() ? "Keine gültigen Sender gefunden." : loaded.size() + " Sender geladen";
                status.setText(currentStatusText);
                if (!loaded.isEmpty()) findViewById(R.id.channel_list).requestFocus();
            });
        }).start();
    }

    private List<Channel> parse(Uri uri) {
        List<Channel> result = new ArrayList<>();
        try (InputStream input = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
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
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Playlist konnte nicht gelesen werden", Toast.LENGTH_LONG).show());
        }
        return result;
    }

    private void playChannel(Channel channel) {
        int idx = channels.indexOf(channel);
        playChannelAtIndex(idx >= 0 ? idx : 0);
    }

    private void playChannelAtIndex(int index) {
        if (channels.isEmpty()) return;
        if (index < 0) {
            index = channels.size() - 1;
        } else if (index >= channels.size()) {
            index = 0;
        }
        currentChannelIndex = index;
        Channel channel = channels.get(currentChannelIndex);

        if (!playerVisible) {
            playerVisible = true;
            setContentView(R.layout.player_screen);
        }

        PlayerView playerView = findViewById(R.id.player_view);
        playerTitle = findViewById(R.id.player_title);
        playerGroup = findViewById(R.id.player_group);

        String titleText = String.format(Locale.getDefault(), "%03d  %s", currentChannelIndex + 1, channel.name);
        if (playerTitle != null) playerTitle.setText(titleText);
        if (playerGroup != null) playerGroup.setText(channel.group);

        if (player == null) {
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    30_000,
                    120_000,
                    1_500,
                    3_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(15_000, true)
                .build();

            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setConnectTimeoutMs(20_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true);

            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpFactory);

            player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();
        }
        playerView.setPlayer(player);

        MediaItem mediaItem = new MediaItem.Builder()
            .setUri(Uri.parse(channel.url))
            .setLiveConfiguration(
                new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(15_000)
                    .setMinOffsetMs(5_000)
                    .setMaxOffsetMs(45_000)
                    .build()
            )
            .build();

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void nextChannel() {
        if (channels.isEmpty()) return;
        playChannelAtIndex(currentChannelIndex + 1);
    }

    private void previousChannel() {
        if (channels.isEmpty()) return;
        playChannelAtIndex(currentChannelIndex - 1);
    }

    private void togglePlayPause() {
        if (player != null) {
            if (player.isPlaying()) player.pause();
            else player.play();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && playerVisible) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_CHANNEL_UP:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_PAGE_UP:
                    nextChannel();
                    return true;

                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_PAGE_DOWN:
                    previousChannel();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    togglePlayPause();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    if (player != null && !player.isPlaying()) player.play();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    if (player != null && player.isPlaying()) player.pause();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void stopPlayer() {
        if (player != null) { player.stop(); player.clearMediaItems(); }
    }

    @Override protected void onStop() {
        super.onStop();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
