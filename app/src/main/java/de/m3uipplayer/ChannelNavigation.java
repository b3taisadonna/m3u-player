package de.m3uipplayer;

import java.util.List;

public final class ChannelNavigation {
    private ChannelNavigation() { }

    public static Channel adjacent(List<Channel> channels, Channel current, int direction) {
        if (direction != -1 && direction != 1) throw new IllegalArgumentException("direction must be -1 or 1");
        int index = channels.indexOf(current);
        int nextIndex = index + direction;
        if (index < 0 || nextIndex < 0 || nextIndex >= channels.size()) return null;
        return channels.get(nextIndex);
    }
}
