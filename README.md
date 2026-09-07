# Studio AI Engine - Comprehensive Mobile Development Environment

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Android Support](https://img.shields.io/badge/Android-5.0%2B%20%28API%2021%2B%20%7C%20Lollipop%2B%29-blue)
![Device Target](https://img.shields.io/badge/Optimized-itel%20A70%20%7C%20Low--Spec%20RAM%201GB%2B-orange)
![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey)

**Studio AI Engine** adalah lingkungan pengembangan game & aplikasi mobile berbasis AI terpadu (Integrated Development & AI Studio Environment) yang dirancang khusus untuk berjalan dengan performa tinggi, responsif, dan hemat sumber daya pada perangkat Android kelas *low-entry* (mulai dari Android 5.0 Lollipop ke atas, RAM minimal 1 GB, dan dioptimalkan secara khusus untuk perangkat seperti **itel A70**).

---

## 🚀 Fitur Utama & Arsitektur Sistem

Aplikasi ini menggabungkan berbagai subsistem canggih yang terintegrasi secara mulus:

1. **AI Copilot & Thinking Engine (`StudioAiDirector`)**:
   - Ditenagai oleh model **Gemini AI** (`gemini-3.1-pro-preview` / `gemini-2.5-flash`) dengan dukungan tingkat pemikiran tinggi (*ThinkingLevel.HIGH*) untuk analisis arsitektur, generasi kode, serta pembuatan blueprint level game secara otomatis.

2. **Mesin Fisika 2D Ringan & Kustom (`PhysicsWorld2D`)**:
   - Menggunakan deteksi tabrakan AABB, resolver impuls manifold, *spatial hash grid*, dan *Fast Binary Math* tanpa beban berat GPU/CPU tambahan, menjamin pergerakan fisika yang mulus hingga 60 FPS pada perangkat low-end.

3. **Polyglot Runtime & Code Studio**:
   - Mendukung sintaksis multi-bahasa (Kotlin, TypeScript, Python, C++, Rust, Go, Bytecode VM) lengkap dengan editor kode ringkas dan *syntax highlighting* yang ramah layar smartphone.

4. **Audio & Visual Synthesizer (`BytebeatDspSynthesizer`)**:
   - Generator musik & efek suara prosedural menggunakan rumus DSP Bytebeat tanpa memerlukan file audio MP3/WAV besar, menghemat penyimpanan dan memori RAM.

5. **Antarmuka Pengguna Responsif & Intuitif**:
   - Layout Jetpack Compose yang disederhanakan tanpa elemen interface yang saling berdempetan, tombol berukuran ramah sentuhan jari (*touch-friendly targets*), serta pengelolaan *viewport* yang menyesuaikan rasio layar smartphone.

---

## 📱 Spesifikasi Perangkat Teruji & Optimalisasi Low-Entry

- **Versi Minimum Android**: Android 5.0 Lollipop (API Level 21) ke atas
- **RAM Minimum**: 1 GB RAM (Profil memori rendah / `isLowRamDevice` dikonfigurasi secara otomatis)
- **Perangkat Referensi Pengujian**: **itel A70** (Unisoc T603, 3/4GB RAM, Android 13 Go Edition)
- **Optimasi Khusus**:
  - *Hardware Profile Preset*: Mode hemat daya & pembersihan cache *Spatial Hash Grid* secara berkala.
  - *ProGuard & R8 Shrinking*: Pengurangan ukuran APK dan *dead code elimination* yang dioptimalkan.
  - *Multi-ABI Split*: Pembagian paket APK berdasarkan arsitektur CPU (arm64-v8a, armeabi-v7a) untuk meminimalisir file instalasi.

---

## 🛠️ GitHub Actions Release APK Direct Artifacts

Aplikasi ini dilengkapi dengan alur kerja otomatisasi **GitHub Actions** (`.github/workflows/build-release.yml`) yang langsung menghasilkan file APK siap pakai di GitHub Artifacts setiap kali ada komit baru atau pembaruan rilis:

1. Setiap `push`, `pull_request`, atau pembentukan tag versi (`v*`), workflow GitHub akan memicu build Gradle secara otomatis.
2. File APK yang disesuaikan (*fully customized & optimized*) akan diunggah langsung ke artefak rilis GitHub:
   - `studio-ai-debug-apk`
   - `studio-ai-release-apk`

---

## 📦 Cara Membangun dari Sumber Kode (Local Build)

1. **Prasyarat**:
   - Android Studio Ladybug / Meerkat atau Gradle 8.8+
   - JDK 17

2. **Langkah-Langkah Build**:
   ```bash
   # Clone repositori
   git clone https://github.com/your-org/studio-ai.git
   cd studio-ai

   # Jalankan Build Debug APK
   gradle assembleDebug
   ```

---

## 📜 Lisensi (License)

Proyek ini dilesensikan di bawah **Apache License 2.0**.
Lihat berkas [LICENSE](LICENSE) untuk detail lengkap hak cipta dan ketentuan penggunaan.
