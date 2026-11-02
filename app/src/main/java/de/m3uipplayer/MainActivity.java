package de.m3uipplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final ExecutorService playlistExecutor = Executors.newSingleThreadExecutor(task -> new Thread(() -> {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        task.run();
    }, "playlist-parser"));
    private Future<?> playlistTask;
    private int playlistRequestId;
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
        if (!channels.isEmpty()) adapter.replace(channels);
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
        if (playlistTask != null) playlistTask.cancel(true);
        int requestId = ++playlistRequestId;
        playlistTask = playlistExecutor.submit(() -> {
            List<Channel> loaded = parse(uri);
            runOnUiThread(() -> {
                if (requestId != playlistRequestId || isFinishing() || isDestroyed()) return;
                channels.clear(); channels.addAll(loaded); adapter.replace(channels);
                String name = uri.getLastPathSegment() == null ? "USB-Playlist" : uri.getLastPathSegment();
                playlistName.setText(name);
                status.setText(loaded.isEmpty() ? "Keine gültigen Sender gefunden." : loaded.size() + " Sender geladen");
                if (!loaded.isEmpty()) findViewById(R.id.channel_list).requestFocus();
            });
        });
    }

    private List<Channel> parse(Uri uri) {
        List<Channel> result = new ArrayList<>();
        try (InputStream input = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line, pendingName = null, pendingGroup = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
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
        playerVisible = true;
        setContentView(R.layout.player_screen);
        PlayerView playerView = findViewById(R.id.player_view);
        TextView title = findViewById(R.id.player_title);
        title.setText(channel.name);
        if (player == null) {
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(1280, 720).build());
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 30_000, 1_500, 5_000)
                    .setTargetBufferBytes(24 * 1024 * 1024)
                    .setPrioritizeTimeOverSizeThresholds(false)
                    .build();
            player = new ExoPlayer.Builder(this)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build();
            player.addListener(new Player.Listener() {
                @Override public void onPlayerError(PlaybackException error) {
                    Log.e("MainActivity", "Playback failed for channel: " + channel.name, error);
                    Toast.makeText(MainActivity.this, getPlaybackErrorMessage(error), Toast.LENGTH_LONG).show();
                }
            });
        }
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)));
        player.prepare(); player.play();
    }

    private String getPlaybackErrorMessage(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                int statusCode = ((HttpDataSource.InvalidResponseCodeException) cause).responseCode;
                if (statusCode == 401) return "Der Anbieter lehnt den Sender ab (HTTP 401). Bitte Zugangsdaten oder Playlist prüfen.";
                if (statusCode == 403) return "Der Anbieter sperrt den Sender (HTTP 403). Bitte Anbieter oder Abo prüfen.";
                if (statusCode == 404) return "Der Sender wurde beim Anbieter nicht gefunden (HTTP 404).";
                return "Der Anbieter meldet HTTP " + statusCode + ". Bitte Sender oder Playlist prüfen.";
            }
            if (cause instanceof UnknownHostException) return "Kein Netzwerk oder der Stream-Server ist nicht erreichbar.";
            if (cause instanceof SocketTimeoutException) return "Zeitüberschreitung: Der Stream-Server antwortet nicht.";
        }
        return "Stream konnte nicht gestartet werden. Prüfe Netzwerk und Sender-URL.";
    }

    private void stopPlayer() {
        releasePlayer();
    }

    private void releasePlayer() {
        if (player == null) return;
        PlayerView playerView = playerVisible ? findViewById(R.id.player_view) : null;
        if (playerView != null) playerView.setPlayer(null);
        player.release();
        player = null;
    }

    @Override protected void onStop() {
        boolean wasPlayerVisible = playerVisible;
        releasePlayer();
        if (wasPlayerVisible && !isChangingConfigurations()) showLibrary();
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (playlistTask != null) playlistTask.cancel(true);
        playlistExecutor.shutdownNow();
        releasePlayer();
        super.onDestroy();
    }
}
