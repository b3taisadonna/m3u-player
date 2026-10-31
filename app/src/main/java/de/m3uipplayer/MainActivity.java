package de.m3uipplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
        new Thread(() -> {
            List<Channel> loaded = parse(uri);
            runOnUiThread(() -> {
                channels.clear(); channels.addAll(loaded); adapter.replace(channels);
                String name = uri.getLastPathSegment() == null ? "USB-Playlist" : uri.getLastPathSegment();
                playlistName.setText(name);
                status.setText(loaded.isEmpty() ? "Keine gültigen Sender gefunden." : loaded.size() + " Sender geladen");
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
        playerVisible = true;
        setContentView(R.layout.player_screen);
        PlayerView playerView = findViewById(R.id.player_view);
        TextView title = findViewById(R.id.player_title);
        title.setText(channel.name);
        if (player == null) player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)));
        player.prepare(); player.play();
    }

    private void stopPlayer() {
        if (player != null) { player.stop(); player.clearMediaItems(); }
    }

    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
