package com.example.engine.graphics

/**
 * Metadata and definitions for the 8 standard 2D Mobile / One-Button Game Background types.
 */
enum class GameBackgroundType(
    val title: String,
    val shortName: String,
    val description: String,
    val pros: String,
    val cons: String,
    val gameExamples: String
) {
    STATIC_BG(
        title = "1. Static Background",
        shortName = "Static",
        description = "1 gambar diam, gak gerak sama sekali.",
        pros = "Paling ringan. 0 draw call tambahan. Sangat hemat daya baterai & RAM.",
        cons = "Visual statis tanpa ilusi pergerakan.",
        gameExamples = "Flappy Bird, 2D Puzzle, Word Games"
    ),
    TILED_REPEATING_BG(
        title = "2. Tiled / Repeating Background",
        shortName = "Tiled",
        description = "1 tile kecil diulang terus ke samping dan bawah secara seamless.",
        pros = "Hemat memory luar biasa. Bisa infinite world tanpa file texture besar.",
        cons = "Bisa terlihat repetitif jika pola tile terlalu mencolok.",
        gameExamples = "Endless Runner, Retro Platformer, Terraria"
    ),
    SCROLLING_BG(
        title = "3. Scrolling Background",
        shortName = "Scrolling",
        description = "1 gambar atau layer panjang yang digeser terus secara kontinyu.",
        pros = "Simpel, memberikan kesan maju/melaju dengan konsisten.",
        cons = "Keliatan looping jika tekstur terlalu pendek.",
        gameExamples = "Subway Surfers retro, Space Shooter, Jetpack Joyride"
    ),
    GRADIENT_SOLID_BG(
        title = "4. Gradient / Solid Color Background",
        shortName = "Gradient",
        description = "Warna polos solid atau linear/radial gradient dinamis.",
        pros = "Paling ringan, fokus 100% ke karakter/item, estetik modern minimalis.",
        cons = "Tidak memiliki elemen pemandangan detail.",
        gameExamples = "2048, Hyper-casual, Color Switch"
    ),
    PROCEDURAL_BG(
        title = "5. Procedural Background",
        shortName = "Procedural",
        description = "Background digenerate murni dengan algoritma kode (stars, grid cyber, neon wave).",
        pros = "Gak butuh file asset gambar sama sekali. Selalu variatif tiap sesi.",
        cons = "Memerlukan perhitungan matematika GPU/CPU ringan di setiap frame.",
        gameExamples = "Geometry Dash, Arcade Shooters, Synthwave Racers"
    ),
    ANIMATED_BG(
        title = "6. Animated Background",
        shortName = "Animated",
        description = "Background memiliki animasi kecil (awan melayang, lampu neon berkedip, partikel api/kunang-kunang).",
        pros = "Menghidupkan scene secara dinamis tanpa perlu multi-layer parallax rumit.",
        cons = "Memerlukan pembaruan time-tick partikel background.",
        gameExamples = "Premium Platformers, Alto's Adventure, Hollow Knight"
    ),
    CAMERA_FOLLOW_BG(
        title = "7. Camera Follow Background",
        shortName = "Camera Follow",
        description = "Background besar 1x (world map luas), kamera bergerak mulus mengikuti player.",
        pros = "Bisa bikin level open-world luas tanpa loading screen.",
        cons = "Perlu viewport bounding math dan sistem koordinat kamera terpadu.",
        gameExamples = "1-Screen Adventure, Super Mario World, Zelda 2D"
    ),
    VERTEX_LOW_POLY_3D_BG(
        title = "8. Vertex Painted / Low-Poly 3D BG",
        shortName = "Low-Poly 3D",
        description = "Background 3D low-poly faceted di-render proyeksi 2D dengan dynamic lighting & watercolor vertex shading.",
        pros = "Aesthetic premium bergaya Monument Valley, match dengan karakter modern.",
        cons = "Sedikit lebih intensif komputasi dibanding solid 2D biasa.",
        gameExamples = "Monument Valley, Lara Croft GO, Badland"
    )
}
