## Rizumu v0.14.1

### Pencarian regional

- Wilayah yang dipilih di halaman tangga lagu sekarang disimpan dan dipakai juga untuk pencarian.
- Bahasa pencarian mengikuti wilayah tersebut (`ID→id`, `KR→ko`, `JP→ja`, `US→en`, dll.) dan negara memakai kode wilayah. Tidak memerlukan izin lokasi/GPS.
- Hasil dari wilayah terpilih didahulukan. Jika hasil relevan kurang dari 5, hasil global (`en/US`) ditambahkan sebagai fallback dan duplikat dihapus.
- Filter **Semua / Terbaru / 7 hari / 30 hari** tetap berlaku untuk jalur regional maupun fallback.

### Stabilitas pemutaran

- Service pemutaran diberi `android:stopWithTask="false"` agar tidak langsung mati ketika aplikasi dihapus dari daftar recent apps.

**APK:** `Rizumu-0.14.1.apk` (`17134371` bytes)

- SHA-256: `7006942d53ddb549532b97f66bd0b9724bd06aed2b6018d6bc4c24a45b752120`
- MD5: `dbce9a8f961463d2370246f00b16c4ac`
- Package `com.rizumu.music`, versionCode 35, minSdk 26, targetSdk 34; signing cert unchanged.
