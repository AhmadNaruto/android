<p align="center">
  <img src="docs/banner.png" alt="FMHYPlay" width="560">
</p>

<p align="center">
  <b>Any streaming site, a real player.</b><br>
  A TV-first Android app that opens the sites you choose, finds the video they play, and plays it in a native hardware-accelerated player.
</p>

<p align="center">
  <a href="https://github.com/fmhyplay/android/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/fmhyplay/android?color=ff3b5c"></a>
  <a href="https://github.com/fmhyplay/android/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/fmhyplay/android/build.yml?branch=main"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3ddc84">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-blue">
</p>

<p align="center">
  <a href="https://fmhyplay.github.io/android/">Documentation</a> ·
  <a href="https://github.com/fmhyplay/android/releases/latest">Download</a> ·
  <a href="https://fmhyplay.github.io/android/guide.html">User guide</a> ·
  <a href="https://fmhyplay.github.io/android/build.html">Build from source</a> ·
  <a href="https://buymeacoffee.com/rohit267">Support</a>
</p>

<p align="center">
  <img src="docs/screenshots/player.jpg" alt="The native player" width="760">
</p>

## Why

Streaming sites are built for a desktop browser with a mouse. On a TV remote they are painful, they are full of pop-ups, and their embedded players stutter on weak hardware. FMHYPlay keeps the site (so you keep every title and server it offers) but takes over the part that goes wrong:

- It **finds the video stream** the page starts and plays it in ExoPlayer with the hardware decoder, with nothing painted over the picture.
- It **drives the site for you**: the player's Servers and Quality buttons press the site's own buttons, so you do not go back to the page to switch.
- It **blocks ads and pop-ups**, **tunnels the app's own traffic** through Cloudflare WARP when you want it, and works with a **TV remote** and with **touch**.

## Features

**Cast to TV**
- The **Cast** button in the phone's player sends the video to a TV on the same Wi-Fi, from the same spot. The phone becomes the remote: play, pause, seek, ±10 s and TV volume (the phone's volume keys work too)
- **TV with FMHYPlay (recommended)**: the TV plays the stream itself, with the site's headers and cookie, WARP, ad blocking and subtitles. It opens even when FMHYPlay is closed on the TV
- **Chromecast, Google TV and TVs with Cast built in**: no app needed on the TV; the phone relays the stream so sites still accept it
- **DLNA / UPnP smart TVs** (Samsung, LG, Sony, Kodi and others): same relay; works best with MP4 and TS streams, since not every TV plays HLS

<p align="center"><img src="docs/cast.svg" alt="Cast from the phone to a TV on the same Wi-Fi" width="760"></p>

**Native player**
- Hands off automatically when a page starts an HLS or DASH stream; falls back to the page if the stream cannot be played
- Replays the page's Referer, Origin and cookies, so streams that check them still play
- **Resumes where you stopped on any site or server**, with a Restart button
- Quality list with frame rate (for example 1080p60), audio and subtitle tracks, speed and zoom modes
- **Servers and Quality buttons that control the site's own player**, including players inside cross-origin iframes
- **Volume boost** (up to +12 dB) and **Dialog boost** (clearer voices, on by default)
- **OpenSubtitles** built in, remembered per title, optional auto subtitles in your language; size, position, color, background and timing
- **Simkl** scrobbling (optional): titles you finish are marked watched
- Phone: follows the rotation lock with a Full screen button; swipe for brightness and volume, double-tap to skip 10 seconds

**Browsing**
- Home with Continue watching, Favorites, and **New and Popular** for Movies, TV and Anime (from Cinemeta and Kitsu)
- **One search** for movies, series, IPTV channels and sites, by typing or by voice
- A directory of streaming sites that tracks the [FMHY](https://github.com/fmhy/edit) lists; opens a title's own page on many sites
- **Add your own site**, an optional address bar, dark pages, a cursor for the TV remote (with funny and useful looks)
- Pop-ups that are real links open in the same tab (Back returns); pop-up ads are dropped

**Your language**
- English, Hindi, Spanish, Portuguese, Indonesian, Arabic (right to left) and German, picked from the phone's or TV's language

**Network and privacy**
- **In-app Cloudflare WARP**: a userspace WireGuard tunnel for the app's traffic only, with no VPN permission and no root; LAN addresses stay direct. A WARP log to share when it fails
- Domain-based ad, tracker and pop-up blocking, plus redirect blocking
- No FMHYPlay account, no analytics, no telemetry. Updates come from this repository's releases, with What's new before you install

**IPTV**
- More than 11,000 public channels from [iptv-org](https://github.com/iptv-org/iptv), grouped by category and country, with logos
- A background check hides channels that are offline, and a Working row shows the ones that played
- **Add your own playlist, a single `.m3u8` stream link, an Xtream Codes login or a Stalker portal (MAC address)**; each playlist gets its own row
- Favorite channels with a heart; channels also show up in the one search

**Engineering**
- Plain Java and SDK command-line tools, **no Gradle**, roughly 11 MB per APK
- Reproducible CI build and signed releases from a tag

## Screenshots

<table>
  <tr>
    <td><img src="docs/screenshots/home.jpg" alt="Home"></td>
    <td><img src="docs/screenshots/sites.jpg" alt="Sites and catalog"></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/iptv.jpg" alt="IPTV channels"></td>
    <td><img src="docs/screenshots/quality.jpg" alt="Quality list"></td>
  </tr>
</table>

<p align="center">
  <img src="docs/screenshots/phone-home.jpg" alt="Phone home" height="420">
  &nbsp;
  <img src="docs/screenshots/phone-cast.jpg" alt="Phone as the remote while casting" height="420">
  &nbsp;
  <img src="docs/screenshots/phone-hindi.jpg" alt="Home in Hindi" height="420">
</p>

<p align="center">
  <img src="docs/screenshots/phone-player.jpg" alt="Phone player" width="760">
</p>

## Install

Download the APK for your device from [Releases](https://github.com/fmhyplay/android/releases/latest):

| File | For |
| --- | --- |
| `FMHYPlay-TV-vX.Y.Z.apk` | Android TV, Google TV, Fire TV, boxes (adds the TV launcher entry) |
| `FMHYPlay-Android-vX.Y.Z.apk` | phones and tablets |

Allow installs from your file manager or browser, open the APK, done. On a TV, `adb install -r FMHYPlay-TV-vX.Y.Z.apk` works over the network. Updates install over the old version because releases are signed with the same key. Check a download with `SHA256SUMS.txt` from the release.

Requires Android 8.0 (API 26) or newer, arm64 or 32-bit ARM.

### Fire TV

1. On the Fire TV: *Settings → My Fire TV → About*, press OK on the device name 7 times (developer options on).
2. Install **Downloader** (by AFTVnews) from the Amazon Appstore.
3. *Settings → My Fire TV → Developer options → Install unknown apps* → turn on **Downloader**.
4. In Downloader, type the code **`3116054`** or `https://github.com/fmhyplay/android/releases/latest/download/FMHYPlay-TV.apk`, then *Go → Install → Open*.

Fire OS 7 or newer (Fire TV Stick 4K and later). The app updates itself afterwards. That link always gets the newest TV APK (`FMHYPlay-Android.apk` for phones).

## Build from source

```sh
git clone https://github.com/fmhyplay/android
cd android
warp/build.sh      # builds the WARP helper (needs Go and the Android NDK)
./build.sh         # needs an Android SDK (platform 34, build-tools) and JDK 17+
```

The APKs appear in the project folder. Details, signing and the CI setup are in the [build guide](https://fmhyplay.github.io/android/build.html).

## How it works

1. The page loads in a WebView. A small script (`assets/frame.js`) runs in every frame, including cross-origin embeds.
2. The WebView's requests are watched for HLS and DASH playlists; some sites hide them behind URLs without an extension, so downloads are also recognised by their first bytes.
3. When a stream shows up, the page is paused and the stream goes to ExoPlayer with the same headers and cookies the page used.
4. The same script reads the page's server and quality lists. The player's buttons send a click back to it, the page switches, and the new stream is handed over the same way.

## Support

FMHYPlay is free and made in spare time. If it is useful to you: [Buy me a coffee](https://buymeacoffee.com/rohit267) or [PayPal](https://paypal.me/champdev).

## Legal

FMHYPlay is a browser and a media player. It does not host, store, index or distribute any video, and it ships no list of streams. The sites it opens, the IPTV playlists it reads and the content they serve are provided by third parties; what you open and play is your responsibility, and you should only use content you have the right to watch. See [THIRD_PARTY.md](THIRD_PARTY.md) for the components and data sources it builds on. FMHYPlay is not affiliated with or endorsed by FMHY, Cloudflare, Stremio, OpenSubtitles, iptv-org or any streaming site.

## License

[MIT](LICENSE)
