## Rizumu v0.14.2

### Pencarian channel satu kata

- Untuk query satu kata pada filter **Semua**, aplikasi mencari channel yang namanya mengandung kata tersebut dan memilih yang paling cocok: nama persis dulu, lalu kemiripan nama, lalu jumlah subscriber terbesar.
- Jika semua kandidat tidak memiliki data subscriber, pencarian channel dilewati dan hasil biasa dipakai.
- Hingga **8 unggahan video** dari channel terpilih disisipkan di atas hasil pencarian, dengan header **“Dari channel: …”**. Shorts dan siaran langsung dilewati.
- Proses ini dibungkus penanganan galat; kegagalan pencarian channel tidak menggagalkan pencarian biasa.

**APK:** `Rizumu-0.14.2.apk` (`17134375` bytes)

- SHA-256: `863f13113a8dc12a5347f5f4e79e6082b1e477be38a99931c1da84e8e20ad23c`
- MD5: `8de58f443863b0cd1acd67ae09144052`
- Package `com.rizumu.music`, versionCode 36, minSdk 26, targetSdk 34; signing cert unchanged.
