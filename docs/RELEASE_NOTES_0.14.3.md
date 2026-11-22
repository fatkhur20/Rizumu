## Rizumu v0.14.3

### Perbaikan hasil pencarian

- Pengaya hasil dari channel satu kata dihapus; ternyata membuat hasil tidak fokus (misalnya memasukkan unggahan channel “Romi” yang bukan yang dicari).
- Peringkat sekarang memprioritaskan **artis yang cocok**, bukan kecocokan judul saja: lagu dari artis yang namanya mengandung kata kunci naik ke atas.
- Judul/artis yang sama kini dihapus duplikatnya, jadi hasil tidak berulang.
- Filter **Semua / Terbaru / 7 hari / 30 hari** dan fallback regional/global tetap berlaku.

Contoh live: pencarian `romi` sekarang menampilkan lagu-lagu **Romi Jahat** di enam hasil teratas.

**APK:** `Rizumu-0.14.3.apk` (`17134375` bytes)

- SHA-256: `4186f33f8807cd9936c499c25cb24ccc58f21fb1ea127f6fd6c2689bd9de6612`
- MD5: `734c18382772e86f9abb39cd4c22c7d2`
- Package `com.rizumu.music`, versionCode 37, minSdk 26, targetSdk 34; signing cert unchanged.
