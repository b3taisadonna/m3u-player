package de.m3uipplayer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackBufferPolicyTest {
    @Test public void bufferHasEnoughHeadroomForThePlaybackThresholds() {
        assertTrue(PlaybackBufferPolicy.MIN_BUFFER_MS >= PlaybackBufferPolicy.BUFFER_FOR_PLAYBACK_MS);
        assertTrue(PlaybackBufferPolicy.MIN_BUFFER_MS
                >= PlaybackBufferPolicy.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        assertTrue(PlaybackBufferPolicy.MAX_BUFFER_MS >= PlaybackBufferPolicy.MIN_BUFFER_MS);
        assertTrue(PlaybackBufferPolicy.LIVE_MIN_OFFSET_MS <= PlaybackBufferPolicy.LIVE_TARGET_OFFSET_MS);
        assertTrue(PlaybackBufferPolicy.LIVE_MAX_OFFSET_MS >= PlaybackBufferPolicy.LIVE_TARGET_OFFSET_MS);
    }

    @Test public void configuredBufferIncreasesPreloadAndRecoveryComparedToDefaults() {
        assertTrue(PlaybackBufferPolicy.MIN_BUFFER_MS > 15_000);
        assertTrue(PlaybackBufferPolicy.MAX_BUFFER_MS > 50_000);
        assertTrue(PlaybackBufferPolicy.BUFFER_FOR_PLAYBACK_MS > 1_000);
        assertTrue(PlaybackBufferPolicy.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS > 2_500);
        assertTrue(PlaybackBufferPolicy.LIVE_TARGET_OFFSET_MS >= PlaybackBufferPolicy.MIN_BUFFER_MS);
    }
}
