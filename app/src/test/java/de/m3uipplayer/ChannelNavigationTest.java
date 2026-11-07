package de.m3uipplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ChannelNavigationTest {
    private final Channel first = new Channel("One", "https://example.test/one", "News");
    private final Channel second = new Channel("Two", "https://example.test/two", "News");
    private final List<Channel> channels = Arrays.asList(first, second);

    @Test public void movesForwardAndBackwardWithinTheCurrentChannelList() {
        assertEquals(second, ChannelNavigation.adjacent(channels, first, 1));
        assertEquals(first, ChannelNavigation.adjacent(channels, second, -1));
    }

    @Test public void stopsAtListBoundariesWithoutWrapping() {
        assertNull(ChannelNavigation.adjacent(channels, first, -1));
        assertNull(ChannelNavigation.adjacent(channels, second, 1));
        assertNull(ChannelNavigation.adjacent(Collections.singletonList(first), first, 1));
    }

    @Test public void unknownChannelHasNoAdjacentResult() {
        assertNull(ChannelNavigation.adjacent(channels,
                new Channel("Other", "https://example.test/other", "Other"), 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDirectionsOtherThanPreviousOrNext() {
        ChannelNavigation.adjacent(channels, first, 0);
    }
}
