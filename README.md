# M3U IP Player für Android TV

Ein schlanker Android-TV-Player für lokale `.m3u`- und `.m3u8`-Playlists.

## Verwendung

1. APK bauen und auf dem Android-TV installieren.
2. USB-Stick anschließen.
3. **USB-Playlist öffnen** wählen und die Playlist im Dateiauswahldialog öffnen.
4. Sender mit dem D-Pad auswählen und mit OK starten.

Die App verwendet den Android-Dateiauswahldialog. Dadurch funktioniert sie mit USB-Sticks, ohne einen festen Mount-Pfad oder zusätzliche Speicherberechtigungen vorauszusetzen. Unterstützt werden HTTP/HTTPS-Streams, sofern das jeweilige Format von Media3 unterstützt wird.

## Build

```bash
./gradlew assembleDebug
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

Das Projekt benötigt Android Studio bzw. eine Android-SDK-Installation mit SDK 35. Die Abhängigkeiten werden beim ersten Build aus Google Maven und Maven Central geladen.
