package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Santri
import com.example.data.Transaksi
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Helper format Rupiah
fun formatRupiah(amount: Long): String {
  val localeID = Locale("id", "ID")
  val format = NumberFormat.getCurrencyInstance(localeID)
  format.maximumFractionDigits = 0
  return format.format(amount)
}

// Helper format Tanggal Indo
fun formatTanggalIndo(dateStr: String): String {
  return try {
    val parts = dateStr.split("-")
    if (parts.size == 3) {
      val cal = Calendar.getInstance()
      cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
      val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
      sdf.format(cal.time)
    } else dateStr
  } catch (e: Exception) {
    dateStr
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TpqApp(viewModel: TpqViewModel) {
  val state by viewModel.uiState.collectAsState()
  var showLogoutConfirm by remember { mutableStateOf(false) }

  // Tampilkan data hanya jika sudah login
  if (state.session == null) {
    AuthScreen(
      isLoading = state.isLoading,
      errorMessage = state.errorMessage,
      successMessage = state.successMessage,
      onLogin = { email, pass -> viewModel.login(email, pass) },
      onSignUp = { email, pass -> viewModel.signUp(email, pass) },
      onClearMessage = { viewModel.clearMessages() }
    )
    return
  }

  // Dialog Konfirmasi Keluar (Logout)
  if (showLogoutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      title = { Text("Konfirmasi Keluar") },
      text = { Text("Apakah Anda yakin ingin keluar dari sistem Keuangan TPQ?") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          onClick = {
            showLogoutConfirm = false
            viewModel.logout()
          }
        ) {
          Text("Ya, Keluar")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showLogoutConfirm = false }) {
          Text("Batal")
        }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF047857),
          titleContentColor = Color.White,
          actionIconContentColor = Color.White
        ),
        title = {
          Column {
            Text(
              "Keuangan TPQ",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              state.session?.userEmail ?: "Pengurus TPQ",
              style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD1FAE5))
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.loadData() },
            modifier = Modifier.testTag("refresh_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Segarkan Data")
          }
          IconButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier.testTag("logout_button")
          ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Keluar")
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = state.currentTab == "dashboard",
          onClick = { viewModel.setTab("dashboard") },
          icon = { Icon(Icons.Default.Home, contentDescription = "Ringkasan") },
          label = { Text("Ringkasan") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF047857),
            selectedTextColor = Color(0xFF047857),
            indicatorColor = Color(0xFFECFDF5)
          )
        )
        NavigationBarItem(
          selected = state.currentTab == "transaksi",
          onClick = { viewModel.setTab("transaksi") },
          icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transaksi") },
          label = { Text("Transaksi") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF047857),
            selectedTextColor = Color(0xFF047857),
            indicatorColor = Color(0xFFECFDF5)
          )
        )
        NavigationBarItem(
          selected = state.currentTab == "santri",
          onClick = { viewModel.setTab("santri") },
          icon = { Icon(Icons.Default.People, contentDescription = "Santri") },
          label = { Text("Santri") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF047857),
            selectedTextColor = Color(0xFF047857),
            indicatorColor = Color(0xFFECFDF5)
          )
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(Color(0xFFF8FAFC))
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Banner Pesan Sukses / Error
        state.errorMessage?.let { err ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
              Spacer(Modifier.width(8.dp))
              Text(
                err,
                color = Color(0xFF991B1B),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { viewModel.clearMessages() }) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF991B1B))
              }
            }
          }
        }

        state.successMessage?.let { msg ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
              Spacer(Modifier.width(8.dp))
              Text(
                msg,
                color = Color(0xFF065F46),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { viewModel.clearMessages() }) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF065F46))
              }
            }
          }
        }

        if (state.isLoading) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF047857)
          )
        }

        when (state.currentTab) {
          "dashboard" -> DashboardView(
            transaksiList = state.transaksiList,
            santriList = state.santriList,
            bulanTerpilih = state.bulanTerpilih,
            onBulanChanged = { viewModel.setBulan(it) },
            onNavToTransaksi = { viewModel.setTab("transaksi") },
            onNavToSantri = { viewModel.setTab("santri") }
          )
          "transaksi" -> TransaksiView(
            transaksiList = state.transaksiList,
            santriList = state.santriList,
            bulanTerpilih = state.bulanTerpilih,
            onBulanChanged = { viewModel.setBulan(it) },
            onAddTransaksi = { tgl, jns, kat, jml, sid, ket, cb ->
              viewModel.addTransaksi(tgl, jns, kat, jml, sid, ket, cb)
            },
            onDeleteTransaksi = { id -> viewModel.deleteTransaksi(id) }
          )
          "santri" -> SantriView(
            santriList = state.santriList,
            onAddSantri = { nama, kls, cb -> viewModel.addSantri(nama, kls, cb) },
            onToggleStatus = { id, target -> viewModel.toggleSantriStatus(id, target) }
          )
        }
      }
    }
  }
}

// ==========================================
// FITUR 1: Layar Autentikasi
// ==========================================
@Composable
fun AuthScreen(
  isLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  onLogin: (String, String) -> Unit,
  onSignUp: (String, String) -> Unit,
  onClearMessage: () -> Unit
) {
  var isRegister by remember { mutableStateOf(false) }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 440.dp)
        .testTag("auth_card"),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF047857), shape = RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
          "Keuangan TPQ",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = Color(0xFF0F172A)
        )
        Text(
          "Sistem Kas & Administrasi Santri",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF64748B)
        )

        Spacer(Modifier.height(20.dp))

        errorMessage?.let {
          Text(
            it,
            color = Color(0xFFDC2626),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }

        successMessage?.let {
          Text(
            it,
            color = Color(0xFF059669),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }

        OutlinedTextField(
          value = email,
          onValueChange = { email = it; onClearMessage() },
          label = { Text("Alamat Email") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("email_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
          value = password,
          onValueChange = { password = it; onClearMessage() },
          label = { Text("Kata Sandi") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("password_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(Modifier.height(20.dp))

        Button(
          onClick = {
            if (isRegister) onSignUp(email, password) else onLogin(email, password)
          },
          enabled = !isLoading,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("auth_submit_button")
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          } else {
            Text(if (isRegister) "Daftar Akun Baru" else "Masuk ke Sistem")
          }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
          onClick = {
            isRegister = !isRegister
            onClearMessage()
          }
        ) {
          Text(
            if (isRegister) "Sudah punya akun? Masuk di sini" else "Belum punya akun? Buat akun pengurus",
            color = Color(0xFF047857),
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

// ==========================================
// FITUR 2: Layar Dashboard (Ringkasan Keuangan)
// ==========================================
@Composable
fun DashboardView(
  transaksiList: List<Transaksi>,
  santriList: List<Santri>,
  bulanTerpilih: String,
  onBulanChanged: (String) -> Unit,
  onNavToTransaksi: () -> Unit,
  onNavToSantri: () -> Unit
) {
  val filteredTransaksi = remember(transaksiList, bulanTerpilih) {
    if (bulanTerpilih == "semua") transaksiList
    else transaksiList.filter { it.tanggal.startsWith(bulanTerpilih) }
  }

  val totalMasuk = remember(filteredTransaksi) {
    filteredTransaksi.filter { it.jenis == "masuk" }.sumOf { it.jumlah }
  }
  val totalKeluar = remember(filteredTransaksi) {
    filteredTransaksi.filter { it.jenis == "keluar" }.sumOf { it.jumlah }
  }
  val saldo = totalMasuk - totalKeluar
  val santriAktifCount = remember(santriList) { santriList.count { it.aktif } }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Filter Bulan
    item {
      BulanPickerRow(bulanTerpilih = bulanTerpilih, onBulanSelected = onBulanChanged)
    }

    // Kartu Saldo Utama
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Saldo Kas TPQ", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B))
          Spacer(Modifier.height(4.dp))
          Text(
            formatRupiah(saldo),
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFF047857)
            )
          )
          Spacer(Modifier.height(8.dp))
          Surface(
            color = if (saldo >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
            shape = RoundedCornerShape(20.dp)
          ) {
            Text(
              if (saldo >= 0) "✓ Kondisi Surplus" else "⚠ Kondisi Defisit",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              color = if (saldo >= 0) Color(0xFF15803D) else Color(0xFFB91C1C),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
          }

          Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

          Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Total Pemasukan", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
              Text(
                formatRupiah(totalMasuk),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF059669)
                )
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Total Pengeluaran", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
              Text(
                formatRupiah(totalKeluar),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFDC2626)
                )
              )
            }
          }
        }
      }
    }

    // Tombol Aksi Cepat
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onNavToTransaksi,
          modifier = Modifier.weight(1f).height(48.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Catat Transaksi")
        }

        OutlinedButton(
          onClick = onNavToSantri,
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF047857))
          Spacer(Modifier.width(6.dp))
          Text("Kelola Santri ($santriAktifCount)", color = Color(0xFF047857))
        }
      }
    }

    // Riwayat Terkini
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Riwayat Terkini", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        TextButton(onClick = onNavToTransaksi) {
          Text("Lihat Semua →", color = Color(0xFF047857), fontWeight = FontWeight.Bold)
        }
      }
    }

    val riwayatSingkat = filteredTransaksi.take(5)
    if (riwayatSingkat.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
          Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Belum ada transaksi pada periode ini.", color = Color(0xFF94A3B8))
          }
        }
      }
    } else {
      items(riwayatSingkat, key = { it.id }) { tx ->
        val santri = tx.santriId?.let { sid -> santriList.find { it.id == sid } }
        TransactionItemCard(tx = tx, santri = santri, onDelete = null)
      }
    }
  }
}

// ==========================================
// FITUR 3 & 4: Layar Transaksi
// ==========================================
@Composable
fun TransaksiView(
  transaksiList: List<Transaksi>,
  santriList: List<Santri>,
  bulanTerpilih: String,
  onBulanChanged: (String) -> Unit,
  onAddTransaksi: (String, String, String, Long, String?, String?, () -> Unit) -> Unit,
  onDeleteTransaksi: (String) -> Unit
) {
  var viewMode by remember { mutableStateOf("daftar") } // "daftar" atau "tambah"
  var selectedJenisFilter by remember { mutableStateOf("semua") } // "semua", "masuk", "keluar"
  var searchQuery by remember { mutableStateOf("") }
  var txToDelete by remember { mutableStateOf<Transaksi?>(null) }

  // Form states
  val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  var tanggal by remember { mutableStateOf(today) }
  var jenis by remember { mutableStateOf("masuk") }
  var kategori by remember { mutableStateOf("SPP") }
  var jumlahStr by remember { mutableStateOf("") }
  var selectedSantriId by remember { mutableStateOf<String?>(null) }
  var keterangan by remember { mutableStateOf("") }
  var formError by remember { mutableStateOf<String?>(null) }

  val kategoriOptions = listOf("SPP", "Infaq", "Donasi", "Gaji Ustadz", "Listrik", "Lainnya")

  // Dialog Konfirmasi Hapus Transaksi (Fitur 4)
  txToDelete?.let { tx ->
    AlertDialog(
      onDismissRequest = { txToDelete = null },
      title = { Text("Konfirmasi Hapus Transaksi") },
      text = {
        Text("Apakah Anda yakin ingin menghapus transaksi ${tx.kategori} sebesar ${formatRupiah(tx.jumlah)}?")
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          onClick = {
            onDeleteTransaksi(tx.id)
            txToDelete = null
          }
        ) {
          Text("Ya, Hapus")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { txToDelete = null }) {
          Text("Batal")
        }
      }
    )
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    // Tab selector (Daftar Transaksi / Catat Baru)
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
      Button(
        onClick = { viewMode = "daftar" },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (viewMode == "daftar") Color(0xFF047857) else Color(0xFFE2E8F0),
          contentColor = if (viewMode == "daftar") Color.White else Color(0xFF0F172A)
        ),
        modifier = Modifier.weight(1f).height(44.dp)
      ) {
        Text("📋 Daftar Transaksi")
      }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = { viewMode = "tambah" },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (viewMode == "tambah") Color(0xFF047857) else Color(0xFFE2E8F0),
          contentColor = if (viewMode == "tambah") Color.White else Color(0xFF0F172A)
        ),
        modifier = Modifier.weight(1f).height(44.dp)
      ) {
        Text("➕ Catat Baru")
      }
    }

    if (viewMode == "tambah") {
      // FORM TRANSAKSI BARU (Fitur 3)
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Catat Transaksi Kas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Spacer(Modifier.height(12.dp))

          formError?.let {
            Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
          }

          // Segmented Control: Masuk vs Keluar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
              .padding(4.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .background(
                  if (jenis == "masuk") Color(0xFF059669) else Color.Transparent,
                  RoundedCornerShape(8.dp)
                )
                .clickable {
                  jenis = "masuk"
                  if (kategori == "Gaji Ustadz" || kategori == "Listrik") kategori = "SPP"
                }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "🟢 Pemasukan",
                color = if (jenis == "masuk") Color.White else Color(0xFF64748B),
                fontWeight = FontWeight.Bold
              )
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .background(
                  if (jenis == "keluar") Color(0xFFDC2626) else Color.Transparent,
                  RoundedCornerShape(8.dp)
                )
                .clickable {
                  jenis = "keluar"
                  if (kategori == "SPP" || kategori == "Infaq" || kategori == "Donasi") kategori = "Gaji Ustadz"
                }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "🔴 Pengeluaran",
                color = if (jenis == "keluar") Color.White else Color(0xFF64748B),
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(Modifier.height(12.dp))

          OutlinedTextField(
            value = tanggal,
            onValueChange = { tanggal = it },
            label = { Text("Tanggal (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(Modifier.height(10.dp))

          // Kategori Dropdown / Selector
          Text("Kategori:", style = MaterialTheme.typography.labelMedium)
          Spacer(Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            kategoriOptions.take(3).forEach { opt ->
              FilterChip(
                selected = kategori == opt,
                onClick = { kategori = opt },
                label = { Text(opt, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF047857),
                  selectedLabelColor = Color.White
                )
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            kategoriOptions.drop(3).forEach { opt ->
              FilterChip(
                selected = kategori == opt,
                onClick = { kategori = opt },
                label = { Text(opt, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF047857),
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          Spacer(Modifier.height(10.dp))

          OutlinedTextField(
            value = jumlahStr,
            onValueChange = { jumlahStr = it.filter { ch -> ch.isDigit() } },
            label = { Text("Jumlah Nominal (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
              val num = jumlahStr.toLongOrNull() ?: 0L
              if (num > 0) Text("Format: ${formatRupiah(num)}", color = Color(0xFF047857), fontWeight = FontWeight.Bold)
            }
          )

          Spacer(Modifier.height(10.dp))

          // Santri Dropdown
          val santriAktif = santriList.filter { it.aktif }
          Text("Santri Terkait (Opsional):", style = MaterialTheme.typography.labelMedium)
          Spacer(Modifier.height(4.dp))
          var santriDropdownExpanded by remember { mutableStateOf(false) }
          val selectedSantri = santriAktif.find { it.id == selectedSantriId }

          OutlinedButton(
            onClick = { santriDropdownExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              selectedSantri?.let { "${it.nama} (Kelas ${it.kelas})" } ?: "-- Tanpa Santri / Umum --",
              modifier = Modifier.weight(1f),
              textAlign = TextAlign.Start
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
          }

          DropdownMenu(
            expanded = santriDropdownExpanded,
            onDismissRequest = { santriDropdownExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("-- Tanpa Santri / Umum --") },
              onClick = {
                selectedSantriId = null
                santriDropdownExpanded = false
              }
            )
            santriAktif.forEach { s ->
              DropdownMenuItem(
                text = { Text("${s.nama} (${s.kelas})") },
                onClick = {
                  selectedSantriId = s.id
                  santriDropdownExpanded = false
                }
              )
            }
          }

          Spacer(Modifier.height(10.dp))

          OutlinedTextField(
            value = keterangan,
            onValueChange = { keterangan = it },
            label = { Text("Keterangan Tambahan") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
          )

          Spacer(Modifier.height(16.dp))

          Button(
            onClick = {
              val jml = jumlahStr.toLongOrNull()
              if (jml == null || jml <= 0L) {
                formError = "Jumlah harus berupa nominal angka valid lebih dari 0!"
                return@Button
              }
              formError = null
              onAddTransaksi(tanggal, jenis, kategori, jml, selectedSantriId, keterangan) {
                // Reset form & kembali ke daftar
                jumlahStr = ""
                keterangan = ""
                selectedSantriId = null
                viewMode = "daftar"
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Text("💾 Simpan Transaksi")
          }
        }
      }
    } else {
      // DAFTAR TRANSAKSI DENGAN FILTER BULAN & HAPUS (Fitur 4)
      BulanPickerRow(bulanTerpilih = bulanTerpilih, onBulanSelected = onBulanChanged)

      Spacer(Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari...", fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          modifier = Modifier.weight(1.2f).height(50.dp),
          singleLine = true
        )

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilterChip(
            selected = selectedJenisFilter == "semua",
            onClick = { selectedJenisFilter = "semua" },
            label = { Text("Semua", fontSize = 11.sp) }
          )
          FilterChip(
            selected = selectedJenisFilter == "masuk",
            onClick = { selectedJenisFilter = "masuk" },
            label = { Text("Masuk", fontSize = 11.sp) }
          )
          FilterChip(
            selected = selectedJenisFilter == "keluar",
            onClick = { selectedJenisFilter = "keluar" },
            label = { Text("Keluar", fontSize = 11.sp) }
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      val filteredList = remember(transaksiList, bulanTerpilih, selectedJenisFilter, searchQuery) {
        transaksiList.filter { tx ->
          if (bulanTerpilih != "semua" && !tx.tanggal.startsWith(bulanTerpilih)) return@filter false
          if (selectedJenisFilter != "semua" && tx.jenis != selectedJenisFilter) return@filter false
          if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            val matchKet = tx.keterangan?.lowercase()?.contains(q) == true
            val matchKat = tx.kategori.lowercase().contains(q)
            val matchSantri = tx.santriId?.let { sid -> santriList.find { it.id == sid }?.nama?.lowercase()?.contains(q) } ?: false
            if (!matchKet && !matchKat && !matchSantri) return@filter false
          }
          true
        }
      }

      if (filteredList.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Tidak ada transaksi sesuai filter.", color = Color(0xFF94A3B8))
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredList, key = { it.id }) { tx ->
            val santri = tx.santriId?.let { sid -> santriList.find { it.id == sid } }
            TransactionItemCard(
              tx = tx,
              santri = santri,
              onDelete = { txToDelete = tx }
            )
          }
        }
      }
    }
  }
}

// ==========================================
// FITUR 5: Layar Santri (Tambah, Daftar, Nonaktifkan)
// ==========================================
@Composable
fun SantriView(
  santriList: List<Santri>,
  onAddSantri: (String, String, () -> Unit) -> Unit,
  onToggleStatus: (String, Boolean) -> Unit
) {
  var showForm by remember { mutableStateOf(false) }
  var namaSantri by remember { mutableStateOf("") }
  var kelasSantri by remember { mutableStateOf("Jilid 1") }
  var statusFilter by remember { mutableStateOf("semua") } // "semua", "aktif", "nonaktif"
  var searchSantri by remember { mutableStateOf("") }
  var santriToToggle by remember { mutableStateOf<Santri?>(null) }

  val daftarKelas = listOf("Jilid 1", "Jilid 2", "Jilid 3", "Jilid 4", "Jilid 5", "Jilid 6", "Al-Qur'an", "Tajwid")

  // Dialog Konfirmasi Nonaktifkan / Aktifkan Santri
  santriToToggle?.let { s ->
    val targetAktif = !s.aktif
    AlertDialog(
      onDismissRequest = { santriToToggle = null },
      title = { Text(if (targetAktif) "Aktifkan Santri" else "Nonaktifkan Santri") },
      text = {
        Text(
          if (targetAktif) "Aktifkan kembali santri ${s.nama}?"
          else "Apakah Anda yakin ingin menonaktifkan santri ${s.nama}? Santri tidak akan muncul di opsi transaksi baru."
        )
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(
            containerColor = if (targetAktif) Color(0xFF047857) else MaterialTheme.colorScheme.error
          ),
          onClick = {
            onToggleStatus(s.id, targetAktif)
            santriToToggle = null
          }
        ) {
          Text(if (targetAktif) "Ya, Aktifkan" else "Ya, Nonaktifkan")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { santriToToggle = null }) {
          Text("Batal")
        }
      }
    )
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    // Ringkasan Jumlah Santri
    val total = santriList.size
    val aktifCount = santriList.count { it.aktif }
    val nonaktifCount = total - aktifCount

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatChip(label = "Total", count = total, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
      StatChip(label = "Aktif", count = aktifCount, color = Color(0xFF059669), modifier = Modifier.weight(1f))
      StatChip(label = "Nonaktif", count = nonaktifCount, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
    }

    Button(
      onClick = { showForm = !showForm },
      modifier = Modifier.fillMaxWidth().height(44.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (showForm) Color(0xFFE2E8F0) else Color(0xFF047857),
        contentColor = if (showForm) Color(0xFF0F172A) else Color.White
      )
    ) {
      Text(if (showForm) "✕ Tutup Form Tambah" else "➕ Tambah Santri Baru")
    }

    if (showForm) {
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Pendaftaran Santri Baru", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Spacer(Modifier.height(10.dp))

          OutlinedTextField(
            value = namaSantri,
            onValueChange = { namaSantri = it },
            label = { Text("Nama Lengkap Santri") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(Modifier.height(10.dp))

          Text("Kelas / Tingkat:", style = MaterialTheme.typography.labelMedium)
          Spacer(Modifier.height(4.dp))
          var kelasExpanded by remember { mutableStateOf(false) }

          OutlinedButton(
            onClick = { kelasExpanded = true },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(kelasSantri, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
          }

          DropdownMenu(
            expanded = kelasExpanded,
            onDismissRequest = { kelasExpanded = false }
          ) {
            daftarKelas.forEach { k ->
              DropdownMenuItem(
                text = { Text(k) },
                onClick = {
                  kelasSantri = k
                  kelasExpanded = false
                }
              )
            }
          }

          Spacer(Modifier.height(14.dp))

          Button(
            onClick = {
              if (namaSantri.isNotBlank()) {
                onAddSantri(namaSantri, kelasSantri) {
                  namaSantri = ""
                  kelasSantri = "Jilid 1"
                  showForm = false
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Text("💾 Simpan Data Santri")
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))

    // Filter & Pencarian
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = searchSantri,
        onValueChange = { searchSantri = it },
        placeholder = { Text("Cari nama/kelas...", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.weight(1.2f).height(50.dp),
        singleLine = true
      )

      Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
          selected = statusFilter == "semua",
          onClick = { statusFilter = "semua" },
          label = { Text("Semua", fontSize = 11.sp) }
        )
        FilterChip(
          selected = statusFilter == "aktif",
          onClick = { statusFilter = "aktif" },
          label = { Text("Aktif", fontSize = 11.sp) }
        )
        FilterChip(
          selected = statusFilter == "nonaktif",
          onClick = { statusFilter = "nonaktif" },
          label = { Text("Nonaktif", fontSize = 11.sp) }
        )
      }
    }

    Spacer(Modifier.height(10.dp))

    val filteredSantri = remember(santriList, statusFilter, searchSantri) {
      santriList.filter { s ->
        if (statusFilter == "aktif" && !s.aktif) return@filter false
        if (statusFilter == "nonaktif" && s.aktif) return@filter false
        if (searchSantri.isNotBlank()) {
          val q = searchSantri.lowercase()
          val matchNama = s.nama.lowercase().contains(q)
          val matchKelas = s.kelas.lowercase().contains(q)
          if (!matchNama && !matchKelas) return@filter false
        }
        true
      }
    }

    if (filteredSantri.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
      ) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
          Text("Tidak ada santri ditemukan.", color = Color(0xFF94A3B8))
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredSantri, key = { it.id }) { santri ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(
                    if (santri.aktif) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                    CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  santri.nama.take(1).uppercase(),
                  fontWeight = FontWeight.Bold,
                  color = if (santri.aktif) Color(0xFF047857) else Color(0xFF64748B)
                )
              }

              Spacer(Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(santri.nama, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("Kelas ${santri.kelas}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                  Surface(
                    color = if (santri.aktif) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text(
                      if (santri.aktif) "Aktif" else "Nonaktif",
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      color = if (santri.aktif) Color(0xFF15803D) else Color(0xFF64748B),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              if (santri.aktif) {
                OutlinedButton(
                  onClick = { santriToToggle = santri },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Text("Nonaktifkan", fontSize = 12.sp)
                }
              } else {
                Button(
                  onClick = { santriToToggle = santri },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Text("Aktifkan", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

// ==========================================
// Sub-Komponen Pembantu
// ==========================================
@Composable
fun TransactionItemCard(
  tx: Transaksi,
  santri: Santri?,
  onDelete: (() -> Unit)?
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(
            if (tx.jenis == "masuk") Color(0xFFECFDF5) else Color(0xFFFEF2F2),
            RoundedCornerShape(10.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          if (tx.jenis == "masuk") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
          contentDescription = null,
          tint = if (tx.jenis == "masuk") Color(0xFF059669) else Color(0xFFDC2626)
        )
      }

      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(tx.kategori, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          santri?.let {
            Text(" • ${it.nama}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF047857), fontWeight = FontWeight.SemiBold))
          }
        }
        Text(formatTanggalIndo(tx.tanggal), style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        tx.keterangan?.let {
          if (it.isNotBlank()) {
            Text(
              it,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF475569),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          "${if (tx.jenis == "masuk") "+" else "-"} ${formatRupiah(tx.jumlah)}",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            color = if (tx.jenis == "masuk") Color(0xFF059669) else Color(0xFFDC2626)
          )
        )

        onDelete?.let {
          IconButton(
            onClick = it,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
fun BulanPickerRow(
  bulanTerpilih: String,
  onBulanSelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  val daftarBulan = remember {
    val list = mutableListOf<Pair<String, String>>()
    list.add("semua" to "Semua Periode")
    val cal = Calendar.getInstance()
    for (i in 0 until 12) {
      val valStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
      val labelStr = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(cal.time)
      list.add(valStr to labelStr)
      cal.add(Calendar.MONTH, -1)
    }
    list
  }

  val selectedLabel = daftarBulan.find { it.first == bulanTerpilih }?.second ?: bulanTerpilih

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(12.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF047857))
      Spacer(Modifier.width(8.dp))
      Text("Periode: ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
      Text(selectedLabel, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
    }

    Box {
      IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Bulan")
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        daftarBulan.forEach { (value, label) ->
          DropdownMenuItem(
            text = { Text(label) },
            onClick = {
              onBulanSelected(value)
              expanded = false
            }
          )
        }
      }
    }
  }
}

@Composable
fun StatChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
      Text("$count", fontSize = 18.sp, color = color, fontWeight = FontWeight.ExtraBold)
    }
  }
}
