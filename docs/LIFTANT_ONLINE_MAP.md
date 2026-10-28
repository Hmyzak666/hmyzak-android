# Online map – 2026-10-28

Based on verified APK 0.13.31 / fce56b90. Home latest HIIT preserved.

- Optional explicit Show online map in live GPS and saved routes. Default route preview remains offline.
- Native Web Mercator raster canvas, drag, zoom, fit; route segments never bridge pauses.
- OpenStreetMap standard tiles, HTTPS, identifiable LiftAnt User-Agent and visible linked attribution.
- Only current viewport requested, sequentially after a short pan debounce. No prefetch or offline download feature.
- Seven-day tile cache in Android cacheDir (excluded from backups), bounded to about 64 MiB; conditional request on expiration. No route upload, analytics or account.
- Requests stop when map is hidden or activity lifecycle is not RESUMED. An already pending HTTP call has a five-second timeout.
- Internet permission added for optional map. No background-location permission, GPS service and DB/JSON 13 unchanged.
- Privacy text in all ten locales explains IP/viewed-area disclosure. Old claim that nothing goes to a map server corrected.
- OSM community tiles have no availability guarantee. Before wider Play rollout, review provider capacity/terms and privacy/Data Safety declarations. Tile endpoint is isolated in RouteTiles for replacement.

Sources: https://operations.osmfoundation.org/policies/tiles/ and https://www.openstreetmap.org/copyright (reviewed 2026-10-28).

Phone/tablet verification: open a recorded route, enable map, verify alignment, drag/zoom/fit, pause gaps, loss of internet and retry. Verify live GPS keeps recording while map is hidden, offline and screen locked. No device data deletion required.
