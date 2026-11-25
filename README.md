# Papan Skor

Aplikasi papan skor dua kubu untuk Android. Ringan (sekitar 200 KB), jalan tanpa internet, dan tanpa iklan.

- Landscape dan potrait
- Nama, warna, dan emoji tiap kubu bisa diganti
- Kalkulator untuk menambah atau mengurangi poin
- Max poin per set bisa diatur atau dimatikan
- Hitungan set, riwayat set, undo, tukar sisi, timer, dan peluit
- Skor tetap tersimpan walau aplikasi ditutup

## Cara pasang aplikasi

Butuh HP dengan **Android 11 atau lebih baru**.

### 1. Unduh APK

Buka halaman [Releases](https://github.com/AkbarLingga/ScoreBoard/releases/latest) lewat browser di HP, lalu unduh **PapanSkor.apk** di bagian **Assets**.

### 2. Buka file APK

Buka **File Saya** (My Files) atau **Files** → **Download**, lalu ketuk **PapanSkor.apk**.

### 3. Izinkan pemasangan

Karena aplikasi ini tidak dari Play Store, Android akan meminta izin dulu:

1. Ketuk **Setelan** (Settings) di peringatan yang muncul.
2. Nyalakan **Izinkan dari sumber ini** (Allow from this source).
3. Kembali, lalu ketuk **Instal** (Install).

### 4. Buka aplikasi

Ketuk **Buka** (Open), atau cari **Papan Skor** di daftar aplikasi.

### Kalau tidak bisa dipasang

| Masalah | Solusi |
|---|---|
| HP Samsung memblokir pemasangan | Matikan sementara **Pemblokir Otomatis** (Auto Blocker) di **Setelan → Keamanan dan privasi**. Nyalakan lagi setelah selesai. |
| Muncul peringatan Play Protect | Pastikan APK diunduh dari halaman Releases repo ini, lalu pilih **Detail lainnya → Tetap instal** (More details → Install anyway). |
| "Aplikasi tidak terpasang" saat memperbarui | Versi yang terpasang punya tanda tangan berbeda, misalnya hasil build sendiri. Hapus versi lama lalu pasang lagi. Skor yang tersimpan ikut terhapus. |
| "Terjadi masalah saat mengurai paket" | Unduhan rusak atau versi Android di bawah 11. Coba unduh ulang. |

### Memperbarui aplikasi

Unduh APK terbaru dari Releases dan pasang seperti biasa, tanpa menghapus versi lama. Skor dan pengaturan tetap ada.

## Cara pakai

| Mau apa | Caranya |
|---|---|
| Tambah atau kurangi poin | Ketuk angka poin, masukkan hitungan, lalu pilih **Tambah** atau **Kurangi** |
| Catat pemenang set | Ketuk angka set kubu yang menang. Poin kedua kubu kembali ke 0 |
| Koreksi set | Tekan lama angka set untuk mengurangi 1 |
| Ganti nama, warna, emoji | Ketuk ikon pensil |
| Atur max poin, posisi layar, getar | Menu **Atur** |
| Membatalkan aksi terakhir | Menu **Undo** |

Saat max poin tercapai, muncul tanda **MENANG** dan HP bergetar. Poin tidak kembali ke 0 sendiri sampai angka set diketuk.

## Build dari kode sumber

Butuh Android SDK 36 dan JDK 17 atau lebih baru. Cara termudah: buka folder ini di Android Studio, atau jalankan di Windows:

```powershell
.\gradlew.bat assembleDebug
```

Hasilnya ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Lisensi font

Font [Saira](https://github.com/google/fonts/tree/main/ofl/sairasemicondensed) dipakai di bawah SIL Open Font License 1.1, lihat [licenses/Saira-OFL.txt](licenses/Saira-OFL.txt).
