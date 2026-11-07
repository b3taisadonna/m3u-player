package de.m3uipplayer;

public final class PlaybackBufferPolicy {
    public static final int MIN_BUFFER_MS = 60_000;
    public static final int MAX_BUFFER_MS = 120_000;
    public static final int BUFFER_FOR_PLAYBACK_MS = 5_000;
    public static final int BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 10_000;
    public static final long LIVE_TARGET_OFFSET_MS = 60_000L;
    public static final long LIVE_MIN_OFFSET_MS = 45_000L;
    public static final long LIVE_MAX_OFFSET_MS = 90_000L;

    private PlaybackBufferPolicy() { }
}
