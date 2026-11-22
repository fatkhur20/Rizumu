## Rizumu v0.14.6

### Infrastruktur

- Workflow GitHub Actions diperbaiki: tidak lagi memakai `android-actions/setup-android@v3` yang gagal karena paket `tools` sudah dihapus. CI sekarang memakai Android SDK bawaan runner GitHub.
- `testReleaseUnitTest` tetap berjalan otomatis pada push/PR ke `main` dan tag `v*`.
- Tidak ada perubahan perilaku aplikasi.

**APK:** `Rizumu-0.14.6.apk` (`17134375` bytes)

- SHA-256: `5354238f8e0c8cb30facb96183b1f434d4cad3e9dc76e9801aaf1c76e1b42495`
- MD5: `a2712baf6ad1a7f67ac6ea438e91dfc8`
- Package `com.rizumu.music`, versionCode 40, minSdk 26, targetSdk 34; signing cert unchanged.
