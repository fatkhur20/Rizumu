## Rizumu v0.11.0

### Yang baru

- Unduhan offline: unduh lagu dari menu lagu, pantau progres di Library → Unduhan, lalu putar tanpa koneksi. Antrean memakai WorkManager dan file disimpan di penyimpanan privat aplikasi.
- Kecepatan putar 0.75x, 1.0x, 1.25x, dan 1.5x dari Now Playing.
- Backup data pengguna: ekspor JSON lewat pemilih file Android dan impor kembali dengan mode gabung, bukan menghapus data lama.

### Catatan

- Backup berisi Disukai, Riwayat, pencarian, dan Playlist. File audio serta catatan unduhan tidak dimasukkan ke JSON.
- Impor backup bersifat aditif: data yang sudah ada dipertahankan dan duplikat dilewati.
- Crossfade belum ditampilkan karena membutuhkan implementasi dua player yang stabil; fitur ini tidak diaktifkan diam-diam.
- Unduhan memerlukan koneksi saat memulai. Jika proses dihentikan sistem, WorkManager mencoba lagi.

**SHA-256:** `cced72a5175cfd6d829f4ae0ababa604df2d7b12372333039c57806b7142279e`
**APK:** `Rizumu-0.11.0.apk` (`16957442` bytes)
