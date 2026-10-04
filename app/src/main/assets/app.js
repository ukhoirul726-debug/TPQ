/**
 * ============================================================================
 * Aplikasi Keuangan TPQ (Taman Pendidikan Al-Qur'an)
 * Berbahasa Indonesia, Mobile-First, Menggunakan React & @supabase/supabase-js
 * ============================================================================
 */

(function () {
  "use strict";

  const { useState, useEffect, useMemo, createElement: h } = React;

  // 1. Inisialisasi Client Supabase dari config.js
  if (!window.SUPABASE_URL || !window.SUPABASE_ANON_KEY) {
    console.error("Konfigurasi Supabase tidak ditemukan di config.js!");
  }

  // Fallback penyimpanan aman jika localStorage dibatasi di file:// WebView
  const safeStorage = {
    getItem: function (key) {
      try {
        return window.localStorage.getItem(key);
      } catch (e) {
        return window.__memStore ? window.__memStore[key] || null : null;
      }
    },
    setItem: function (key, val) {
      try {
        window.localStorage.setItem(key, val);
      } catch (e) {
        if (!window.__memStore) window.__memStore = {};
        window.__memStore[key] = val;
      }
    },
    removeItem: function (key) {
      try {
        window.localStorage.removeItem(key);
      } catch (e) {
        if (window.__memStore) delete window.__memStore[key];
      }
    },
  };

  const supabase = window.supabase
    ? window.supabase.createClient(window.SUPABASE_URL, window.SUPABASE_ANON_KEY, {
        auth: {
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: false,
          storage: safeStorage,
        },
      })
    : null;

  // 2. Fungsi Pembantu: Format Mata Uang Rupiah Indonesia
  function formatRupiah(amount) {
    const num = Number(amount) || 0;
    return new Intl.NumberFormat("id-ID", {
      style: "currency",
      currency: "IDR",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(num);
  }

  // Format Tanggal Indonesia (contoh: 04 Oktober 2026)
  function formatTanggalIndo(dateStr) {
    if (!dateStr) return "-";
    try {
      const parts = dateStr.split("-");
      if (parts.length === 3) {
        const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        return d.toLocaleDateString("id-ID", { day: "numeric", month: "short", year: "numeric" });
      }
      return new Date(dateStr).toLocaleDateString("id-ID", { day: "numeric", month: "short", year: "numeric" });
    } catch (e) {
      return dateStr;
    }
  }

  // Ambil tahun-bulan saat ini (YYYY-MM)
  function getBulanSekarang() {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, "0");
    return `${yyyy}-${mm}`;
  }

  // ============================================================================
  // Komponen Notifikasi / Alert
  // ============================================================================
  function Alert({ type, message, onClose }) {
    if (!message) return null;
    return h(
      "div",
      { className: `alert alert-${type || "info"}` },
      h("span", { style: { fontSize: "16px" } }, type === "error" ? "⚠️" : type === "success" ? "✅" : "ℹ️"),
      h("div", { style: { flex: 1 } }, message),
      onClose &&
        h(
          "button",
          {
            onClick: onClose,
            style: { background: "none", border: "none", cursor: "pointer", fontSize: "16px", color: "inherit" },
          },
          "✕"
        )
    );
  }

  // ============================================================================
  // Komponen Modal Konfirmasi
  // ============================================================================
  function ModalConfirm({ isOpen, title, message, onConfirm, onCancel, confirmText = "Ya, Lanjutkan", isDanger = false, isLoading = false }) {
    if (!isOpen) return null;
    return h(
      "div",
      { className: "modal-overlay" },
      h(
        "div",
        { className: "modal-dialog" },
        h("div", { className: "modal-title" }, title),
        h("div", { className: "modal-desc" }, message),
        h(
          "div",
          { className: "modal-footer" },
          h(
            "button",
            { className: "btn btn-secondary", onClick: onCancel, disabled: isLoading },
            "Batal"
          ),
          h(
            "button",
            {
              className: `btn ${isDanger ? "btn-danger" : "btn-primary"}`,
              onClick: onConfirm,
              disabled: isLoading,
            },
            isLoading ? h("span", { className: "spinner" }) : confirmText
          )
        )
      )
    );
  }

  // ============================================================================
  // FITUR 1: Komponen Autentikasi (Login & Registrasi Supabase Auth)
  // ============================================================================
  function AuthScreen({ onLoginSuccess }) {
    const [isRegister, setIsRegister] = useState(false);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [successMsg, setSuccessMsg] = useState("");

    const handleSubmit = async (e) => {
      e.preventDefault();
      setErrorMsg("");
      setSuccessMsg("");

      if (!email.trim() || !password.trim()) {
        setErrorMsg("Email dan kata sandi wajib diisi!");
        return;
      }

      if (password.length < 6) {
        setErrorMsg("Kata sandi minimal harus 6 karakter!");
        return;
      }

      setLoading(true);
      try {
        if (isRegister) {
          // Registrasi akun baru di Supabase Auth
          const { data, error } = await supabase.auth.signUp({
            email: email.trim(),
            password: password,
          });

          if (error) throw error;

          if (data.session) {
            setSuccessMsg("Pendaftaran berhasil! Anda langsung masuk.");
            if (onLoginSuccess) onLoginSuccess(data.session);
          } else {
            setSuccessMsg("Pendaftaran berhasil! Silakan periksa email Anda untuk konfirmasi atau coba masuk.");
            setIsRegister(false);
          }
        } else {
          // Login dengan email dan password
          const { data, error } = await supabase.auth.signInWithPassword({
            email: email.trim(),
            password: password,
          });

          if (error) {
            if (error.message.includes("Invalid login credentials")) {
              throw new Error("Email atau kata sandi yang Anda masukkan salah.");
            } else if (error.message.includes("Email not confirmed")) {
              throw new Error("Email belum dikonfirmasi. Silakan periksa inbox email Anda.");
            }
            throw error;
          }

          if (data.session && onLoginSuccess) {
            onLoginSuccess(data.session);
          }
        }
      } catch (err) {
        setErrorMsg(err.message || "Terjadi kesalahan saat menghubungi server.");
      } finally {
        setLoading(false);
      }
    };

    return h(
      "div",
      { className: "auth-wrapper" },
      h(
        "div",
        { className: "auth-card" },
        h(
          "div",
          { className: "auth-header" },
          h("div", { className: "auth-logo" }, "📖"),
          h("h1", { className: "auth-title" }, "Keuangan TPQ"),
          h("p", { className: "auth-subtitle" }, "Sistem Pengelolaan Kas & Administrasi Santri")
        ),

        h(Alert, { type: "error", message: errorMsg, onClose: () => setErrorMsg("") }),
        h(Alert, { type: "success", message: successMsg, onClose: () => setSuccessMsg("") }),

        h(
          "form",
          { onSubmit: handleSubmit },
          h(
            "div",
            { className: "form-group" },
            h("label", { className: "form-label" }, "Alamat Email"),
            h("input", {
              type: "email",
              className: "form-input",
              placeholder: "ustadz@tpq.com",
              value: email,
              onChange: (e) => setEmail(e.target.value),
              required: true,
              autoComplete: "email",
            })
          ),
          h(
            "div",
            { className: "form-group" },
            h("label", { className: "form-label" }, "Kata Sandi"),
            h("input", {
              type: "password",
              className: "form-input",
              placeholder: "Minimal 6 karakter",
              value: password,
              onChange: (e) => setPassword(e.target.value),
              required: true,
              autoComplete: isRegister ? "new-password" : "current-password",
            })
          ),
          h(
            "button",
            {
              type: "submit",
              className: "btn btn-primary btn-block",
              disabled: loading,
              style: { marginTop: "10px" },
            },
            loading
              ? h("span", { className: "spinner" })
              : isRegister
              ? "Daftar Akun Baru"
              : "Masuk ke Sistem"
          )
        ),

        h(
          "div",
          {
            style: {
              textAlign: "center",
              marginTop: "20px",
              paddingTop: "16px",
              borderTop: "1px solid var(--border)",
              fontSize: "13px",
              color: "var(--text-muted)",
            },
          },
          isRegister
            ? [
                "Sudah punya akun pengurus? ",
                h(
                  "a",
                  {
                    href: "#",
                    onClick: (e) => {
                      e.preventDefault();
                      setIsRegister(false);
                      setErrorMsg("");
                      setSuccessMsg("");
                    },
                    style: { color: "var(--primary)", fontWeight: 700, textDecoration: "none" },
                  },
                  "Masuk di sini"
                ),
              ]
            : [
                "Belum punya akun? ",
                h(
                  "a",
                  {
                    href: "#",
                    onClick: (e) => {
                      e.preventDefault();
                      setIsRegister(true);
                      setErrorMsg("");
                      setSuccessMsg("");
                    },
                    style: { color: "var(--primary)", fontWeight: 700, textDecoration: "none" },
                  },
                  "Daftar akun pengurus"
                ),
              ]
        )
      )
    );
  }

  // ============================================================================
  // FITUR 2: Dashboard Keuangan (Pemasukan, Pengeluaran, Saldo Bulan Terpilih)
  // ============================================================================
  function DashboardScreen({
    transaksiList,
    santriList,
    bulanTerpilih,
    setBulanTerpilih,
    daftarBulan,
    onNavToTransaksi,
    onNavToSantri,
  }) {
    // Hitung ringkasan berdasarkan bulan terpilih
    const ringkasan = useMemo(() => {
      let filtered = transaksiList;
      if (bulanTerpilih !== "semua") {
        filtered = transaksiList.filter((t) => t.tanggal && t.tanggal.startsWith(bulanTerpilih));
      }

      let totalMasuk = 0;
      let totalKeluar = 0;

      filtered.forEach((t) => {
        const nominal = Number(t.jumlah) || 0;
        if (t.jenis === "masuk") {
          totalMasuk += nominal;
        } else if (t.jenis === "keluar") {
          totalKeluar += nominal;
        }
      });

      const saldo = totalMasuk - totalKeluar;
      const count = filtered.length;

      return { totalMasuk, totalKeluar, saldo, count, filtered };
    }, [transaksiList, bulanTerpilih]);

    const santriAktifCount = useMemo(() => {
      return santriList.filter((s) => s.aktif).length;
    }, [santriList]);

    // Transaksi terbaru bulan ini (maks 4)
    const transaksiTerbaru = useMemo(() => {
      return ringkasan.filtered.slice(0, 4);
    }, [ringkasan.filtered]);

    return h(
      "div",
      null,
      // Baris Filter Bulan
      h(
        "div",
        { className: "filter-row" },
        h("label", null, "📅 Periode:"),
        h(
          "select",
          {
            value: bulanTerpilih,
            onChange: (e) => setBulanTerpilih(e.target.value),
          },
          h("option", { value: "semua" }, "Semua Periode Transaksi"),
          daftarBulan.map((bln) =>
            h("option", { key: bln.value, value: bln.value }, bln.label)
          )
        )
      ),

      // Kartu Ringkasan (Pemasukan, Pengeluaran, Saldo)
      h(
        "div",
        { className: "card", style: { padding: "16px" } },
        h(
          "div",
          { className: "summary-grid" },
          // Kartu Saldo Akhir
          h(
            "div",
            { className: "summary-card balance" },
            h("div", { className: "summary-label" }, "💰 Saldo Kas Kasih TPQ"),
            h(
              "div",
              { className: "summary-amount balance" },
              formatRupiah(ringkasan.saldo)
            ),
            h(
              "span",
              {
                className: `balance-status-badge ${
                  ringkasan.saldo >= 0 ? "badge-surplus" : "badge-defisit"
                }`,
              },
              ringkasan.saldo >= 0 ? "✓ Kondisi Surplus" : "⚠ Kondisi Defisit"
            )
          ),

          // Total Pemasukan
          h(
            "div",
            { className: "summary-card income" },
            h("div", { className: "summary-label" }, "🟢 Total Pemasukan"),
            h(
              "div",
              { className: "summary-amount income" },
              formatRupiah(ringkasan.totalMasuk)
            )
          ),

          // Total Pengeluaran
          h(
            "div",
            { className: "summary-card expense" },
            h("div", { className: "summary-label" }, "🔴 Total Pengeluaran"),
            h(
              "div",
              { className: "summary-amount expense" },
              formatRupiah(ringkasan.totalKeluar)
            )
          )
        ),

        // Statistik Cepat
        h(
          "div",
          {
            style: {
              display: "flex",
              justifyContent: "space-between",
              paddingTop: "12px",
              borderTop: "1px solid var(--border)",
              fontSize: "12px",
              color: "var(--text-muted)",
            },
          },
          h("span", null, `Total ${ringkasan.count} transaksi pada periode ini`),
          h("span", null, `👥 ${santriAktifCount} Santri Aktif`)
        )
      ),

      // Tombol Aksi Cepat
      h(
        "div",
        { style: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "16px" } },
        h(
          "button",
          {
            className: "btn btn-primary",
            onClick: onNavToTransaksi,
          },
          "➕ Catat Transaksi"
        ),
        h(
          "button",
          {
            className: "btn btn-secondary",
            onClick: onNavToSantri,
          },
          "👥 Kelola Santri"
        )
      ),

      // Daftar Transaksi Terakhir di Periode Ini
      h(
        "div",
        { className: "card" },
        h(
          "div",
          { className: "card-title" },
          h("span", null, "Riwayat Terkini"),
          h(
            "button",
            {
              onClick: onNavToTransaksi,
              style: {
                background: "none",
                border: "none",
                color: "var(--primary)",
                fontSize: "13px",
                fontWeight: 700,
                cursor: "pointer",
              },
            },
            "Lihat Semua →"
          )
        ),

        transaksiTerbaru.length === 0
          ? h(
              "div",
              { className: "empty-state" },
              h("div", { className: "empty-icon" }, "📝"),
              h("div", { className: "empty-title" }, "Belum Ada Transaksi"),
              h("div", { className: "empty-desc" }, "Belum ada catatan keuangan pada periode ini.")
            )
          : transaksiTerbaru.map((tx) => {
              const santriObj = tx.santri_id ? santriList.find((s) => s.id === tx.santri_id) : null;
              return h(
                "div",
                { key: tx.id, className: "transaction-item" },
                h(
                  "div",
                  { className: "tx-left" },
                  h(
                    "div",
                    { className: `tx-badge-icon ${tx.jenis}` },
                    tx.jenis === "masuk" ? "↓" : "↑"
                  ),
                  h(
                    "div",
                    { className: "tx-info" },
                    h(
                      "div",
                      { className: "tx-title" },
                      tx.kategori,
                      santriObj ? ` - ${santriObj.nama}` : ""
                    ),
                    h(
                      "div",
                      { className: "tx-meta" },
                      h("span", null, formatTanggalIndo(tx.tanggal)),
                      tx.keterangan ? h("span", null, `• ${tx.keterangan}`) : null
                    )
                  )
                ),
                h(
                  "div",
                  { className: "tx-right" },
                  h(
                    "div",
                    { className: `tx-amount ${tx.jenis}` },
                    `${tx.jenis === "masuk" ? "+" : "-"} ${formatRupiah(tx.jumlah)}`
                  )
                )
              );
            })
      )
    );
  }

  // ============================================================================
  // FITUR 3 & 4: Halaman Transaksi (Form Tambah & Daftar dengan Filter + Hapus)
  // ============================================================================
  function TransaksiScreen({
    transaksiList,
    santriList,
    bulanTerpilih,
    setBulanTerpilih,
    daftarBulan,
    onRefreshData,
  }) {
    // Mode Tampilan: "daftar" atau "tambah"
    const [viewMode, setViewMode] = useState("daftar");

    // State Form Transaksi
    const todayStr = new Date().toISOString().split("T")[0];
    const [tanggal, setTanggal] = useState(todayStr);
    const [jenis, setJenis] = useState("masuk"); // 'masuk' atau 'keluar'
    const [kategori, setKategori] = useState("SPP");
    const [jumlah, setJumlah] = useState("");
    const [santriId, setSantriId] = useState("");
    const [keterangan, setKeterangan] = useState("");
    const [formLoading, setFormLoading] = useState(false);
    const [formError, setFormError] = useState("");
    const [formSuccess, setFormSuccess] = useState("");

    // State Filter & Pencarian Daftar Transaksi
    const [filterJenis, setFilterJenis] = useState("semua"); // 'semua', 'masuk', 'keluar'
    const [searchKeyword, setSearchKeyword] = useState("");

    // State Modal Konfirmasi Hapus
    const [deleteModal, setDeleteModal] = useState({ isOpen: false, item: null, loading: false });

    // Daftar Kategori Sesuai Permintaan Spesifikasi
    const kategoriOptions = ["SPP", "Infaq", "Donasi", "Gaji Ustadz", "Listrik", "Lainnya"];

    // Update kategori default saat toggle jenis transaksi
    const handleJenisChange = (newJenis) => {
      setJenis(newJenis);
      if (newJenis === "masuk" && (kategori === "Gaji Ustadz" || kategori === "Listrik")) {
        setKategori("SPP");
      } else if (newJenis === "keluar" && (kategori === "SPP" || kategori === "Infaq" || kategori === "Donasi")) {
        setKategori("Gaji Ustadz");
      }
    };

    // Handler Simpan Transaksi Baru
    const handleSubmitTransaksi = async (e) => {
      e.preventDefault();
      setFormError("");
      setFormSuccess("");

      const nominal = parseFloat(jumlah);
      if (isNaN(nominal) || nominal <= 0) {
        setFormError("Nominal jumlah transaksi harus berupa angka lebih dari 0!");
        return;
      }

      if (!tanggal) {
        setFormError("Tanggal transaksi wajib diisi!");
        return;
      }

      if (!kategori) {
        setFormError("Kategori transaksi wajib dipilih!");
        return;
      }

      setFormLoading(true);
      try {
        const payload = {
          tanggal: tanggal,
          jenis: jenis,
          kategori: kategori,
          jumlah: nominal,
          santri_id: santriId ? santriId : null,
          keterangan: keterangan.trim() || null,
        };

        const { error } = await supabase.from("transaksi").insert([payload]);
        if (error) throw error;

        setFormSuccess("Transaksi berhasil disimpan!");
        // Reset form
        setJumlah("");
        setKeterangan("");
        setSantriId("");
        setTanggal(todayStr);

        // Segarkan data
        if (onRefreshData) await onRefreshData();

        // Pindah kembali ke daftar setelah 1.2 detik
        setTimeout(() => {
          setViewMode("daftar");
          setFormSuccess("");
        }, 1200);
      } catch (err) {
        setFormError(err.message || "Gagal menyimpan transaksi. Pastikan tabel Supabase terhubung.");
      } finally {
        setFormLoading(false);
      }
    };

    // Handler Buka Modal Konfirmasi Hapus
    const handleOpenDelete = (item) => {
      setDeleteModal({ isOpen: true, item: item, loading: false });
    };

    // Handler Eksekusi Hapus Transaksi
    const handleConfirmDelete = async () => {
      if (!deleteModal.item) return;
      setDeleteModal((prev) => ({ ...prev, loading: true }));
      try {
        const { error } = await supabase.from("transaksi").delete().eq("id", deleteModal.item.id);
        if (error) throw error;

        setDeleteModal({ isOpen: false, item: null, loading: false });
        if (onRefreshData) await onRefreshData();
      } catch (err) {
        alert("Gagal menghapus transaksi: " + (err.message || "Kesalahan jaringan"));
        setDeleteModal({ isOpen: false, item: null, loading: false });
      }
    };

    // Santri aktif untuk pilihan dropdown
    const santriAktif = useMemo(() => {
      return santriList.filter((s) => s.aktif);
    }, [santriList]);

    // Filter daftar transaksi
    const filteredTransaksi = useMemo(() => {
      return transaksiList.filter((tx) => {
        // Filter Bulan
        if (bulanTerpilih !== "semua" && tx.tanggal && !tx.tanggal.startsWith(bulanTerpilih)) {
          return false;
        }
        // Filter Jenis
        if (filterJenis !== "semua" && tx.jenis !== filterJenis) {
          return false;
        }
        // Filter Pencarian
        if (searchKeyword.trim()) {
          const q = searchKeyword.toLowerCase();
          const matchKet = tx.keterangan && tx.keterangan.toLowerCase().includes(q);
          const matchKat = tx.kategori && tx.kategori.toLowerCase().includes(q);
          const santriObj = tx.santri_id ? santriList.find((s) => s.id === tx.santri_id) : null;
          const matchSantri = santriObj && santriObj.nama.toLowerCase().includes(q);
          if (!matchKet && !matchKat && !matchSantri) return false;
        }
        return true;
      });
    }, [transaksiList, bulanTerpilih, filterJenis, searchKeyword, santriList]);

    return h(
      "div",
      null,
      // Navigasi Tab Sub-layar (Daftar vs Tambah)
      h(
        "div",
        {
          style: {
            display: "flex",
            gap: "8px",
            marginBottom: "16px",
          },
        },
        h(
          "button",
          {
            className: `btn ${viewMode === "daftar" ? "btn-primary" : "btn-secondary"}`,
            style: { flex: 1, minHeight: "44px" },
            onClick: () => setViewMode("daftar"),
          },
          "📋 Daftar Transaksi"
        ),
        h(
          "button",
          {
            className: `btn ${viewMode === "tambah" ? "btn-primary" : "btn-secondary"}`,
            style: { flex: 1, minHeight: "44px" },
            onClick: () => setViewMode("tambah"),
          },
          "➕ Catat Baru"
        )
      ),

      // Tampilan 1: Form Catat Transaksi Baru
      viewMode === "tambah" &&
        h(
          "div",
          { className: "card" },
          h("div", { className: "card-title" }, "Formulir Catat Transaksi"),

          h(Alert, { type: "error", message: formError, onClose: () => setFormError("") }),
          h(Alert, { type: "success", message: formSuccess, onClose: () => setFormSuccess("") }),

          h(
            "form",
            { onSubmit: handleSubmitTransaksi },
            // Segmented Button: Masuk / Keluar
            h("label", { className: "form-label" }, "Jenis Transaksi"),
            h(
              "div",
              { className: "segmented-control" },
              h(
                "button",
                {
                  type: "button",
                  className: `segment-btn ${jenis === "masuk" ? "active masuk" : ""}`,
                  onClick: () => handleJenisChange("masuk"),
                },
                "🟢 Pemasukan"
              ),
              h(
                "button",
                {
                  type: "button",
                  className: `segment-btn ${jenis === "keluar" ? "active keluar" : ""}`,
                  onClick: () => handleJenisChange("keluar"),
                },
                "🔴 Pengeluaran"
              )
            ),

            // Tanggal
            h(
              "div",
              { className: "form-group" },
              h("label", { className: "form-label" }, "Tanggal Transaksi"),
              h("input", {
                type: "date",
                className: "form-input",
                value: tanggal,
                onChange: (e) => setTanggal(e.target.value),
                required: true,
              })
            ),

            // Kategori
            h(
              "div",
              { className: "form-group" },
              h("label", { className: "form-label" }, "Kategori Transaksi"),
              h(
                "select",
                {
                  className: "form-select",
                  value: kategori,
                  onChange: (e) => setKategori(e.target.value),
                  required: true,
                },
                kategoriOptions.map((opt) =>
                  h("option", { key: opt, value: opt }, opt)
                )
              )
            ),

            // Jumlah (Nominal)
            h(
              "div",
              { className: "form-group" },
              h("label", { className: "form-label" }, "Jumlah Nominal (Rp)"),
              h("input", {
                type: "number",
                step: "1000",
                min: "0",
                className: "form-input",
                placeholder: "Contoh: 25000",
                value: jumlah,
                onChange: (e) => setJumlah(e.target.value),
                required: true,
              }),
              jumlah &&
                h(
                  "div",
                  { className: "rupiah-preview" },
                  `Format: ${formatRupiah(jumlah)}`
                )
            ),

            // Santri (Opsional)
            h(
              "div",
              { className: "form-group" },
              h(
                "label",
                { className: "form-label" },
                "Santri Terkait ",
                h("span", { className: "optional" }, "(Opsional, untuk SPP / Infaq)")
              ),
              h(
                "select",
                {
                  className: "form-select",
                  value: santriId,
                  onChange: (e) => setSantriId(e.target.value),
                },
                h("option", { value: "" }, "-- Tanpa Santri (Umum) --"),
                santriAktif.map((s) =>
                  h(
                    "option",
                    { key: s.id, value: s.id },
                    `${s.nama} (${s.kelas || "Tanpa Kelas"})`
                  )
                )
              )
            ),

            // Keterangan
            h(
              "div",
              { className: "form-group" },
              h("label", { className: "form-label" }, "Keterangan / Catatan"),
              h("textarea", {
                className: "form-textarea",
                placeholder: "Contoh: Pembayaran SPP bulan Oktober an. Fikri",
                rows: 2,
                value: keterangan,
                onChange: (e) => setKeterangan(e.target.value),
              })
            ),

            // Tombol Simpan
            h(
              "button",
              {
                type: "submit",
                className: "btn btn-primary btn-block",
                disabled: formLoading,
                style: { marginTop: "10px" },
              },
              formLoading ? h("span", { className: "spinner" }) : "💾 Simpan Transaksi"
            )
          )
        ),

      // Tampilan 2: Daftar Transaksi & Filter
      viewMode === "daftar" &&
        h(
          "div",
          null,
          // Filter Periode Bulan
          h(
            "div",
            { className: "filter-row" },
            h("label", null, "📅 Bulan:"),
            h(
              "select",
              {
                value: bulanTerpilih,
                onChange: (e) => setBulanTerpilih(e.target.value),
              },
              h("option", { value: "semua" }, "Semua Bulan"),
              daftarBulan.map((bln) =>
                h("option", { key: bln.value, value: bln.value }, bln.label)
              )
            )
          ),

          // Filter Pencarian & Jenis
          h(
            "div",
            {
              style: {
                display: "grid",
                gridTemplateColumns: "1.2fr 1fr",
                gap: "8px",
                marginBottom: "14px",
              },
            },
            h("input", {
              type: "text",
              className: "form-input",
              placeholder: "🔍 Cari keterangan...",
              value: searchKeyword,
              onChange: (e) => setSearchKeyword(e.target.value),
              style: { minHeight: "40px", fontSize: "13px" },
            }),
            h(
              "select",
              {
                className: "form-select",
                value: filterJenis,
                onChange: (e) => setFilterJenis(e.target.value),
                style: { minHeight: "40px", fontSize: "13px" },
              },
              h("option", { value: "semua" }, "Semua Jenis"),
              h("option", { value: "masuk" }, "🟢 Pemasukan"),
              h("option", { value: "keluar" }, "🔴 Pengeluaran")
            )
          ),

          // List Data Transaksi
          filteredTransaksi.length === 0
            ? h(
                "div",
                { className: "card" },
                h(
                  "div",
                  { className: "empty-state" },
                  h("div", { className: "empty-icon" }, "📭"),
                  h("div", { className: "empty-title" }, "Tidak Ada Transaksi"),
                  h(
                    "div",
                    { className: "empty-desc" },
                    "Tidak ditemukan transaksi yang cocok dengan kriteria filter."
                  )
                )
              )
            : filteredTransaksi.map((tx) => {
                const santriObj = tx.santri_id
                  ? santriList.find((s) => s.id === tx.santri_id)
                  : null;

                return h(
                  "div",
                  { key: tx.id, className: "transaction-item" },
                  h(
                    "div",
                    { className: "tx-left" },
                    h(
                      "div",
                      { className: `tx-badge-icon ${tx.jenis}` },
                      tx.jenis === "masuk" ? "↓" : "↑"
                    ),
                    h(
                      "div",
                      { className: "tx-info" },
                      h(
                        "div",
                        { className: "tx-title" },
                        tx.kategori,
                        santriObj ? ` • ${santriObj.nama}` : ""
                      ),
                      h(
                        "div",
                        { className: "tx-meta" },
                        h("span", null, formatTanggalIndo(tx.tanggal)),
                        santriObj && h("span", { style: { color: "var(--primary)" } }, `Kelas ${santriObj.kelas}`)
                      ),
                      tx.keterangan &&
                        h("div", { className: "tx-desc" }, tx.keterangan)
                    )
                  ),
                  h(
                    "div",
                    { className: "tx-right" },
                    h(
                      "div",
                      { className: `tx-amount ${tx.jenis}` },
                      `${tx.jenis === "masuk" ? "+" : "-"} ${formatRupiah(tx.jumlah)}`
                    ),
                    h(
                      "button",
                      {
                        className: "btn-danger-outline",
                        onClick: () => handleOpenDelete(tx),
                        title: "Hapus Transaksi",
                      },
                      "🗑 Hapus"
                    )
                  )
                );
              })
        ),

      // Modal Konfirmasi Hapus Transaksi (Fitur 4)
      h(ModalConfirm, {
        isOpen: deleteModal.isOpen,
        title: "Konfirmasi Hapus Transaksi",
        message: deleteModal.item
          ? `Apakah Anda yakin ingin menghapus transaksi "${deleteModal.item.kategori}" sebesar ${formatRupiah(
              deleteModal.item.jumlah
            )} pada tanggal ${formatTanggalIndo(deleteModal.item.tanggal)}?`
          : "",
        confirmText: "Ya, Hapus",
        isDanger: true,
        isLoading: deleteModal.loading,
        onConfirm: handleConfirmDelete,
        onCancel: () => setDeleteModal({ isOpen: false, item: null, loading: false }),
      })
    );
  }

  // ============================================================================
  // FITUR 5: Halaman Santri (Tambah, Daftar, Nonaktifkan)
  // ============================================================================
  function SantriScreen({ santriList, onRefreshData }) {
    // State Form Tambah Santri
    const [nama, setNama] = useState("");
    const [kelas, setKelas] = useState("Jilid 1");
    const [formLoading, setFormLoading] = useState(false);
    const [formError, setFormError] = useState("");
    const [formSuccess, setFormSuccess] = useState("");
    const [showForm, setShowForm] = useState(false);

    // State Filter & Pencarian
    const [statusFilter, setStatusFilter] = useState("semua"); // 'semua', 'aktif', 'nonaktif'
    const [searchName, setSearchName] = useState("");

    // State Modal Konfirmasi Status
    const [statusModal, setStatusModal] = useState({
      isOpen: false,
      santri: null,
      targetAktif: false,
      loading: false,
    });

    const daftarPilihanKelas = [
      "Jilid 1",
      "Jilid 2",
      "Jilid 3",
      "Jilid 4",
      "Jilid 5",
      "Jilid 6",
      "Al-Qur'an Dasar",
      "Al-Qur'an Lanjutan",
      "Tajwid",
      "Pasca TPQ",
    ];

    // Handler Tambah Santri
    const handleAddSantri = async (e) => {
      e.preventDefault();
      setFormError("");
      setFormSuccess("");

      if (!nama.trim()) {
        setFormError("Nama santri wajib diisi!");
        return;
      }

      setFormLoading(true);
      try {
        const payload = {
          nama: nama.trim(),
          kelas: kelas.trim(),
          aktif: true,
        };

        const { error } = await supabase.from("santri").insert([payload]);
        if (error) throw error;

        setFormSuccess(`Santri "${nama}" berhasil ditambahkan!`);
        setNama("");
        setKelas("Jilid 1");

        if (onRefreshData) await onRefreshData();

        setTimeout(() => {
          setShowForm(false);
          setFormSuccess("");
        }, 1200);
      } catch (err) {
        setFormError(err.message || "Gagal menambahkan santri.");
      } finally {
        setFormLoading(false);
      }
    };

    // Buka Modal Konfirmasi Ubah Status Aktif/Nonaktif
    const handlePromptToggleStatus = (santri) => {
      setStatusModal({
        isOpen: true,
        santri: santri,
        targetAktif: !santri.aktif,
        loading: false,
      });
    };

    // Eksekusi Ubah Status Aktif/Nonaktif Santri
    const handleConfirmToggleStatus = async () => {
      if (!statusModal.santri) return;
      setStatusModal((prev) => ({ ...prev, loading: true }));
      try {
        const { error } = await supabase
          .from("santri")
          .update({ aktif: statusModal.targetAktif })
          .eq("id", statusModal.santri.id);

        if (error) throw error;

        setStatusModal({ isOpen: false, santri: null, targetAktif: false, loading: false });
        if (onRefreshData) await onRefreshData();
      } catch (err) {
        alert("Gagal mengubah status santri: " + (err.message || "Kesalahan jaringan"));
        setStatusModal({ isOpen: false, santri: null, targetAktif: false, loading: false });
      }
    };

    // Filter daftar santri
    const filteredSantri = useMemo(() => {
      return santriList.filter((s) => {
        if (statusFilter === "aktif" && !s.aktif) return false;
        if (statusFilter === "nonaktif" && s.aktif) return false;
        if (searchName.trim()) {
          const q = searchName.toLowerCase();
          const matchNama = s.nama && s.nama.toLowerCase().includes(q);
          const matchKelas = s.kelas && s.kelas.toLowerCase().includes(q);
          if (!matchNama && !matchKelas) return false;
        }
        return true;
      });
    }, [santriList, statusFilter, searchName]);

    // Ringkasan jumlah santri
    const stats = useMemo(() => {
      const total = santriList.length;
      const aktif = santriList.filter((s) => s.aktif).length;
      const nonaktif = total - aktif;
      return { total, aktif, nonaktif };
    }, [santriList]);

    return h(
      "div",
      null,
      // Statistik Santri
      h(
        "div",
        {
          style: {
            display: "grid",
            gridTemplateColumns: "1fr 1fr 1fr",
            gap: "8px",
            marginBottom: "14px",
          },
        },
        h(
          "div",
          { className: "card", style: { padding: "12px", textAlign: "center", marginBottom: 0 } },
          h("div", { style: { fontSize: "11px", color: "var(--text-muted)", fontWeight: 600 } }, "Total Santri"),
          h("div", { style: { fontSize: "18px", fontWeight: 800, color: "var(--text-main)" } }, stats.total)
        ),
        h(
          "div",
          { className: "card", style: { padding: "12px", textAlign: "center", marginBottom: 0 } },
          h("div", { style: { fontSize: "11px", color: "var(--income)", fontWeight: 600 } }, "Santri Aktif"),
          h("div", { style: { fontSize: "18px", fontWeight: 800, color: "var(--income)" } }, stats.aktif)
        ),
        h(
          "div",
          { className: "card", style: { padding: "12px", textAlign: "center", marginBottom: 0 } },
          h("div", { style: { fontSize: "11px", color: "var(--text-muted)", fontWeight: 600 } }, "Nonaktif"),
          h("div", { style: { fontSize: "18px", fontWeight: 800, color: "var(--text-muted)" } }, stats.nonaktif)
        )
      ),

      // Tombol Toggle Form Tambah Santri
      h(
        "button",
        {
          className: `btn ${showForm ? "btn-secondary" : "btn-primary"} btn-block`,
          style: { marginBottom: "14px" },
          onClick: () => setShowForm(!showForm),
        },
        showForm ? "✕ Tutup Form Tambah" : "➕ Tambah Santri Baru"
      ),

      // Form Tambah Santri
      showForm &&
        h(
          "div",
          { className: "card" },
          h("div", { className: "card-title" }, "Pendaftaran Santri Baru"),

          h(Alert, { type: "error", message: formError, onClose: () => setFormError("") }),
          h(Alert, { type: "success", message: formSuccess, onClose: () => setFormSuccess("") }),

          h(
            "form",
            { onSubmit: handleAddSantri },
            h(
              "div",
              { className: "form-group" },
              h("label", { className: "form-label" }, "Nama Lengkap Santri"),
              h("input", {
                type: "text",
                className: "form-input",
                placeholder: "Contoh: Ahmad Fauzan",
                value: nama,
                onChange: (e) => setNama(e.target.value),
                required: true,
              })
            ),
            h(
              "div",
              { className: "form-group" },
              h("label", { className: "form-label" }, "Kelas / Tingkatan"),
              h(
                "select",
                {
                  className: "form-select",
                  value: kelas,
                  onChange: (e) => setKelas(e.target.value),
                },
                daftarPilihanKelas.map((k) =>
                  h("option", { key: k, value: k }, k)
                )
              )
            ),
            h(
              "button",
              {
                type: "submit",
                className: "btn btn-primary btn-block",
                disabled: formLoading,
                style: { marginTop: "10px" },
              },
              formLoading ? h("span", { className: "spinner" }) : "💾 Simpan Data Santri"
            )
          )
        ),

      // Filter Pencarian & Status Santri
      h(
        "div",
        {
          style: {
            display: "grid",
            gridTemplateColumns: "1.2fr 1fr",
            gap: "8px",
            marginBottom: "14px",
          },
        },
        h("input", {
          type: "text",
          className: "form-input",
          placeholder: "🔍 Cari nama/kelas...",
          value: searchName,
          onChange: (e) => setSearchName(e.target.value),
          style: { minHeight: "40px", fontSize: "13px" },
        }),
        h(
          "select",
          {
            className: "form-select",
            value: statusFilter,
            onChange: (e) => setStatusFilter(e.target.value),
            style: { minHeight: "40px", fontSize: "13px" },
          },
          h("option", { value: "semua" }, "Semua Status"),
          h("option", { value: "aktif" }, "🟢 Aktif Saja"),
          h("option", { value: "nonaktif" }, "⚪ Nonaktif Saja")
        )
      ),

      // List Data Santri
      filteredSantri.length === 0
        ? h(
            "div",
            { className: "card" },
            h(
              "div",
              { className: "empty-state" },
              h("div", { className: "empty-icon" }, "👥"),
              h("div", { className: "empty-title" }, "Tidak Ada Data Santri"),
              h("div", { className: "empty-desc" }, "Belum ada santri terdaftar atau sesuai filter.")
            )
          )
        : filteredSantri.map((santri) =>
            h(
              "div",
              { key: santri.id, className: "santri-item" },
              h(
                "div",
                null,
                h("div", { className: "santri-name" }, santri.nama),
                h(
                  "div",
                  { style: { display: "flex", alignItems: "center", gap: "8px", marginTop: "4px" } },
                  h("span", { className: "santri-class" }, `Kelas: ${santri.kelas || "-"}`),
                  h(
                    "span",
                    { className: `status-chip ${santri.aktif ? "aktif" : "nonaktif"}` },
                    santri.aktif ? "Aktif" : "Nonaktif"
                  )
                )
              ),
              h(
                "div",
                null,
                santri.aktif
                  ? h(
                      "button",
                      {
                        className: "btn-danger-outline",
                        onClick: () => handlePromptToggleStatus(santri),
                      },
                      "Nonaktifkan"
                    )
                  : h(
                      "button",
                      {
                        className: "btn btn-secondary",
                        style: { padding: "6px 10px", fontSize: "12px", minHeight: "36px" },
                        onClick: () => handlePromptToggleStatus(santri),
                      },
                      "Aktifkan"
                    )
              )
            )
          ),

      // Modal Konfirmasi Nonaktifkan Santri
      h(ModalConfirm, {
        isOpen: statusModal.isOpen,
        title: statusModal.targetAktif ? "Aktifkan Kembali Santri" : "Nonaktifkan Santri",
        message: statusModal.santri
          ? statusModal.targetAktif
            ? `Apakah Anda ingin mengaktifkan kembali santri "${statusModal.santri.nama}"?`
            : `Apakah Anda yakin ingin menonaktifkan santri "${statusModal.santri.nama}"? Santri tidak akan muncul di opsi pencatatan transaksi baru.`
          : "",
        confirmText: statusModal.targetAktif ? "Ya, Aktifkan" : "Ya, Nonaktifkan",
        isDanger: !statusModal.targetAktif,
        isLoading: statusModal.loading,
        onConfirm: handleConfirmToggleStatus,
        onCancel: () =>
          setStatusModal({ isOpen: false, santri: null, targetAktif: false, loading: false }),
      })
    );
  }

  // ============================================================================
  // KOMPONEN UTAMA (App Root)
  // ============================================================================
  function App() {
    // Sesi Pengguna Supabase Auth
    const [session, setSession] = useState(null);
    const [authLoading, setAuthLoading] = useState(true);

    // Navigasi Tab Utama: 'dashboard' | 'transaksi' | 'santri'
    const [activeTab, setActiveTab] = useState("dashboard");

    // Data dari Supabase
    const [transaksiList, setTransaksiList] = useState([]);
    const [santriList, setSantriList] = useState([]);
    const [dataLoading, setDataLoading] = useState(false);
    const [globalError, setGlobalError] = useState("");

    // State Filter Bulan Global
    const [bulanTerpilih, setBulanTerpilih] = useState(getBulanSekarang());

    // 1. Inisialisasi Sesi Supabase Auth saat Komponen Dimuat
    useEffect(() => {
      let isMounted = true;

      async function initAuth() {
        try {
          if (!supabase) {
            setGlobalError("Gagal menghubungkan ke Supabase. Periksa config.js!");
            setAuthLoading(false);
            return;
          }

          const { data, error } = await supabase.auth.getSession();
          if (error) console.warn("Pengecekan sesi:", error.message);

          if (isMounted) {
            setSession(data && data.session ? data.session : null);
            setAuthLoading(false);
          }
        } catch (e) {
          if (isMounted) {
            setAuthLoading(false);
          }
        }
      }

      initAuth();

      // Listener perubahan autentikasi
      let authSubscription = null;
      if (supabase) {
        const { data: sub } = supabase.auth.onAuthStateChange((_event, newSession) => {
          if (isMounted) {
            setSession(newSession);
          }
        });
        authSubscription = sub.subscription;
      }

      return () => {
        isMounted = false;
        if (authSubscription) authSubscription.unsubscribe();
      };
    }, []);

    // 2. Muat Data Transaksi & Santri Jika Sudah Login
    const fetchData = async () => {
      if (!session) return;
      setDataLoading(true);
      setGlobalError("");
      try {
        // Ambil data santri
        const { data: santriData, error: santriErr } = await supabase
          .from("santri")
          .select("*")
          .order("nama", { ascending: true });

        if (santriErr) throw santriErr;
        setSantriList(santriData || []);

        // Ambil data transaksi
        const { data: txData, error: txErr } = await supabase
          .from("transaksi")
          .select("*")
          .order("tanggal", { ascending: false })
          .order("id", { ascending: false });

        if (txErr) throw txErr;
        setTransaksiList(txData || []);
      } catch (err) {
        console.error("Fetch Data Error:", err);
        setGlobalError("Gagal memuat data dari Supabase: " + (err.message || "Periksa koneksi"));
      } finally {
        setDataLoading(false);
      }
    };

    useEffect(() => {
      if (session) {
        fetchData();
      }
    }, [session]);

    // Logout Handler
    const handleLogout = async () => {
      if (window.confirm("Apakah Anda yakin ingin keluar dari akun?")) {
        try {
          if (supabase) await supabase.auth.signOut();
          setSession(null);
          setTransaksiList([]);
          setSantriList([]);
        } catch (err) {
          console.error("Gagal logout:", err);
        }
      }
    };

    // Daftar Pilihan Bulan Dinamis (12 bulan terakhir)
    const daftarBulan = useMemo(() => {
      const options = [];
      const now = new Date();
      for (let i = 0; i < 12; i++) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const val = `${yyyy}-${mm}`;
        const label = d.toLocaleDateString("id-ID", { month: "long", year: "numeric" });
        options.push({ value: val, label: label });
      }
      return options;
    }, []);

    // Tampilan Loading Awal Sesi Auth
    if (authLoading) {
      return h(
        "div",
        {
          className: "mobile-wrapper",
          style: { alignItems: "center", justifyContent: "center" },
        },
        h("div", { className: "spinner", style: { borderTopColor: "var(--primary)", width: "36px", height: "36px" } }),
        h("p", { style: { marginTop: "14px", color: "var(--text-muted)", fontSize: "14px" } }, "Memeriksa status login...")
      );
    }

    // ATURAN 1: Tampilkan data HANYA jika sudah login!
    if (!session) {
      return h(
        "div",
        { className: "mobile-wrapper" },
        h(AuthScreen, { onLoginSuccess: (newSession) => setSession(newSession) })
      );
    }

    // JIKA SUDAH LOGIN: Tampilkan Dashboard & Navigasi Utama
    return h(
      "div",
      { className: "mobile-wrapper" },
      // Header Aplikasi
      h(
        "header",
        { className: "app-header" },
        h(
          "div",
          { className: "header-top" },
          h(
            "div",
            { className: "header-brand" },
            h("div", { className: "brand-icon" }, "📖"),
            h(
              "div",
              null,
              h("div", { className: "brand-title" }, "Keuangan TPQ"),
              h(
                "div",
                { className: "brand-subtitle" },
                session.user && session.user.email ? session.user.email : "Pengurus TPQ"
              )
            )
          ),
          h(
            "div",
            { className: "header-actions" },
            h(
              "button",
              {
                className: "btn-logout",
                onClick: handleLogout,
                title: "Keluar dari Akun",
              },
              "🚪 Keluar"
            )
          )
        )
      ),

      // Area Konten Berdasarkan Tab
      h(
        "main",
        { className: "main-content" },
        h(Alert, { type: "error", message: globalError, onClose: () => setGlobalError("") }),

        dataLoading &&
          h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "8px",
                padding: "8px",
                fontSize: "13px",
                color: "var(--text-muted)",
              },
            },
            h("span", { className: "spinner", style: { borderTopColor: "var(--primary)", width: "16px", height: "16px" } }),
            "Memperbarui data kas..."
          ),

        activeTab === "dashboard" &&
          h(DashboardScreen, {
            transaksiList,
            santriList,
            bulanTerpilih,
            setBulanTerpilih,
            daftarBulan,
            onNavToTransaksi: () => setActiveTab("transaksi"),
            onNavToSantri: () => setActiveTab("santri"),
          }),

        activeTab === "transaksi" &&
          h(TransaksiScreen, {
            transaksiList,
            santriList,
            bulanTerpilih,
            setBulanTerpilih,
            daftarBulan,
            onRefreshData: fetchData,
          }),

        activeTab === "santri" &&
          h(SantriScreen, {
            santriList,
            onRefreshData: fetchData,
          })
      ),

      // Navigasi Bawah (Bottom Navigation)
      h(
        "nav",
        { className: "bottom-nav" },
        h(
          "button",
          {
            className: `nav-item ${activeTab === "dashboard" ? "active" : ""}`,
            onClick: () => setActiveTab("dashboard"),
          },
          h("div", { className: "nav-icon" }, "📊"),
          h("div", { className: "nav-label" }, "Ringkasan")
        ),
        h(
          "button",
          {
            className: `nav-item ${activeTab === "transaksi" ? "active" : ""}`,
            onClick: () => setActiveTab("transaksi"),
          },
          h("div", { className: "nav-icon" }, "💳"),
          h("div", { className: "nav-label" }, "Transaksi")
        ),
        h(
          "button",
          {
            className: `nav-item ${activeTab === "santri" ? "active" : ""}`,
            onClick: () => setActiveTab("santri"),
          },
          h("div", { className: "nav-icon" }, "👥"),
          h("div", { className: "nav-label" }, "Santri")
        )
      )
    );
  }

  // Render Aplikasi React ke Element Root
  const rootElement = document.getElementById("root");
  if (rootElement) {
    const root = ReactDOM.createRoot(rootElement);
    root.render(h(App));
  }
})();
