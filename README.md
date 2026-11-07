# M3U IP Player für Android TV

Ein schlanker Android-TV-Player für lokale `.m3u`- und `.m3u8`-Playlists.

## Verwendung

1. APK bauen und auf dem Android-TV installieren.
2. USB-Stick anschließen.
3. **Playlist hinzufügen** wählen und die Datei im Dateiauswahldialog öffnen.
4. Weitere Playlists lassen sich jederzeit hinzufügen und bleiben im privaten App-Speicher erhalten. Über die Playlist-Auswahl kannst du zwischen ihnen wechseln; **Playlist löschen** entfernt nur die gerade ausgewählte Playlist.
5. Sender mit dem D-Pad auswählen und mit OK starten.

Die App verwendet den Android-Dateiauswahldialog. Dadurch funktioniert sie mit USB-Sticks, ohne einen festen Mount-Pfad oder zusätzliche Speicherberechtigungen vorauszusetzen. Unterstützt werden HTTP/HTTPS-Streams, sofern das jeweilige Format von Media3 unterstützt wird.

## Funktionen in Version 1.8

- Sender nach Name oder Gruppe durchsuchen; die Suche ignoriert Groß-/Kleinschreibung.
- Sendergruppen über die Gruppenauswahl filtern.
- Sender als Favoriten markieren und Favoriten dauerhaft auf diesem Gerät speichern. Der Stream-Link dient als Favoritenkennung, auch playlistübergreifend.
- Alle hinzugefügten Playlists werden dauerhaft im App-Speicher abgelegt. Beim App-Start wird die zuletzt ausgewählte Playlist wiederhergestellt; **Playlist löschen** entfernt nur die ausgewählte Playlist.
- Unter **Videoplayer** kannst du den integrierten ExoPlayer, VLC oder MX Player auswählen. Für VLC/MX muss die jeweilige App installiert sein; externe Player erhalten die Stream-URL und übernehmen ihre eigene Wiedergabeoberfläche. Eine ruckelfreie Wiedergabe kann wegen Netzwerk, Stream und Gerät nicht garantiert werden.
- Im integrierten Player schalten **Vorheriger/Nächster Sender** sowie die Fernbedienungstasten **Media Previous/Next** innerhalb der aktuell gefilterten Senderliste um. Am Listenende wird nicht automatisch zum anderen Ende gesprungen. Externe Player verwenden ihre eigenen Bedienelemente.
- Der integrierte Player puffert bis zu 120 Sekunden vor und wartet nach einem erneuten Buffering bis zu 10 Sekunden gepufferte Wiedergabe ab. Das kann kurze Netzschwankungen abfangen, erhöht aber Start-/Wartezeit und garantiert keine Wiedergabe bei dauerhaft zu geringer Bandbreite.
- Bei Live-Streams zielt der Player außerdem auf 60 Sekunden Abstand zur Live-Kante (zulässiger Bereich 45–90 Sekunden), damit der HLS-Puffer nicht direkt am Live-Rand bei kurzen Segmentfenstern hängen bleibt. Der Stream-Anbieter muss ältere Segmente weiterhin bereitstellen.

Diese Funktionen sind eigenständige Erweiterungen für M3U Player. Eine Verhaltensgleichheit mit anderen IPTV-Apps wird nicht behauptet.

## Build

```bash
./gradlew assembleDebug
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

Das Projekt benötigt Android Studio bzw. eine Android-SDK-Installation mit SDK 35. Die Abhängigkeiten werden beim ersten Build aus Google Maven und Maven Central geladen.
