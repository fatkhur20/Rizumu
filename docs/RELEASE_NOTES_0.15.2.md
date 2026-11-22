## Rizumu v0.15.2

### Perbaikan penting

- Tombol opsi (titik tiga) pada lagu di halaman **Cari** sekarang benar-benar membuka sheet opsi.
- Akar masalahnya bukan lagi area klik baris, melainkan halaman Cari/Library tidak punya `Box` akar seperti Home. Akibatnya `SongActionSheet` yang dipancarkan sebagai sibling `Column` tidak menimpa halaman dan tidak bisa diakses. Sekarang Cari dan Library dibungkus `Box(Modifier.fillMaxSize())`.
- Perbaikan isolasi klik baris dari 0.15.1 tetap dipertahankan.

**APK:** `Rizumu-0.15.2.apk` (`17150759` bytes)

- SHA-256: `dc3ecf654e83dee431c83bab22b4bdbb8086a95d5a1a3345899cf618c6b3236a`
- MD5: `1fe801c484065acacbb809f523650b3b`
- Package `com.rizumu.music`, versionCode 43, minSdk 26, targetSdk 34; signing cert unchanged.
