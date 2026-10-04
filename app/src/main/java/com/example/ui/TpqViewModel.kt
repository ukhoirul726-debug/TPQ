package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthSession
import com.example.data.Santri
import com.example.data.SupabaseRepository
import com.example.data.Transaksi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TpqUiState(
  val session: AuthSession? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val santriList: List<Santri> = emptyList(),
  val transaksiList: List<Transaksi> = emptyList(),
  val currentTab: String = "dashboard", // "dashboard", "transaksi", "santri"
  val bulanTerpilih: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
)

class TpqViewModel(
  private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(TpqUiState())
  val uiState: StateFlow<TpqUiState> = _uiState.asStateFlow()

  fun clearMessages() {
    _uiState.update { it.copy(errorMessage = null, successMessage = null) }
  }

  fun setTab(tab: String) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun setBulan(bulan: String) {
    _uiState.update { it.copy(bulanTerpilih = bulan) }
  }

  // ==========================================
  // FITUR 1: Auth
  // ==========================================
  fun login(email: String, pass: String) {
    if (email.isBlank() || pass.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Email dan kata sandi wajib diisi!") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repository.login(email, pass)
      result.fold(
        onSuccess = { session ->
          _uiState.update {
            it.copy(
              session = session,
              isLoading = false,
              successMessage = "Berhasil masuk sebagai ${session.userEmail}"
            )
          }
          loadData()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Gagal masuk")
          }
        }
      )
    }
  }

  fun signUp(email: String, pass: String) {
    if (email.isBlank() || pass.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Email dan kata sandi wajib diisi!") }
      return
    }
    if (pass.length < 6) {
      _uiState.update { it.copy(errorMessage = "Kata sandi minimal 6 karakter!") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repository.signUp(email, pass)
      result.fold(
        onSuccess = { msg ->
          val newSession = repository.currentSession
          _uiState.update {
            it.copy(
              session = newSession,
              isLoading = false,
              successMessage = msg
            )
          }
          if (newSession != null) loadData()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Pendaftaran gagal")
          }
        }
      )
    }
  }

  fun logout() {
    repository.logout()
    _uiState.update {
      TpqUiState() // Reset semua state kembali ke layar login
    }
  }

  // ==========================================
  // Load All Data
  // ==========================================
  fun loadData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }

      val santriRes = repository.getSantri()
      val transaksiRes = repository.getTransaksi()

      santriRes.onSuccess { sList ->
        _uiState.update { it.copy(santriList = sList) }
      }
      transaksiRes.onSuccess { tList ->
        _uiState.update { it.copy(transaksiList = tList) }
      }

      val err = santriRes.exceptionOrNull() ?: transaksiRes.exceptionOrNull()
      _uiState.update {
        it.copy(
          isLoading = false,
          errorMessage = err?.message
        )
      }
    }
  }

  // ==========================================
  // FITUR 3 & 4: Transaksi
  // ==========================================
  fun addTransaksi(
    tanggal: String,
    jenis: String,
    kategori: String,
    jumlah: Long,
    santriId: String?,
    keterangan: String?,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val res = repository.addTransaksi(tanggal, jenis, kategori, jumlah, santriId, keterangan)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Transaksi berhasil dicatat!"
            )
          }
          loadData()
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Gagal mencatat transaksi")
          }
        }
      )
    }
  }

  fun deleteTransaksi(id: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val res = repository.deleteTransaksi(id)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Transaksi berhasil dihapus."
            )
          }
          loadData()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Gagal menghapus transaksi")
          }
        }
      )
    }
  }

  // ==========================================
  // FITUR 5: Santri
  // ==========================================
  fun addSantri(nama: String, kelas: String, onSuccess: () -> Unit) {
    if (nama.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Nama santri wajib diisi!") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val res = repository.addSantri(nama, kelas)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Santri $nama berhasil ditambahkan!"
            )
          }
          loadData()
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Gagal menambah santri")
          }
        }
      )
    }
  }

  fun toggleSantriStatus(id: String, targetAktif: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val res = repository.updateSantriStatus(id, targetAktif)
      res.fold(
        onSuccess = {
          val statusText = if (targetAktif) "diaktifkan kembali" else "dinonaktifkan"
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Status santri berhasil $statusText."
            )
          }
          loadData()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Gagal mengubah status santri")
          }
        }
      )
    }
  }
}
