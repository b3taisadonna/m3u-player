package de.m3uipplayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ExternalPlayerSupport {
    public static final String EXOPLAYER = "exoplayer";
    public static final String VLC = "vlc";
    public static final String MX_PLAYER = "mx_player";

    private ExternalPlayerSupport() { }

    public static List<String> packageCandidates(String player) {
        if (VLC.equals(player)) return Collections.singletonList("org.videolan.vlc");
        if (MX_PLAYER.equals(player)) {
            return Arrays.asList("com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.pro");
        }
        return Collections.emptyList();
    }
}
