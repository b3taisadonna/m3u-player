package de.m3uipplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class ExternalPlayerSupportTest {
    @Test public void vlcUsesItsAndroidPackageName() {
        assertEquals(Collections.singletonList("org.videolan.vlc"),
                ExternalPlayerSupport.packageCandidates(ExternalPlayerSupport.VLC));
    }

    @Test public void mxPlayerChecksFreeAndProPackageNames() {
        assertEquals(2, ExternalPlayerSupport.packageCandidates(ExternalPlayerSupport.MX_PLAYER).size());
        assertTrue(ExternalPlayerSupport.packageCandidates(ExternalPlayerSupport.MX_PLAYER)
                .contains("com.mxtech.videoplayer.ad"));
        assertTrue(ExternalPlayerSupport.packageCandidates(ExternalPlayerSupport.MX_PLAYER)
                .contains("com.mxtech.videoplayer.pro"));
    }

    @Test public void internalOrUnknownModesDoNotResolveToAnExternalPackage() {
        assertTrue(ExternalPlayerSupport.packageCandidates(ExternalPlayerSupport.EXOPLAYER).isEmpty());
        assertTrue(ExternalPlayerSupport.packageCandidates("unknown").isEmpty());
    }
}
