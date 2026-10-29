# GPS hotfix after 0.13.35 (29 October 2026)

Reported on Samsung S23 Ultra: crash on resume, unreliable offline recording, zero-duration paused route with no exit. No actual device stack trace has been supplied. The precise cause of that crash is not definitively confirmed.

## Code findings and changes

- `onCreate` previously constructed storage and notification infrastructure outside a catch; route decoding in `onStartCommand` was also outside the catch. An exception here could terminate the process before foreground promotion. Promotion now precedes database reads, with guarded launch/initialization and cleanup.
- Call-site preflight checks precise permission and GPS provider before `startForegroundService`; permission/provider changes after preflight are handled in the service. Promotion failures, registration, checkpoint, pause and teardown are locally diagnosed. No automatic background restart (`START_NOT_STICKY`).
- Resuming a persisted RUNNING route after process loss previously allowed an exception path to checkpoint from `tickAt=0`. A nullable service-owned clock now counts only this instance's actual recording time. New segments never bridge pauses/process loss.
- Cleanup operations cannot throw a second exception while handling the first. Repeated START for the same running activity does not add timers, listeners or segments.
- Satellite `LocationManager.GPS_PROVIDER` is the sole recording provider, independent of network connectivity and map tile requests. Waiting text explains slower offline first fix. Existing offline route and optional online OSM map remain unchanged.
- Paused/interrupted activities, including zero seconds / zero distance, have a confirmed discard action. It deletes only the selected `gps_tracks` row with no `run_id`, rejects finished/active tracks and touches no history tables. Empty activity still cannot be saved as a finished workout.
- Optional JSON `runId:null` is accepted as absent. Schema 13, backups and existing data remain compatible; no destructive migration.
- GPS screen offers selectable local diagnostics (40 entries): event, SDK/app version, exception class and up to 8 stack frames. No exception messages, coordinates, activity IDs, personal data or automatic uploads. This preference file is outside the existing system-backup allowlist.
- UI strings translated in all ten languages.

## Automated and physical verification

New regression cases cover repeated resume, GPS disabled, malformed/missing commands, storage decoding failure, clock recovery after process interruption, no active internet network with simulated GPS fixes, inaccurate fixes, interruption segment gaps, bounded private diagnostics, JSON null compatibility, single-route discard and its zero-duration confirmation UI. Existing migration/backup/history/map tests remain enabled.

Local Python: 33 tool tests + 6 migration tests passed. Android tests/build and signing results to be appended after CI.

Physical checks are still REQUIRED; Robolectric does not prove satellite acquisition, Android system foreground timeouts or Samsung power management:
1. Update over 0.13.35 without uninstalling; verify existing history/backup and original signer.
2. Discard only the stuck empty record, cancelling the dialog first to verify it is retained.
3. Outside, disable Wi-Fi/mobile data but keep Location enabled; start and wait for a satellite fix. Verify time progresses while waiting, then distance records. Offline route works; unavailable online tiles must not stop recording.
4. Pause/resume repeatedly, including a short wait after pause. Finish/save, check history and map.
5. Lock screen for 5–10 minutes while walking; unlock and verify distance/time. Compare against a known route.
6. Disable Location/revoke precise permission during recording; confirm recoverable paused state and explicit resume after restoring permission.
7. Simulate process death only on a disposable test profile/device, not the user's data: last checkpoint preserved, elapsed time excludes interruption, new route segment on explicit resume. Force-stop is not expected to auto-resume.
8. If a crash recurs, copy local GPS diagnostics; obtain Android crash log if possible. Do not assert the Samsung crash is fixed solely from passing unit tests.
