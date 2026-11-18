## Rizumu v0.12.1

### Perbaikan

- Auto-return dock sekarang dijadwalkan sebagai job saat tab navigasi ditekan. Timer tidak lagi bergantung pada `LaunchedEffect` ber-key sehingga perubahan status pemutar tidak mereset hitungan.
- Timer tetap 2 detik saat playing dan 15 detik saat pause; swipe halaman dan pembukaan Now Playing membatalkan timer.
- Deteksi swipe halaman memakai `isScrollInProgress` atau `currentPageOffsetFraction` selama layar disentuh, jadi dock tetap pindah ke navigasi walau status scroll pager terlambat.
- Halaman Home sekarang memakai `Box` eksplisit untuk overlay sheet, sehingga tombol info di kanan atas dan sheet Info tidak lagi bergantung pada layout item pager.
- Tombol back dari Cari/Library kembali ke Home; back dari Home tetap keluar.
- Versi `0.12.1` / versionCode 29.

**APK:** `Rizumu-0.12.1.apk` (`16973822` bytes)

- SHA-256: `3d3b1bd6228b7bb34cbb06ecf1b43042d3ca38076a13d6c331b7ae679cd0a887`
- MD5: `ad1b362b16ef0e7e1f1a83bd410a3724`
- Package `com.kyrix.ftxmusic`, versionCode 29, minSdk 26, targetSdk 34; signing cert unchanged.
