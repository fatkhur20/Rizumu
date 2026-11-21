## Rizumu v0.14.0

### Package identity

- Application id dan package source sekarang `com.rizumu.music` (versionCode 34).
- Ini adalah identitas aplikasi baru. Install dengan paket lama `com.kyrix.ftxmusic` tidak otomatis terupdate dan data privatnya tidak dipindahkan. Uninstall aplikasi lama setelah versi ini terpasang.
- Signing certificate tidak berubah.

### Notifikasi & kontrol pemutaran

- MediaSession sekarang didaftarkan ke `MediaSessionService` lewat `addSession()` saat service dibuat, sehingga notifikasi MediaStyle dan kontrol play/pause/next/prev dari luar aplikasi bisa muncul.
- Service juga mendeklarasikan action `android.media.browse.MediaBrowserService` untuk kompatibilitas penemuan sesi.

**APK:** `Rizumu-0.14.0.apk` (`17134351` bytes)

- SHA-256: `b70d0ec7ae7f7669c8893898eefb77fa032edc234ce813338c8c3d492e027889`
- MD5: `527b0b224046247e7a64a67a82f33150`
- Package `com.rizumu.music`, versionCode 34, minSdk 26, targetSdk 34.
