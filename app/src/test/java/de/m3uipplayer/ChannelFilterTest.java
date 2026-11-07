package de.m3uipplayer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class ChannelFilterTest {
    private final Channel news = new Channel("World News", "https://example.test/news", "News");
    private final Channel sports = new Channel("Sports Live", "https://example.test/sports", "Sports");
    private final List<Channel> channels = Arrays.asList(news, sports);

    @Test public void searchTrimsAndIgnoresCaseAcrossNamesAndGroups() {
        assertEquals(Collections.singletonList(news),
                ChannelFilter.filter(channels, "  WORLD ", null, Collections.emptySet(), false));
        assertEquals(Collections.singletonList(sports),
                ChannelFilter.filter(channels, "sports", null, Collections.emptySet(), false));
    }

    @Test public void groupFilterKeepsOnlyTheSelectedGroup() {
        assertEquals(Collections.singletonList(sports),
                ChannelFilter.filter(channels, "", "Sports", Collections.emptySet(), false));
    }

    @Test public void favoriteOnlyAndOtherFiltersAreCombined() {
        HashSet<String> favorites = new HashSet<>(Collections.singletonList(news.url));
        assertEquals(Collections.singletonList(news),
                ChannelFilter.filter(channels, "world", "News", favorites, true));
        assertEquals(Collections.emptyList(),
                ChannelFilter.filter(channels, "world", "Sports", favorites, true));
    }

    @Test public void nullSearchAndFavoritesDoNotExcludeChannelsUnlessFavoritesOnlyIsEnabled() {
        assertEquals(channels, ChannelFilter.filter(channels, null, null, null, false));
        assertEquals(Collections.emptyList(), ChannelFilter.filter(channels, null, null, null, true));
    }
}
