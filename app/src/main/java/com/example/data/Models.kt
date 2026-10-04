package com.example.data

/**
 * Model data Santri sesuai tabel Supabase santri(id, nama, kelas, aktif)
 */
data class Santri(
  val id: String,
  val nama: String,
  val kelas: String,
  val aktif: Boolean
)

/**
 * Model data Transaksi sesuai tabel Supabase transaksi(id, tanggal, jenis, kategori, jumlah, santri_id, keterangan)
 */
data class Transaksi(
  val id: String,
  val tanggal: String,
  val jenis: String, // "masuk" atau "keluar"
  val kategori: String, // "SPP", "Infaq", "Donasi", "Gaji Ustadz", "Listrik", "Lainnya"
  val jumlah: Long,
  val santriId: String?,
  val keterangan: String?
)

/**
 * Model sesi autentikasi pengguna
 */
data class AuthSession(
  val accessToken: String,
  val userEmail: String,
  val userId: String
)
