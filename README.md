# Jaga Anak - Aplikasi Orang Tua

Aplikasi ini dipasang di HP orang tua. Fungsinya:
1. Masukkan kode pairing dari HP anak → menarik daftar aplikasi yang terpasang di HP anak.
2. Pilih SATU aplikasi yang mau diblokir.
3. Atur jam mulai & jam selesai "jam tidur".
4. Simpan → pengaturan otomatis terkirim ke HP anak lewat Firebase, langsung aktif di aplikasi Anak.

## Cara setup (WAJIB sebelum build)
Gunakan **Firebase project yang sama** dengan aplikasi Anak:
1. Di project Firebase yang sama, tambahkan Android app dengan package name `com.jagaanak.ortu`.
2. Download `google-services.json`, timpa file placeholder di `app/google-services.json`.

## Build APK lewat GitHub Actions
1. Push folder ini ke repo GitHub terpisah (atau folder terpisah di repo yang sama, sesuaikan path workflow bila digabung).
2. Tab **Actions** → workflow "Build APK Jaga Anak (Ortu)" jalan otomatis tiap push ke `main`, atau jalankan manual.
3. Download APK hasil build dari bagian **Artifacts**.

## Cara pakai
- Buka aplikasi, masukkan kode pairing dari HP anak, tekan "Hubungkan".
- Pilih 1 aplikasi dari daftar yang muncul.
- Atur Jam Mulai & Jam Selesai tidur.
- Tekan "Simpan Pengaturan".
