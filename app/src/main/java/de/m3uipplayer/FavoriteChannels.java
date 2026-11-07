package de.m3uipplayer;

import java.util.Set;

public final class FavoriteChannels {
    private FavoriteChannels() { }

    public static boolean toggle(Set<String> favoriteUrls, String url) {
        if (favoriteUrls.contains(url)) {
            favoriteUrls.remove(url);
            return false;
        }
        favoriteUrls.add(url);
        return true;
    }
}
