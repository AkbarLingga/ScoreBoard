# Papan Skor Implementation Plan

> **For agentic workers:** execute natively with superpowers:executing-plans. User approved going straight through to the APK.

**Goal:** APK papan skor 2 kubu untuk Galaxy S25, offline, < 10 MB.

**Architecture:** Logika murni Kotlin (Match, Calc, GameTimer, MatchCodec) diuji JUnit; UI View bawaan digambar dari kode, satu Activity yang menangani rotasi sendiri (configChanges) supaya sheet yang terbuka tidak hilang.

**Tech Stack:** Kotlin, AGP, minSdk 26, targetSdk 36, tanpa dependency runtime. Test: junit, org.json.

**Spec:** docs/superpowers/specs/2026-11-26-scoreboard-design.md

## Global Constraints
- Paket `com.pointrecord.app`, label "Papan Skor", teks UI Bahasa Indonesia.
- Tanpa izin INTERNET; izin hanya VIBRATE. APK < 10 MB.
- Poin tidak pernah reset otomatis; hanya lewat tap set atau match baru.

## Review Focus
1. Ekspresi menggantung ("25+") atau super besar: dihitung tanpa operator akhir, dibatasi, tidak crash.
2. Data tersimpan rusak: balik ke default, tidak crash.
3. Undo setelah tap set tidak sengaja: poin kedua kubu kembali persis.
4. Tukar sisi lalu riwayat: pemenang tetap kubu yang benar.
5. Rotasi saat kalkulator terbuka: ekspresi tidak hilang (diuji di emulator).

### Task 1: Logika
**Files:** `app/src/main/java/com/pointrecord/app/{Calc,Match,GameTimer,MatchCodec}.kt`, test `app/src/test/java/com/pointrecord/app/LogicTest.kt`

**Interfaces (Produces):**
- `class Calc(expr: String = "") { val text; fun press(key: Char); fun value(): Long? }` kunci `0-9 + - * = C <`
- `data class Team(name, color, emoji, score, sets)`, `data class SetResult(winner, a, b)`, `data class MatchState(teams: List<Team>, maxPoint, history)`
- `class Match(state, undoStack)`: `addPoints(i, n): Boolean` (true = baru mencapai max), `winSet(i)`, `removeSet(i)`, `swap()`, `newMatch()`, `editTeam(i, name, color, emoji)`, `setMaxPoint(n)`, `undo(): Boolean`, `leader(): Int?`
- `class GameTimer(var accumulated, var startedAt)`: `toggle(now)`, `reset()`, `elapsed(now)`, `running`, `format(ms)`
- `object MatchCodec { encode(Match): String; decode(String?): Match }`

- [ ] Tulis test gagal, jalankan (FAIL), implementasi, jalankan (PASS): `gradlew testDebugUnitTest`

### Task 2: UI
**Files:** `Ui.kt, FitNumberView.kt, BoardView.kt, Sheet.kt, Sheets.kt, Whistle.kt, MainActivity.kt`, drawable ikon, manifest, font OFL di `res/font`.
- [ ] Papan landscape + potrait, kalkulator, edit kubu, pengaturan, riwayat, konfirmasi match baru, snackbar undo.
- [ ] `gradlew assembleDebug` sukses.

### Task 3: Verifikasi dan rilis
- [ ] Emulator: cek semua alur + screenshot landscape/potrait, rotasi saat kalkulator terbuka, tutup paksa lalu buka.
- [ ] Keystore rilis, `gradlew assembleRelease` (R8), `apksigner verify`, ukuran < 10 MB, pasang APK rilis di emulator.
