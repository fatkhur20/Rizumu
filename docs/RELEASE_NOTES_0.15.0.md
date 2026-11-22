## Rizumu v0.15.0

### Listening stats (R4)

- Mencatat jumlah putar dan durasi dengar per lagu, artis, dan hari.
- Durasi dihitung dari perubahan posisi pemutaran (maks 2 detik per sampel), diabaikan saat dijeda/ buffering, dan seek tidak dihitung. Data disimpan tiap 30 detik atau saat pindah lagu.
- Ringkasan “Total dengar”, jumlah lagu, dan artis teratas tampil di halaman Library.
- Riwayat pemutaran menampilkan badge jumlah putar di sebelah setiap lagu.
- Backup JSON sekarang schema v2 dan menyimpan statistik; backup schema v1 lama tetap bisa diimpor.

**APK:** `Rizumu-0.15.0.apk` (`17150759` bytes)

- SHA-256: `9163792f420c0f4e03e1e1e65ff9301ca0ce3d970ceb96590f2fcb7e79e2a52a`
- MD5: `4a5ef4d44f380c6f2df24a5678eaa795`
- Package `com.rizumu.music`, versionCode 41, minSdk 26, targetSdk 34; signing cert unchanged.
