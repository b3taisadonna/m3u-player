package de.m3uipplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class FavoriteChannelsTest {
    @Test public void togglingAddsThenRemovesTheStreamUrl() {
        Set<String> favorites = new HashSet<>();

        assertTrue(FavoriteChannels.toggle(favorites, "https://example.test/channel"));
        assertTrue(favorites.contains("https://example.test/channel"));
        assertFalse(FavoriteChannels.toggle(favorites, "https://example.test/channel"));
        assertFalse(favorites.contains("https://example.test/channel"));
    }
}
