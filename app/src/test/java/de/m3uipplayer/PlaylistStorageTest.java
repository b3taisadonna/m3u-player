package de.m3uipplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PlaylistStorageTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test public void stagedPlaylistDoesNotReplaceExistingFileUntilCommitted() throws Exception {
        File destination = new File(temporaryFolder.getRoot(), "playlist_first.m3u");
        write(destination, "old playlist");

        File staged = PlaylistStorage.stage(stream("new playlist"), destination);

        assertEquals("old playlist", read(destination));
        PlaylistStorage.commit(staged, destination);
        assertEquals("new playlist", read(destination));
    }

    @Test public void differentPlaylistFilesCanBeStoredIndependently() throws Exception {
        File first = new File(temporaryFolder.getRoot(), "playlist_first.m3u");
        File second = new File(temporaryFolder.getRoot(), "playlist_second.m3u");

        PlaylistStorage.commit(PlaylistStorage.stage(stream("first"), first), first);
        PlaylistStorage.commit(PlaylistStorage.stage(stream("second"), second), second);

        assertTrue(first.isFile());
        assertTrue(second.isFile());
        assertEquals("first", read(first));
        assertEquals("second", read(second));
    }

    @Test public void failedCopyLeavesPreviouslySavedPlaylistUntouched() throws Exception {
        File destination = new File(temporaryFolder.getRoot(), "playlist_first.m3u");
        write(destination, "keep this");
        InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("read failed"); }
        };

        try {
            PlaylistStorage.stage(broken, destination);
            fail("Expected the source read failure");
        } catch (IOException expected) {
            assertEquals("keep this", read(destination));
        }
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(File file, String value) throws IOException {
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(File file) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
