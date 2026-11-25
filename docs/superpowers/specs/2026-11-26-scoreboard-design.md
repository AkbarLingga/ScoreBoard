# Papan Skor (PointRecord) - desain singkat (disetujui 2026-11-26)

**Tujuan:** papan skor 2 kubu untuk Android (Galaxy S25), offline, susunan layar seperti screenshot user, APK < 10 MB.

**Perilaku**
- Landscape dan potrait.
- Tap angka poin: kalkulator (+ - x =), lalu *Tambah* atau *Kurangi*.
- Max poin bisa diatur atau dimatikan. Tercapai: tanda menang + getar. Poin lebih tetap dicatat, tidak reset sendiri.
- Tap angka set: set +1 untuk kubu itu dan poin kedua kubu balik 0. Tekan lama: set -1 (poin tetap).
- Pensil: nama, warna, emoji per kubu.
- Menu: undo, tukar sisi, riwayat set, match baru, peluit, pengaturan. Timer di atas.
- State tersimpan saat rotasi atau app ditutup; layar tetap nyala.

**Non-goal:** iklan, rekam layar, rating, login, internet.

**Pendekatan:** (A, dipilih) Kotlin native + View bawaan, tanpa dependency, 1-2 MB. (B) Flutter: boros jatah ukuran. (C) WebView: kurang mulus. Ikon dan visual digambar sendiri.

**Terserah builder:** detail visual, struktur kelas, format penyimpanan.
