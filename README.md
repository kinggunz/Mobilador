# Jaga Anak - Aplikasi Anak

Aplikasi ini dipasang di HP anak. Fungsinya:
1. Menampilkan **kode pairing** (6 digit acak, tersimpan otomatis).
2. Mengirim daftar aplikasi yang terpasang di HP ke Firebase (supaya bisa dipilih dari HP orang tua).
3. Menjalankan **Accessibility Service** yang memantau aplikasi apa yang sedang dibuka. Kalau aplikasi itu = aplikasi yang diblokir orang tua, DAN waktu sekarang masuk jam tidur → otomatis ditendang ke Home.

## Cara setup (WAJIB sebelum build)
1. Buat project di https://console.firebase.google.com
2. Aktifkan **Realtime Database** (mode test dulu boleh, untuk pemakaian nyata atur security rules).
3. Di project itu, tambahkan Android app dengan package name `com.jagaanak.anak`.
4. Download `google-services.json`, timpa file placeholder di `app/google-services.json`.
5. Lakukan hal sama untuk project **JagaAnak-Ortu** (package `com.jagaanak.ortu`) di **Firebase project yang SAMA**, supaya keduanya baca/tulis ke database yang sama.

## Build APK lewat GitHub Actions
1. Push folder ini ke repo GitHub (bisa dari Termux: `git init && git add . && git commit -m "init" && git remote add origin <url> && git push`).
2. Buka tab **Actions** di repo → workflow "Build APK Jaga Anak (Anak)" akan otomatis jalan setiap push ke branch `main`, atau jalankan manual lewat "Run workflow".
3. Setelah selesai, download APK dari bagian **Artifacts** di halaman run tersebut.

## Setelah instal di HP anak
- Buka aplikasi, catat kode pairing yang muncul.
- Tekan "Aktifkan Layanan Aksesibilitas" lalu aktifkan "Jaga Anak" di daftar Accessibility.
- Kode pairing itu dimasukkan ke aplikasi Ortu.
