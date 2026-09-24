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

## Update terbaru (perbaikan besar)

- **PERBAIKAN UTAMA — Keyboard fisik (Bluetooth & kabel/OTG) kini beneran
  berfungsi.** Sebelumnya Mobibawah hanya membaca sentuhan di layar, tidak
  pernah membaca tombol keyboard fisik sama sekali — itu sebabnya mapping
  "tidak jalan" saat dipakai dengan keyboard sungguhan. Sekarang
  `MobiAccessibilityService` membaca setiap tombol fisik yang ditekan
  (lewat `onKeyEvent`, diaktifkan dengan `canRequestFilterKeyEvents`) dan
  mencocokkannya ke key yang kamu atur (W/A/S/D/dst) — aksinya
  (Tap/Hold/Toggle/Macro) persis sama seperti kalau kamu sentuh tombol di
  layar. Bluetooth dan kabel/OTG diperlakukan sama oleh Android, jadi
  keduanya otomatis didukung tanpa kode terpisah.
- **Toggle matikan mouse-geser**: ada ikon panah kecil di tengah touchpad
  (sesuai referensi gambar) — tap cepat (tanpa menggeser) di situ untuk
  menyalakan/mematikan mode mouse-geser. Saat dimatikan, area touchpad
  benar-benar "tembus" (tidak menghalangi sentuhan ke game sama sekali).
- **Mapping tersimpan permanen** — ini sebenarnya sudah berjalan sejak
  awal (disimpan di penyimpanan aplikasi, bukan memori sementara), jadi
  pengaturanmu tetap ada walau aplikasi ditutup total atau HP di-restart;
  hanya hilang kalau data aplikasi dihapus manual atau aplikasi di-uninstall.
- **Kode Mapping** — di layar "Atur Mapping Tombol" ada tombol **Bagikan
  Kode** (membuat kode teks dari layout tombolmu, tinggal disalin & kirim
  ke teman) dan **Impor Kode** (tempel kode dari teman untuk langsung
  memakai layout yang sama, di game apa pun yang sedang mereka atur).
- **Dukungan Android**: minSdk tetap 26 (Android 8.0) — ini sudah mencakup
  hampir seluruh perangkat Android aktif saat ini (8.0 s/d 15). Menurunkan
  lagi ke versi yang jauh lebih lama tidak disarankan karena beberapa API
  penting (adaptive icon, foreground service khusus, dll) baru ada di 26+.
- **Soal mouse fisik**: kursor mouse USB/Bluetooth sudah otomatis berfungsi
  di level sistem Android (bukan sesuatu yang perlu dikodekan aplikasi ini)
  — bisa dipakai menavigasi menu Mobibawah maupun UI Android seperti biasa.
  Fitur "touchpad" di Mobibawah adalah simulasi mouse-look lewat GESER JARI
  di layar sentuh, karena itulah yang tidak tersedia secara native di game
  berbasis sentuhan.

## Update sebelumnya

- **Wajib daftar akun dulu** (username & password) sebelum bisa pakai —
  akun ini **lokal di perangkat saja** (bukan online), tersimpan terenkripsi
  hash di `AppPrefs`. Sekali daftar, tidak diminta lagi selanjutnya.
- **Halaman SK (Syarat & Ketentuan)** wajib dicentang & disetujui sebelum
  masuk ke menu utama, ada teks **"Credit by Gunz"**.
- **Menu utama baru (hub)**: logo animasi (efek "bernapas"), tombol **+**
  besar di tengah untuk memilih game, daftar **Game Tersimpan** (game yang
  sudah pernah dipetakan, tap untuk buka lagi), dan tombol **Matikan
  Mobibawah** untuk menghentikan overlay yang sedang berjalan.
- **Halaman Detail Aplikasi**: setelah pilih game dari daftar, tampil logo
  aplikasi besar dengan latar dari icon aplikasi itu sendiri, status
  mapping (sudah/belum diatur), tombol **Atur Mapping Tombol** dan **MULAI**.
- **Gambar HUD custom full screen**: di layar mapping, gambar HUD yang
  dipilih dari galeri sekarang mengisi **seluruh layar** (tanpa bilah hitam
  kosong) sebagai acuan visual. Posisi tombol tetap dihitung sebagai
  persentase lebar/tinggi layar (bukan piksel gambar), jadi presisinya
  tetap sama persis saat overlay sungguhan berjalan di game — baik gambar
  HUD-nya pas layar penuh atau rasionya berbeda sedikit.
- **Panel kontrol melayang**: semua tombol pengaturan (Tambah Tombol,
  Preset WASD, Pilih/Hapus Gambar HUD, ukuran, Simpan) sekarang jadi bilah
  melayang transparan di bagian bawah, tidak memotong area pratinjau.
- **Icon logo & animasi sendiri**: adaptive icon custom (gaya WASD +
  kursor mouse) dan animasi pulse pada logo di menu utama.
- **Fitur Makro (auto-tap cepat)**: tipe aksi baru `MACRO` — sekali tekan
  untuk menyalakan, tombol akan mengirim tap super cepat berulang terus-
  menerus (interval bisa diatur, default 80ms) sampai ditekan sekali lagi
  untuk mematikan. Tombol macro tetap bisa digeser bebas di mode edit, dan
  saat aktif **tidak mengganggu** touchpad/tombol lain karena setiap
  elemen overlay adalah window terpisah.
- **Touchpad mouse drag kontinu** (dari update sebelumnya) tetap ada dan
  tidak terganggu oleh tombol macro yang aktif bersamaan.

## Arsitektur

```
app/src/main/java/com/mobibawah/app/
├── model/
│   ├── ButtonMapping.kt      -> posisi/ukuran/tipe aksi (TAP/HOLD/TOGGLE/MACRO) 1 tombol
│   ├── KeyCatalog.kt         -> daftar semua tombol (A-Z, 0-9, arah, fungsi) + preset WASD
│   └── AppInfo.kt            -> data class untuk daftar aplikasi terpasang
├── data/
│   ├── AppPrefs.kt           -> akun lokal (register/login) & status persetujuan SK
│   └── ProfileRepository.kt  -> simpan/baca mapping + gambar HUD per packageName
├── service/
│   └── MobiAccessibilityService.kt -> dispatch gesture tap/hold/drag/macro ke layar
├── overlay/
│   ├── FloatingButtonView.kt -> View tombol bulat: mode "main" (tap/hold/toggle/macro) & "edit"
│   ├── TouchpadView.kt       -> View trackpad drag kontinu untuk simulasi mouse/kamera
│   └── OverlayService.kt     -> foreground service yang menggambar semua tombol di atas game
└── ui/
    ├── LauncherActivity.kt       -> gerbang routing awal (Daftar -> SK -> Menu Utama)
    ├── AuthActivity.kt           -> halaman daftar akun / login lokal
    ├── TermsActivity.kt          -> halaman SK dengan checkbox wajib
    ├── MainActivity.kt           -> menu utama (hub): logo animasi, tombol +, favorit, matikan
    ├── AppPickerActivity.kt      -> halaman daftar aplikasi terpasang untuk dipilih
    ├── AppDetailActivity.kt      -> halaman detail 1 game: icon besar, MULAI, Atur Mapping
    ├── AppListAdapter.kt         -> adapter RecyclerView daftar aplikasi
    ├── MappingEditorActivity.kt  -> layar "Atur Mapping Tombol" full screen + gambar HUD
    └── KeyPickerDialog.kt        -> dialog pilih tombol keyboard
```

## Alur pemakaian

1. **Buka pertama kali** → wajib **Daftar Akun** (username & password, lokal
   di HP) → wajib centang & setujui **Syarat & Ketentuan** → masuk ke
   **Menu Utama**. Selanjutnya buka app langsung ke Menu Utama.
2. Di Menu Utama, izinkan **Tampil di atas aplikasi lain** (Overlay) dan
   aktifkan **Layanan Aksesibilitas Mobibawah** (tombol kecil di bawah).
3. Tap tombol **+** besar di tengah → pilih game dari daftar aplikasi
   terpasang → masuk ke halaman detail game tersebut (logo besar + latar
   icon game).
4. Tap **Atur Mapping Tombol** → (opsional) **Pilih Gambar HUD** dari
   galeri sebagai acuan visual (mengisi layar penuh) → atur posisi &
   ukuran tombol (drag langsung), pilih key, pilih tipe aksi
   (Tap/Hold/Toggle/**Macro**), atau pakai **Preset WASD** → **Simpan**.
5. Kembali ke halaman detail game → tap **MULAI** → Mobibawah otomatis
   membuka game tersebut, lalu menampilkan semua tombol + touchpad
   mengambang di atasnya.
6. Ada **tombol bulat ⌨ mengambang** yang selalu ada & bisa digeser bebas —
   tap sekali untuk sembunyikan/tampilkan semua tombol kapan saja.
7. Untuk berhenti total, kembali ke Mobibawah (Menu Utama) → tap
   **Matikan Mobibawah**.

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
