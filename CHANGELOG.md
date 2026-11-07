# Changelog

Each release is a tag `vMAJOR.MINOR.PATCH` that publishes both APKs. The notes for a version are its section here. Earlier releases are on the Releases page.

## 2.15.0

- New Home: the name, WARP, Ad block, Search (as wide as the screen allows) and voice across the top; the tabs (Settings is one of them again) below it on a TV, at the bottom on a phone. Then Continue watching, Favorites (once you have some), and Movies, TV and Anime, each with New (newest releases first) and Popular (anime: what is trending on Kitsu).
- One search for everything, from the top bar on every tab: movies and series, IPTV channels (a Channels row; All opens every match), sites and what you watched.
- Settings, Address bar: shows the page's address on top of sites; tap it (TV: click it with the cursor) to type another address or a search.
- New: the newest releases, newest first, only what is already out (a movie from its cinema release, a series from its first episode). New anime is this year's airing anime, from Kitsu (also at the top of the Anime tab). The Movies and TV tabs open on New; Popular and the genres are chips next to it.
- Search shows movies and series in two sideways rows (newest first, each poster labelled Movie, TV Series or Anime), so channels and sites below stay in view.
- Continue watching remembers where you stopped: the player carries on there whatever site or server you open the title on (the newest spot wins), also another day. Restart, the first button in the player, starts over. A line along the bottom of the card shows how much you watched. Changing server no longer adds a second card for the same title.
- Update alert: What's new shows the notes of every version since yours, before you install.
- Volume boost (up to +12 dB) and Dialog boost (louder voices, less rumble): set the default in Settings, Player; their own buttons in the player change them for the video you are watching. The Audio button is for the language only.
- Subtitles panel: Color (white, yellow, cyan, green, pink, orange) and Background (off to 100%). Long lines wrap at three quarters of the screen instead of running edge to edge.
- Subtitles you pick from OpenSubtitles come back by themselves when you play that movie or episode again, and your language is listed first. Settings, Auto subtitles (off by default) starts every movie and episode with subtitles in your language.
- Settings, WARP log: what WARP tried and why it failed, with Share and Copy, to send with a bug report.
- Phones: the player follows the phone's rotation setting (it stays upright while rotation is locked) and its Full screen button turns it to landscape and back. Brightness and volume swipes show a vertical bar.
- IPTV: the Working row shows the channels themselves, not just an All tile. Favorites have a heart, and Add a site is also in Settings, Sites.
- TV cursor: Settings, Cursor look (dot, arrow, crosshair, ring, big yellow, or a pizza, dino, ghost and more) and Cursor size. The cursor hides while a video plays and comes back when you press a key.

## 2.14.3

- Search shows movies and series as a poster grid that fills the page (6 a row on TV, 3 on phones), like the Movies tab, with up to 20 of each instead of one row.

## 2.14.2

- Search finds movies and series much faster: it asks the catalog for both at the same time on its own connection, instead of waiting behind poster downloads. Titles from every country are in it.
- A spinner shows while movies and series are being searched, and "Nothing found" no longer appears before they arrive.
- Titles whose name has the words you typed come first, and accents don't matter ("amelie" finds Amélie, also for sites).

## 2.14.1

- Settings, Check channels now showed "0 offline" right after you added or removed a playlist. It now shows the real count.

## 2.14.0

- Simkl: Settings, Simkl, Connect shows a code; enter it at simkl.com/pin (sign in or make a free Simkl account there). After that, movies and episodes opened from Movies, TV or Anime and watched in the native player are marked watched on Simkl once you pass 80%, and Simkl's "watching now" follows play and pause.
- Fewer permissions: location, camera, microphone and audio settings are gone. Sites that ask for them are told no. Voice search still works: it uses the system's speech app, which has its own microphone permission.
- Subtitles panel: size, position and timing, each with − and + buttons, in the player (Subtitles button, last entry) and in Settings (size and position). Size and position are remembered; timing (+ shows subtitles later, − earlier, 0.1 s a press) is for the video you are watching.
- Xtream Codes: Add a playlist has an Xtream login button (server, username, password). The provider's live channels and categories show on the IPTV tab, and the background channel check never tests them, so the account's connection limit is not used up.
- Playlists get a name when you add them, and each one has its own row on the IPTV tab. Single stream links share a My streams row.
- Update check: FMHYPlay looks for a newer release on GitHub when it starts (at most every 12 hours) and shows one line on Home with an Install button. Settings has Check for updates and a switch to turn the check off. Android asks you to confirm each install, and only an update signed with the project key installs.
- The WARP helper is now built as a normal Android program with the NDK. Some antivirus engines flagged the old static 32-bit build as a "flooder".

## 2.13.0

- Episodes are now picked in the site's own player. The native player no longer has an Episodes button: press Back, choose the episode on the site, and the new stream opens in the native player by itself.
- A stream that has gone to the native player stays ignored until the site starts a different one, so the old video does not reopen while the next episode loads.
- The season and episode line no longer shows the catalog's episode after you have switched to another one on the site.
