## Rizumu v0.11.1

### Yang baru

- Semua bottom sheet memakai `GlassSheet` bersama: blur Haze 40dp, tint gelap netral, scrim gelap, sudut atas 28dp, drag handle, padding nav bar, serta animasi spring yang menghormati reduced-motion.
- Dock navigasi/player disembunyikan saat sheet terbuka agar sheet selalu berada di atas dan tidak menutupi teks.
- Equalizer ditata ulang: judul dan badge “Segera hadir” di kiri, sakelar di kanan, chip preset satu baris, dan slider vertikal setinggi 160dp.
- Timer tidur memakai sheet yang menutup kontrol player, chip glass yang sama, dan tombol “Batalkan timer” bergaya sekunder.
- Info berpindah ke `GlassSheet`; kartu aksi seragam, tombol “Tutup” glass penuh, dan “Bersihkan data lokal” memakai warna peringatan dengan dialog konfirmasi.
- Antrian memakai tint netral, highlight putih untuk lagu aktif, kolom nomor 28dp, swipe-to-delete, drag handle untuk reorder (long-press), dan aksi “Bersihkan antrian” di header.
- Tombol lirik/EQ/timer/antrian di Now Playing memakai tombol glass netral dengan ikon putih.

### Catatan

- Logika playback, lirik, dan persistensi tidak diubah. Satu-satunya tambahan playback adalah `moveQueueItem` agar drag reorder antrian benar-benar mengubah urutan player.
- Aksi hapus antrian tersedia lewat swipe atau menu titik tiga; tidak ada tombol hapus permanen di setiap baris.
- Verifikasi visual blur/kontras dengan cover merah-kuning terang, swipe/drag antrian, dan perilaku sheet di atas dock masih perlu dicek manual karena perangkat build tidak memiliki adb.

**SHA-256:** `bc9970c2cd314c046f6b7abf37bf967f55877f2c091a6bd7cdd492566104e197`
**APK:** `Rizumu-0.11.1.apk` (`16973822` bytes)
