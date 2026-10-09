# Sepi Marketplace 🛒✨

**"Belanja jadi ramai"**

Aplikasi mobile marketplace modern berbasis Android Native (XML Views + ViewBinding + Kotlin + MVVM) dengan desain **Claymorphism biru-putih** dan integrasi Supabase Cloud Backend (PostgreSQL, PostgREST, & GoTrue Auth).

## 📌 Daftar Fitur Utama

- 🎨 **Claymorphism UI:** Palet biru-putih dengan permukaan timbul berlapis ekstrusi (tanpa blur), header dan panel flash sale berwarna biru kobalt, font Nunito, serta mikro-animasi sentuh.
- 🔐 **Autentikasi Supabase:** Pendaftaran akun, masuk akun, pembaruan token otomatis (TokenAuthenticator), dan penyimpanan sesi terenkripsi.
- 🏠 **Beranda Dinamis:** Carousel voucher promo, baris kategori, flash sale dengan hitung mundur instan, serta rekomendasi produk.
- 🔍 **Pencarian & Filter Instan:** Pencarian produk berbasis PostgREST dengan debounce 300ms, penyaringan rentang harga, rating minimal 4 bintang, dan pengurutan (Terbaru, Terlaris, Harga).
- 📦 **Detail & Varian Produk:** Tampilan detail lengkap, pemilih varian (ukuran/warna), harga dinamis (price delta), dan ulasan pembeli.
- 🛒 **Keranjang Belanja:** Pembaruan jumlah (+/-), seleksi item, dan pembaruan badge keranjang secara real-time.
- 💳 **Checkout & Pembayaran Sandbox:** Pilih alamat Cirebon, opsi pengiriman (Reguler/Express), penerapan voucher promo, dan simulasi pembayaran Virtual Account BCA.
- 🚚 **Tracking & Mode Demo:** Lacak status pesanan (UNPAID → PAID → PACKED → SHIPPED → DELIVERED) dengan tombol simulasi Mode Demo (`advance_order`).
- ⭐ **Ulasan Pembeli:** Penulisan ulasan dan rating khusus pembeli yang telah menerima pesanan (DELIVERED).
- ❤️ **Favorit Tersinkron:** Penandaan produk favorit secara optimistis dan tersimpan per akun.
- 📍 **Manajemen Alamat:** CRUD alamat pengiriman khusus wilayah Cirebon dengan penanda alamat utama.

## 🏗️ Arsitektur Aplikasi

Aplikasi dibangun menggunakan pola arsitektur MVVM (Model-View-ViewModel) dengan Manual Dependency Injection (`AppContainer`):

```
[ UI (Activity/Fragment/ViewBinding) ]
                  │
                  ▼
          [ ViewModel ] (StateFlow & SharedFlow)
                  │
                  ▼
         [ Repository Pattern ]
                  │
                  ▼
    [ Retrofit ApiService / OkHttp ]
                  │
                  ▼
   [ Supabase Cloud Backend (PostgREST & Auth) ]
```

## 🛠️ Teknologi & Library

- **Bahasa:** Kotlin (JDK 17)
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 35 (Android 15)
- **UI:** XML Views, ViewBinding, ConstraintLayout, Material Components
- **Arsitektur:** MVVM, StateFlow, SharedFlow, viewModelScope, ViewModelProvider.Factory
- **Network:** Retrofit 2, OkHttp 4, Gson, HttpLoggingInterceptor
- **Pemuatan Gambar:** Coil 2
- **Skeleton Loading:** Facebook Shimmer
- **Logging:** Timber 5

## 🚀 Cara Menjalankan Proyek

1. **Clone Repositori:**
   ```bash
   git clone https://github.com/ozitsaani/ecommerce-sepi.git
   cd ecommerce-sepi
   ```
2. **Setup Database Supabase:**
   - Buat proyek di [Supabase Dashboard](https://supabase.com).
   - Matikan **Confirm email** pada Authentication → Providers → Email.
   - Buka menu **SQL Editor → New Query**, salin seluruh isi file `supabase/supabase_setup.sql`, lalu **Run**.
3. **Konfigurasi `local.properties`:**
   - Buat file `local.properties` di folder root proyek jika belum ada.
   - Tambahkan URL dan Publishable Key dari dashboard Supabase Anda (jangan pakai kunci `service_role`):
   ```properties
   SUPABASE_URL=https://<project-ref>.supabase.co
   SUPABASE_KEY=<your-anon-or-publishable-key>
   ```
4. **Build & Run:**
   - Buka proyek di Android Studio, jalankan Gradle Sync.
   - Jalankan aplikasi pada emulator Pixel atau perangkat fisik Android (Min API 24).

> Aplikasi membutuhkan backend Supabase yang aktif. Proyek gratis bisa terjeda bila lama tidak dipakai, pastikan statusnya **Active** di dashboard.

## 🔑 Akun Demo & Data Uji

| Email       | Kata sandi |
|-------------|------------|
| _bubudenko_ | _masuk123_ |

- Voucher: `SEPIHEMAT`, `ONGKIRFREE`, `SEPIBARU`
- Virtual Account sandbox: `123 456 7890`
- Alamat pengiriman wajib berkota Cirebon

## 📥 Unduh APK

_https://drive.google.com/drive/folders/1laRK20y1xWk2Lw3oshaTUYEcNYJS4RmV?usp=drive_link_

## 👨‍🎓 Lisensi & Kredit

Dipersembahkan untuk memenuhi Tugas Akhir / UAS Mata Kuliah Pemrograman Mobile Dasar, STMIK IKMI Cirebon.

By : Fakhrur Rozi Tsani