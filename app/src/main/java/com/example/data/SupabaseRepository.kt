package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseRepository {

  companion object {
    const val BASE_URL = "https://rqxfjptocopgphmcqtel.supabase.co"
    const val ANON_KEY =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJxeGZqcHRvY29wZ3BobWNxdGVsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTExMTY5NjgsImV4cCI6MjEwNjY5Mjk2OH0.SAQI8NNK0K8kHPEUI3tvbSArbprr2c-m6FVDytg4zmk"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  var currentSession: AuthSession? = null

  private fun authHeader(): String {
    return currentSession?.accessToken?.let { "Bearer $it" } ?: "Bearer $ANON_KEY"
  }

  // ==========================================
  // FITUR 1: Autentikasi (Login & Sign Up)
  // ==========================================
  suspend fun login(email: String, pass: String): Result<AuthSession> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("email", email.trim())
        put("password", pass.trim())
      }
      val request = Request.Builder()
        .url("$BASE_URL/auth/v1/token?grant_type=password")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(JSON_MEDIA))
        .build()

      val response = client.newCall(request).execute()
      val bodyStr = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          val json = JSONObject(bodyStr)
          json.optString("error_description", json.optString("msg", "Gagal masuk"))
        } catch (e: Exception) {
          "Email atau kata sandi tidak valid."
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(bodyStr)
      val token = json.getString("access_token")
      val userObj = json.getJSONObject("user")
      val session = AuthSession(
        accessToken = token,
        userEmail = userObj.optString("email", email),
        userId = userObj.optString("id", "")
      )
      currentSession = session
      Result.success(session)
    } catch (e: Exception) {
      Result.failure(Exception("Koneksi gagal: ${e.localizedMessage ?: "Periksa jaringan"}"))
    }
  }

  suspend fun signUp(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("email", email.trim())
        put("password", pass.trim())
      }
      val request = Request.Builder()
        .url("$BASE_URL/auth/v1/signup")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(JSON_MEDIA))
        .build()

      val response = client.newCall(request).execute()
      val bodyStr = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          val json = JSONObject(bodyStr)
          json.optString("msg", json.optString("error_description", "Pendaftaran gagal"))
        } catch (e: Exception) {
          "Pendaftaran gagal."
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(bodyStr)
      if (json.has("access_token")) {
        val token = json.getString("access_token")
        val userObj = json.getJSONObject("user")
        currentSession = AuthSession(
          accessToken = token,
          userEmail = userObj.optString("email", email),
          userId = userObj.optString("id", "")
        )
      }
      Result.success("Pendaftaran berhasil! Silakan masuk.")
    } catch (e: Exception) {
      Result.failure(Exception("Koneksi gagal: ${e.localizedMessage ?: "Periksa jaringan"}"))
    }
  }

  fun logout() {
    currentSession = null
  }

  // ==========================================
  // FITUR 5: Data Santri
  // ==========================================
  suspend fun getSantri(): Result<List<Santri>> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("$BASE_URL/rest/v1/santri?select=*&order=nama.asc")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Authorization", authHeader())
        .get()
        .build()

      val response = client.newCall(request).execute()
      val bodyStr = response.body?.string() ?: "[]"

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Gagal mengambil data santri (${response.code})"))
      }

      val array = JSONArray(bodyStr)
      val list = mutableListOf<Santri>()
      for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        list.add(
          Santri(
            id = item.optString("id"),
            nama = item.optString("nama"),
            kelas = item.optString("kelas", "-"),
            aktif = item.optBoolean("aktif", true)
          )
        )
      }
      Result.success(list)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addSantri(nama: String, kelas: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("nama", nama.trim())
        put("kelas", kelas.trim())
        put("aktif", true)
      }
      val request = Request.Builder()
        .url("$BASE_URL/rest/v1/santri")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Authorization", authHeader())
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=minimal")
        .post(payload.toString().toRequestBody(JSON_MEDIA))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val err = response.body?.string() ?: ""
        return@withContext Result.failure(Exception("Gagal menambahkan santri: $err"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateSantriStatus(id: String, aktif: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("aktif", aktif)
      }
      val request = Request.Builder()
        .url("$BASE_URL/rest/v1/santri?id=eq.$id")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Authorization", authHeader())
        .addHeader("Content-Type", "application/json")
        .patch(payload.toString().toRequestBody(JSON_MEDIA))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val err = response.body?.string() ?: ""
        return@withContext Result.failure(Exception("Gagal mengubah status santri: $err"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ==========================================
  // FITUR 2, 3, 4: Data Transaksi
  // ==========================================
  suspend fun getTransaksi(): Result<List<Transaksi>> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("$BASE_URL/rest/v1/transaksi?select=*&order=tanggal.desc,id.desc")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Authorization", authHeader())
        .get()
        .build()

      val response = client.newCall(request).execute()
      val bodyStr = response.body?.string() ?: "[]"

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Gagal mengambil data transaksi (${response.code})"))
      }

      val array = JSONArray(bodyStr)
      val list = mutableListOf<Transaksi>()
      for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        list.add(
          Transaksi(
            id = item.optString("id"),
            tanggal = item.optString("tanggal"),
            jenis = item.optString("jenis", "masuk"),
            kategori = item.optString("kategori", "Lainnya"),
            jumlah = item.optLong("jumlah", 0L),
            santriId = if (item.isNull("santri_id")) null else item.optString("santri_id"),
            keterangan = if (item.isNull("keterangan")) null else item.optString("keterangan")
          )
        )
      }
      Result.success(list)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addTransaksi(
    tanggal: String,
    jenis: String,
    kategori: String,
    jumlah: Long,
    santriId: String?,
    keterangan: String?
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("tanggal", tanggal)
        put("jenis", jenis)
        put("kategori", kategori)
        put("jumlah", jumlah)
        if (!santriId.isNullOrEmpty()) {
          put("santri_id", santriId)
        } else {
          put("santri_id", JSONObject.NULL)
        }
        if (!keterangan.isNullOrBlank()) {
          put("keterangan", keterangan.trim())
        } else {
          put("keterangan", JSONObject.NULL)
        }
      }

      val request = Request.Builder()
        .url("$BASE_URL/rest/v1/transaksi")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Authorization", authHeader())
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=minimal")
        .post(payload.toString().toRequestBody(JSON_MEDIA))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val err = response.body?.string() ?: ""
        return@withContext Result.failure(Exception("Gagal menyimpan transaksi: $err"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteTransaksi(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("$BASE_URL/rest/v1/transaksi?id=eq.$id")
        .addHeader("apikey", ANON_KEY)
        .addHeader("Authorization", authHeader())
        .delete()
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val err = response.body?.string() ?: ""
        return@withContext Result.failure(Exception("Gagal menghapus transaksi: $err"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
