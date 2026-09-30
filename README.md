# TV Remote

A WiFi remote control for Android TV / Google TV devices (Sony, TCL,
Hisense, Nvidia Shield, Chromecast with Google TV, etc.) — anything
running Android TV OS, since they all speak the same system-level
remote protocol regardless of the TV brand's own skin. No ads, and the
button layout is yours to customize.

## What it does

- **Discovery**: finds TVs on the same WiFi network via mDNS
  (`_androidtvremote2._tcp`), with manual IP entry as a fallback.
- **Pairing**: opens a TLS connection to the TV, shows a 6-digit code on
  the TV screen, and you type it into the app once — standard Android
  TV Remote pairing handshake.
- **Remote control**: power, home, back, D-pad, volume, mute, play/
  pause, rewind/forward, menu, settings.
- **Voice**: tap the mic, speak, it's sent to the TV as a search.
- **Custom layout**: add, remove, and reorder buttons from Settings.

## Setup

1. Open this folder in Android Studio (Ladybird/Koala or newer).
2. Let Gradle sync — it will pull in the protobuf plugin, Compose, and
   Bouncy Castle automatically.
3. Run on a real phone on the same WiFi network as your TV (an emulator
   has no real WiFi to discover TVs with).
4. On first launch: tap a discovered TV (or type its IP), confirm the
   code shown on the TV screen, and you're paired.

## What's genuinely working vs. what to double-check

I wrote this end to end, but I can't compile/run an Android build in
this environment, so treat the first build in Android Studio as the
real test:

- The **pairing and remote wire protocol** (`pairing.proto`,
  `remote.proto`, `PairingClient`, `RemoteClient`) follows the
  documented Android TV Remote Service protocol structure faithfully.
  The one simplification: message length is sent as a single byte
  (fine for every message this app sends — they're all under 255
  bytes). If you ever add a message that could exceed that, swap the
  `writeByte`/`readUnsignedByte` calls for a real protobuf varint via
  `CodedOutputStream`/`CodedInputStream`.
- **Certificate handling** (`CertificateManager`) generates and stores
  a real self-signed cert in a private PKCS12 file — this part is
  fully implemented, not stubbed.
- **Voice** uses Android's built-in `SpeechRecognizer`. It requires
  `RECORD_AUDIO` permission, which the app requests on first mic tap.
- **Reordering buttons** uses simple up/down arrows rather than
  drag-and-drop, to keep the UI dependency-light for a first version.
  Swap in a drag library (e.g. `reorderable`) later if you want that
  feel.
- The launcher icon (`@mipmap/ic_launcher`) isn't included — Android
  Studio's "New Project" wizard generates a default one, or add your
  own under `app/src/main/res/mipmap-*`.

## Extending it

- More key codes: add entries to the `RemoteKeyCode` enum in
  `remote.proto` and to `RemoteButton` in `model/RemoteButton.kt`.
- Multiple saved TVs: `RemotePrefs` already tracks a set of paired
  hosts; the UI currently only surfaces the most recent one — extend
  `AppRoot` to let you switch between them.
