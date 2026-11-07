package de.m3uipplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChannelFilter {
    private ChannelFilter() { }

    public static List<Channel> filter(List<Channel> channels, String query, String group,
                                       Set<String> favoriteUrls, boolean favoritesOnly) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Channel> result = new ArrayList<>();
        for (Channel channel : channels) {
            if (group != null && !group.equals(channel.group)) continue;
            if (favoritesOnly && (favoriteUrls == null || !favoriteUrls.contains(channel.url))) continue;
            if (!normalizedQuery.isEmpty()
                    && !channel.name.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    && !channel.group.toLowerCase(Locale.ROOT).contains(normalizedQuery)) continue;
            result.add(channel);
        }
        return result;
    }
}
