# Third-party software and data

FMHYPlay is MIT licensed (see `LICENSE`). It includes, or downloads at build or run time, the following.

## Libraries in the app

| Component | License | Used for |
| --- | --- | --- |
| [AndroidX Media3](https://github.com/androidx/media) (ExoPlayer) 1.11.1 | Apache-2.0 | the native player (`libs/media3-*.jar`) |
| [AndroidX WebKit](https://developer.android.com/jetpack/androidx/releases/webkit) 1.12.1 | Apache-2.0 | WebView features (`libs/androidx-webkit-*.jar`) |
| [Guava](https://github.com/google/guava) (Android flavor) | Apache-2.0 | required by Media3 |
| [wireguard-go](https://git.zx2c4.com/wireguard-go) | MIT | the in-app WARP tunnel (`warp/`, built into `libwarp.so`) |
| [gVisor](https://github.com/google/gvisor) netstack | Apache-2.0 | userspace networking for the tunnel |
| [golang.org/x/crypto, x/net, x/sys, x/time](https://pkg.go.dev/golang.org/x) | BSD-3-Clause | the tunnel |

## Data fetched at build or run time (not stored in this repository)

| Source | What for |
| --- | --- |
| [FMHY](https://github.com/fmhy/edit) | the list of streaming sites (refreshed by the app every 12 hours) |
| [HaGeZi DNS blocklists](https://github.com/hagezi/dns-blocklists) (GPL-3.0) | the ad and pop-up domain list (refreshed weekly) |
| [iptv-org](https://github.com/iptv-org/iptv) | the public channel playlist on the IPTV tab |
| [Cinemeta](https://v3-cinemeta.strem.io) | posters, titles and episode lists |
| OpenSubtitles through the [Stremio addon](https://opensubtitles-v3.strem.io) | subtitles in the native player |
| Cloudflare WARP | the optional tunnel; registered through Cloudflare's own endpoint at first start |

FMHYPlay is not affiliated with or endorsed by FMHY, Cloudflare, Stremio, OpenSubtitles, iptv-org or any streaming site.
