package de.m3uipplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class PlaylistStorage {
    private PlaylistStorage() { }

    public static File stage(InputStream source, File destination) throws IOException {
        if (source == null) throw new IOException("Playlist-Datei kann nicht geöffnet werden");
        File directory = destination.getAbsoluteFile().getParentFile();
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
            throw new IOException("App-Speicher ist nicht verfügbar");
        }

        File staged = File.createTempFile("playlist-", ".tmp", directory);
        try (OutputStream output = new FileOutputStream(staged)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
            return staged;
        } catch (IOException e) {
            staged.delete();
            throw e;
        }
    }

    public static void commit(File staged, File destination) throws IOException {
        File directory = destination.getAbsoluteFile().getParentFile();
        File backup = new File(directory, destination.getName() + ".bak");
        if (backup.exists() && !backup.delete()) {
            staged.delete();
            throw new IOException("Vorherige Playlist-Sicherung kann nicht entfernt werden");
        }

        boolean hadPrevious = destination.exists();
        if (hadPrevious && !destination.renameTo(backup)) {
            staged.delete();
            throw new IOException("Bisherige Playlist kann nicht ersetzt werden");
        }
        if (!staged.renameTo(destination)) {
            if (hadPrevious && !backup.renameTo(destination)) {
                throw new IOException("Playlist konnte nicht gespeichert oder wiederhergestellt werden");
            }
            staged.delete();
            throw new IOException("Neue Playlist konnte nicht gespeichert werden");
        }
        if (backup.exists() && !backup.delete()) {
            throw new IOException("Playlist gespeichert, aber alte Sicherung konnte nicht entfernt werden");
        }
    }
}
