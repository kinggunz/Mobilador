# Mobibawah

Aplikasi Android untuk mapping tombol keyboard (WASD, dll) dan mouse/touchpad
ke layar sentuh, agar game/aplikasi Android bisa dimainkan dengan "kontroler"
tombol mengambang yang posisi & ukurannya bisa diatur sendiri.

Mendukung **Android 8.0 (API 26) sampai Android 15 (API 35)**.

## Fitur terbaru

- **Icon aplikasi** custom (adaptive icon, gaya WASD + kursor mouse).
- **Gambar HUD custom**: di layar "Atur Mapping Tombol", tombol **Pilih
  Gambar HUD** membuka galeri — pilih screenshot HUD game kamu, lalu semua
  tombol bisa ditempatkan **tepat di atas** tombol virtual asli di gambar
  itu. Gambar disalin ke penyimpanan internal aplikasi (tidak perlu izin
  penyimpanan tambahan), tersimpan per-game, dan bisa dihapus lewat tombol
  **Hapus**.
- **Touchpad mouse drag kontinu**: geser layar sekarang benar-benar berupa
  satu sentuhan yang ditahan & digerakkan terus (bukan tap-tap terputus),
  jadi terasa seperti drag asli untuk kamera/scroll peta.
- **Landscape wajib**: `MainActivity` dan `MappingEditorActivity` dikunci
  `screenOrientation="landscape"`. Ini penting — posisi X/Y tombol dihitung
  sebagai persentase dari lebar/tinggi layar saat itu; kalau editor boleh
  portrait sementara overlay sungguhan selalu landscape (karena game-nya
  landscape), sumbu lebar/tinggi jadi tertukar dan tombol meleset. Dengan
  keduanya dikunci landscape, apa yang kamu atur di editor **dijamin sama
  persis** posisinya saat overlay berjalan di atas game.

## ⚠️ Batasan teknis penting

Tanpa akses root, Android **tidak mengizinkan** aplikasi mengirim `KeyEvent`
keyboard asli ke aplikasi lain. Karena itu Mobibawah bekerja dengan cara yang
sama seperti aplikasi key-mapper populer lain (Octopus, Panda Keymapper, dll):

- Setiap tombol overlay (`W`, `A`, `S`, `D`, dst) kamu **posisikan tepat di
  atas tombol virtual game** yang ingin diwakilinya.
- Saat tombol overlay ditekan, **Accessibility Service** men-simulasikan
  tap/tahan (hold) di titik itu — bukan mengirim keycode.
- Untuk gerakan mouse/kamera, ada **Touchpad**: geser jari di area touchpad →
  diterjemahkan jadi rangkaian swipe pendek di tengah layar game.

Ini legal, tidak perlu root, dan berlaku umum untuk semua game/app sejenis.

## Arsitektur

```
app/src/main/java/com/mobibawah/app/
├── model/
│   ├── ButtonMapping.kt      -> data class posisi/ukuran/tipe aksi 1 tombol
│   ├── MappingProfile.kt     -> (di dalam ButtonMapping.kt) kumpulan tombol per aplikasi
│   ├── KeyCatalog.kt         -> daftar semua tombol (A-Z, 0-9, arah, fungsi) + preset WASD
│   └── AppInfo.kt            -> data class untuk daftar aplikasi terpasang
├── data/
│   └── ProfileRepository.kt  -> simpan/baca mapping per packageName (SharedPreferences + JSON)
├── service/
│   └── MobiAccessibilityService.kt -> dispatch gesture tap/hold/swipe ke layar
├── overlay/
│   ├── FloatingButtonView.kt -> View tombol bulat, mode "main" & mode "edit"
│   ├── TouchpadView.kt       -> View trackpad untuk simulasi mouse/kamera
│   └── OverlayService.kt     -> foreground service yang menggambar semua tombol
│                                  di atas game (WindowManager overlay)
└── ui/
    ├── MainActivity.kt          -> pilih aplikasi, cek izin, tombol MULAI
    ├── AppListAdapter.kt        -> daftar aplikasi terpasang di device
    ├── MappingEditorActivity.kt -> layar "Atur Mapping Tombol" (drag, resize, key picker)
    └── KeyPickerDialog.kt       -> dialog pilih tombol keyboard
```

## Alur pemakaian

1. Buka Mobibawah → izinkan **Tampil di atas aplikasi lain** (Overlay) dan
   aktifkan **Layanan Aksesibilitas Mobibawah**.
2. Pilih game/aplikasi dari daftar.
3. Tap **Atur Mapping Tombol** → atur posisi & ukuran tombol (drag langsung
   di pratinjau), pilih key (W/A/S/D/dst), pilih tipe aksi (Tap/Hold/Toggle),
   atau pakai **Preset WASD** sebagai titik awal → **Simpan**.
4. Tap **MULAI** → Mobibawah otomatis membuka game tersebut, lalu menampilkan
   semua tombol + touchpad mengambang di atasnya.
5. Ada **tombol bulat ⌨ mengambang** yang selalu ada & bisa digeser bebas —
   tap sekali untuk sembunyikan/tampilkan semua tombol kapan saja.

## Build

1. Buka folder ini dengan **Android Studio** (Koala/Ladybug atau lebih baru).
   Android Studio akan otomatis melengkapi Gradle Wrapper saat sinkronisasi
   pertama (folder `gradle/wrapper` sengaja tidak disertakan agar ukuran repo
   kecil dan wrapper selalu memakai versi Gradle terbaru yang kompatibel).
2. Sync Gradle → Run ke device/emulator (`minSdk 26`, `targetSdk 35`).
3. Untuk build APK release: **Build > Generate Signed Bundle / APK**.

## Upload ke GitHub lewat Termux, lalu build APK otomatis (GitHub Actions)

Build APK Android penuh **di dalam Termux tidak disarankan** (SDK Android
sangat berat & sering error di HP). Cara yang jauh lebih stabil: **Termux
hanya dipakai untuk push kode ke GitHub**, lalu **GitHub Actions yang
membuild APK-nya secara otomatis di server** — sudah disiapkan lewat file
`.github/workflows/build-apk.yml` di repo ini.

### Langkah 1 — Siapkan Termux

```bash
pkg update -y && pkg upgrade -y
pkg install -y git unzip
termux-setup-storage      # izinkan akses penyimpanan saat diminta
```

### Langkah 2 — Pindahkan & ekstrak project

Kalau `Mobibawah.zip` ada di folder Download HP:

```bash
cd ~/storage/downloads
unzip Mobibawah.zip -d ~/
cd ~/Mobibawah
```

### Langkah 3 — Buat repo kosong di GitHub

Buka github.com dari browser HP → **New repository** → nama `Mobibawah` →
biarkan **kosong** (jangan centang README/gitignore, karena repo lokal
sudah punya) → Create repository.

### Langkah 4 — Buat Personal Access Token (buat login dari Termux)

GitHub sudah tidak menerima login pakai password biasa lewat `git push`.
Buat token di: **github.com → Settings → Developer settings → Personal
access tokens → Tokens (classic) → Generate new token**, centang scope
`repo`, lalu salin tokennya (hanya tampil sekali).

### Langkah 5 — Push dari Termux

```bash
git init
git config --global user.name "Nama Kamu"
git config --global user.email "email_kamu@gmail.com"
git add .
git commit -m "Initial commit: Mobibawah keymapper"
git branch -M main
git remote add origin https://github.com/USERNAME/Mobibawah.git
git push -u origin main
```

Ganti `USERNAME` dengan username GitHub-mu. Saat diminta **Username**,
isi username GitHub; saat diminta **Password**, tempel **token** dari
Langkah 4 (bukan password akun).

### Langkah 6 — Ambil APK hasil build

1. Buka repo `Mobibawah` di GitHub → tab **Actions**.
2. Workflow **"Build Mobibawah APK"** akan otomatis berjalan (beberapa
   menit). Tunggu sampai tanda centang hijau ✅.
3. Klik run yang selesai itu → scroll ke bagian **Artifacts** → unduh
   **Mobibawah-debug-apk** (berupa `.zip`, di dalamnya ada `app-debug.apk`).
4. Pindahkan `app-debug.apk` ke HP → install (aktifkan dulu "Izinkan dari
   sumber tidak dikenal" di pengaturan HP kalau diminta).

> Catatan: ini APK **debug** (untuk uji coba pribadi, sudah bisa langsung
> dipasang). Kalau nanti mau upload ke Play Store atau bagikan versi
> resmi, perlu APK **release** yang ditandatangani (signing key) — tinggal
> bilang kalau mau saya tambahkan langkah signing-nya di workflow ini.

### Update kode selanjutnya

Setiap kali kamu edit kode lagi dari Termux dan mau build ulang, cukup:

```bash
git add .
git commit -m "update mapping"
git push
```

GitHub Actions otomatis build ulang APK-nya setiap push ke `main`.

## Roadmap pengembangan lanjutan (opsional)

- Multi-profile per game (beberapa layout tersimpan, tinggal pilih).
- Import/export mapping ke file `.json` supaya bisa dibagikan ke pemain lain.
- Sensitivitas & kurva percepatan mouse yang lebih halus.
- Preset tambahan (MOBA, battle royale, dsb) selain WASD.
