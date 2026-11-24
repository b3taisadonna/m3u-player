# M3U IP Player für Android TV

Ein schlanker Android-TV-Player für lokale `.m3u`- und `.m3u8`-Playlists mit nativer HLS-Unterstützung, optimierter Pufferung und vollständiger Fernbedienungssteuerung.

---

## 📥 Download

Die fertige App kann direkt heruntergeladen und auf dem Android TV installiert werden:

- **[Aktuelle Version 1.6 herunterladen (app-debug.apk)](https://github.com/b3taisadonna/m3u-player/releases/download/v1.6/app-debug.apk)**
- **[Alle GitHub Releases & Versionen ansehen](https://github.com/b3taisadonna/m3u-player/releases)**

---

## ✨ Features & Neuerungen (v1.6)

- **🎮 Vollständige Fernbedienungssteuerung (Kanal vor / zurück):**
  - **Kanal vor (nächster Sender):** `CH+`, `▲` (D-Pad Up), `►` (D-Pad Right), `PAGE UP`, `Media Next`
  - **Kanal zurück (vorheriger Sender):** `CH-`, `▼` (D-Pad Down), `◄` (D-Pad Left), `PAGE DOWN`, `Media Previous`
  - **Play / Pause:** `Play/Pause`-Taste der TV-Fernbedienung
  - **Automatische Synchronisation:** Beim Zurückkehren aus dem Player scrollt die Senderliste automatisch exakt zum aktuell laufenden Sender.
- **⚡ Optimiertes Puffer-Management (kein Einfrieren nach 20s):**
  - Native **HLS-Unterstützung** (`media3-exoplayer-hls`) für dynamische `.m3u8`-Streams.
  - Dynamischer Puffer (`DefaultLoadControl`): 30s Mindestpuffer, bis zu 2 Minuten Vorlauf, schneller Start nach 1,5s.
  - Großzügige HTTP-Timeouts (20s Connect, 30s Read), Cross-Protocol Redirects und Browser User-Agent gegen Server-Throttling.
  - 15s Live-Target-Offset zur Absicherung bei Netzwerkschwankungen.
- **🎨 Modernes TV-Logo & Leanback-Banner:**
  - Modernes Vektor-Display-Logo im Hauptbildschirm und Player.
  - Maßgeschneidertes 16:9 Android TV Launcher Banner (`tv_banner.xml`).
  - Halbtransparente OSD-Infoblende mit Sendernummer, Sendername und Programmgruppe beim Umschalten.
- **🛡️ Stabilität & Lifecycle:**
  - Korrekte Internet- und Leanback-Berechtigungen im Manifest.
  - Hintergrundwiedergabe stoppt sauber beim Verlassen der App.
  - Playlist-Name und Senderanzahl bleiben beim Zappen erhalten.

---

## 📺 Verwendung

1. APK herunterladen und per USB-Stick oder Sideloading (z. B. Send Files to TV, ADB) auf dem Android TV installieren.
2. USB-Stick mit `.m3u`- oder `.m3u8`-Playlist anschließen (oder lokale Datei wählen).
3. **USB-PLAYLIST ÖFFNEN** wählen und die Playlist im Dateidialog auswählen.
4. Sender auswählen oder während der Wiedergabe einfach mit `CH+` / `CH-` bzw. den Pfeiltasten durchzappen.

---

## 🛠️ Build

Das Projekt verwendet Gradle mit Android SDK 35:

```bash
./gradlew assembleDebug
```

Die fertige APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`.
