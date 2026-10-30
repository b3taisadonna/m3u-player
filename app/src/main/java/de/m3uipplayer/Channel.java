package de.m3uipplayer;

public class Channel {
    public final String name;
    public final String url;
    public final String group;

    public Channel(String name, String url, String group) {
        this.name = name;
        this.url = url;
        this.group = group == null || group.isEmpty() ? "Weitere" : group;
    }
}
