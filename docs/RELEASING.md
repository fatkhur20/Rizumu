# Rizumu release procedure

1. Pick the next version and update **both** `versionCode` and `versionName` in
   `app/build.gradle.kts`. Never change `applicationId` (`com.rizumu.music`) or the
   signing key after v0.14.0.
2. Confirm the update-checker repository. Defaults live in `app/build.gradle.kts`
   (`fatkhur20/Rizumu`); override with `RIZUMU_GITHUB_OWNER` / `RIZUMU_GITHUB_REPO` in
   `gradle.properties` or `~/.gradle/gradle.properties` when needed.
3. Run the unit tests:
   ```bash
   ./gradlew --no-daemon testReleaseUnitTest
   ```
4. Build the signed release APK:
   ```bash
   ./gradlew --no-daemon assembleRelease
   ```
   The output is `app/build/outputs/apk/release/app-release.apk`.
5. Verify the signing certificate and hash:
   ```bash
   "$ANDROID_HOME/build-tools/<version>/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
   sha256sum app/build/outputs/apk/release/app-release.apk
   ```
6. Create and push the tag:
   ```bash
   git tag -a v<version> -m "Rizumu v<version>"
   git push origin v<version>
   ```
7. Create a GitHub Release for that tag:
   - title: `Rizumu v<version>`
   - notes: copy `docs/RELEASE_NOTES_TEMPLATE.md` and fill it in
   - asset: `app/build/outputs/apk/release/app-release.apk`
   - include the SHA-256 line exactly; the app reads it from the release body
   - publish the release as `latest`
8. Smoke-test the update path from the previous installed version:
   - app launch auto-check (or Info → Cek pembaruan)
   - release notes render in the glass dialog
   - download progress reaches 100%
   - SHA-256 verification passes
   - unknown-sources permission opens Settings when needed
   - installer opens and the update installs over the old app
   - Disukai, Riwayat, and Playlist data remain
9. Smoke-test R3 offline/backup behavior on a real device:
   - download a song, confirm progress reaches 100% in Library → Unduhan
   - switch to airplane mode and play the downloaded song from Library → Unduhan
   - delete the download and confirm the private file is gone
   - change speed to 0.75x/1.5x and confirm pitch/tempo behavior is acceptable
   - export a backup, reinstall or clear data, import it, and confirm favorites,
     history, searches, and playlists are merged without wiping existing data
10. Keep the previous APK and its checksum for rollback reference. Do not reuse a version
   tag or replace an asset with a different binary.

## Notes

- The update checker checks at most once every 6 hours. Manual checks bypass that delay.
- “Lewati versi ini” stores the skipped version in DataStore and suppresses automatic
  prompts for that version; a manual check still shows it.
- APKs live in `cache/updates/`. Old files are pruned on launch and a started installer
  file is cleaned up after 10 minutes.
