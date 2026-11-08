# Changelog

Each `vX.Y.Z` tag publishes the TV and Android APKs; its section below becomes the release notes.

## Unreleased

### ✨ New
- **Series and anime in the player**: Previous, Next and an Episodes list (seasons, pictures, names) right in the player; the next episode starts by itself at the end. Picking one opens it on the site and the player follows.
- **Sub / Dub button** in the player for anime: one tap switches, and each site remembers your pick.
- **Movies start by themselves**: a movie opened on its site's own page has its Play pressed for you.
- **Episodes open directly** on 34 TV sites and 22 anime sites (no searching or clicking on the site); on other sites the app presses the episode in the site's own list.
- **Sites without a known address open the title's page too**: the app searches the site, then opens the matching result (name, and year when shown) instead of leaving you on the results.
- **Anime opens on anime sites only**, the ones that open episodes directly first; Watch starts Episode 1 there.
- **Site nag pop-ups closed by themselves**: "Join our Discord", "What's new", donate, ad-block and new-domain notices.
- **Skip intro, recap and credits**, for series, anime and movies: a button over the picture during the intro or recap; it takes the remote's focus, so OK presses it. At the credits it offers the next episode and plays it after 10 s; with no next one it skips the credits. Times come from TheIntroDB and AniSkip; without them the last 30 s of an episode offer the next one.
- **Settings, Skip intros**: intros and recaps go by themselves when their times are known.
- **Subtitles in any language**: pick a language first (Arabic and others that never showed now do), then a file; files are converted so every script shows right. They also line up with the speech by themselves when a file is off (a release with a recap can be minutes off).
- **Hold to speed up**: hold the right side of the picture for 2x, the left side to go back at 2x.

### 🐞 Fixed
- **Servers** on Cinejoy showed the player's two "10" skip buttons; it now finds the real server list (it opens a moment later).
- **Sites that pass every stream through one proxy address** (NekoWatch and others): a new episode or the Dub version now goes to the native player instead of being taken for the old stream.
- **App closed on start on Android 8 and 9** (many TV boxes, Fire OS 7, older Sony TVs) because of the cast service; some sites could also close the app there.
- **Subtitle settings in landscape**: the panel scrolls and Reset / Done stay on screen.

### 🔧 Changed
- **WARP connects on more networks**: it looks for Cloudflare's sign-up address in several places at once (built-in, your network's DNS, public DNS, DNS over HTTPS), tries three times, and says why it failed. If Cloudflare is limiting sign-ups from your network (HTTP 429) it stops at once instead of retrying.
- **Settings, WARP DNS**: type your internet provider's DNS (from your router) if WARP can't connect; it is asked first.
- **Movies, TV, Anime and the update alert no longer wait for WARP**: they go out directly first, and use WARP only if that fails.
- **Player buttons**: one compact row of icons (Previous, Next, Quality, Audio, Sub / Dub, Episodes, Servers, Subtitles, More, Cast, Full screen); what is picked (quality, audio, subtitles, server) shows under the title. Speed, boosts, zoom and Restart moved into More. New icon set.
- **Native player** button on a site's page is on the right, and shows as soon as a stream is found.
- **Subtitles**: the style entry is called Settings.
- **On a TV**, the player button the remote is on shows its name above it.

## 2.17.1

### 🐞 Fixed
- **Dialog boost** made everything quieter: it now lowers rumble and bass and lifts the level, so voices come out louder. Turning a boost off brings the sound fully back at once.

## 2.17.0

### ✨ New
- **DLNA / UPnP casting**: smart TVs from Samsung, LG, Sony, Kodi and others show up in the Cast list ("Name - DLNA"), through the phone like Chromecast.
- **Fire TV**: install with Downloader code 9828006; the link `releases/latest/download/FMHYPlay-TV.apk` always gets the newest version.
- **Stalker portals**: add your IPTV provider's portal address and MAC address (MAG login) next to Xtream; channels get their own row.
- **PunchPlay**: Settings, PunchPlay connects with a code at punchplay.tv/link; movies and episodes you watch are tracked there, like Simkl.
- **IPTV filters and pins**: channel lists have Category and Language chips (movies, TV, kids and news first; Hindi, English, Tamil for India). The pin button keeps a list with its chips (India · News · Hindi) as its own row on the IPTV tab.

### 🔧 Changed
- **Audio language**: the language you pick on the player's Audio button is where every next video starts, when it has it (then your device's language), instead of the stream's default, often English. The list shows language names, not "stream_4".

## 2.16.1

### 🐞 Fixed
- **Dialog boost** is off by default again: it caused sound problems on some devices. Turn it on in Settings or with its button in the player.
- **TV tabs** show a ring when the remote is on them.

## 2.16.0

### ✨ New
- **Cast to TV**: the Cast button in the player sends the video to a TV on your Wi-Fi, from the same spot. The phone becomes the remote (play/pause, seek, volume keys).
- **FMHYPlay on the TV** (recommended): plays every stream the phone can, even when the app is closed. On the TV: Settings → Cast to this TV → allow once.
- **Chromecast**: Google TV and Cast TVs work without FMHYPlay on the TV. The phone relays the stream, so keep it on the same Wi-Fi.
- **Your language**: Hindi, Spanish, Portuguese, Indonesian, Arabic and German, picked from the device.

### 🔧 Changed
- **Dialog boost** is on by default.
- **Pop-ups**: a link a site opens in a new window now opens in the same tab (Back returns). Ad pop-ups are dropped, and the link you tapped opens anyway (CricHD's Watch works in one tap).
- **Continue watching** shows the catalog poster for titles opened from Movies, TV or Anime, also on sites without their own.

### 🐞 Fixed
- Volume boost and Dialog boost had no effect on many phones and TVs.

## 2.15.0

### ✨ New
- **New Home**: search across the top, tabs, Continue watching, Favorites, and New + Popular for Movies, TV and Anime.
- **One search** for movies, series, channels and sites.
- **Resume anywhere**: carries on where you stopped, on any site or server. Restart button in the player.
- **Sound**: Volume boost and Dialog boost, with defaults in Settings.
- **Subtitles**: color, background, auto subtitles in your language, remembered picks.
- **What's new** in the update alert, **WARP log** to share, **Address bar** on sites, **cursor looks** on TV.

### 🔧 Changed
- Phone player follows the rotation lock and has a Full screen button.
- IPTV Working row shows channels; favorites use a heart.

## 2.14.3

- Search results fill the page as a poster grid.

## 2.14.2

- Faster search, with a spinner; accents don't matter.

## 2.14.1

- Fixed: Check channels showed "0 offline" after a playlist change.

## 2.14.0

### ✨ New
- **Simkl**: marks movies and episodes watched.
- **Xtream Codes** login for IPTV; playlists get names and their own rows.
- **Update check** on start, with Install on Home.
- **Subtitles panel**: size, position and timing.

### 🔧 Changed
- Fewer permissions; WARP helper rebuilt so antivirus apps stop flagging it.

## 2.13.0

- Episodes are picked on the site; the native player follows by itself.
