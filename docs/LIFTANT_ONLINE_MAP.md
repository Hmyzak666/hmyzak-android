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

## Calories beside activities

Cardio, HIIT and completed strength cards (including Home previews and strength detail) show labelled estimates of active and total kcal. All use the same row conversion, dated-weight selection and stored intensity as the energy overview. Intensity is observable state, refreshed after edits and restore. Missing historical weight or duration yields an explanation, never an invented number. Existing ten-language labels are reused.

CI 56 on source `86fe7b18aef6c5a2ec71eb19c20acd00670247e0` passed: 207 debug + 207 release tests, 39 Python checks, lint 0 errors / 68 warnings; debug APK and release AAB built. CI run: https://github.com/Hmyzak666/hmyzak-android/actions/runs/40741295269 .

Regression coverage adds projection/date-line/poles/viewport fitting, fresh disk tile cache, exact HIIT seconds, historical weight selection, reactive intensity changes, missing weights and unchanged history. Physical device checks above remain necessary.

Signed APK 35 succeeded: https://github.com/Hmyzak666/hmyzak-android/actions/runs/40742341476 . Version 0.13.35 / 13035, 9,593,960 bytes; original certificate independently checked by apksigner. APK SHA-256 `a77644dc06e164cfaa9ad9c341c0b2d3d8c4cca8bc00f5230428449412ee783c`. Test Pro remains explicitly enabled in this test APK. Publication skipped; no main merge or Play publication.
