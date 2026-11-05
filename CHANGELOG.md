# Changelog

Each release is a tag `vMAJOR.MINOR.PATCH` that publishes both APKs. The notes for a version are its section here. Earlier releases are on the Releases page.

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
