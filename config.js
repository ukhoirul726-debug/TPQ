/**
 * ====================================================================
 * Konfigurasi Supabase - Keuangan TPQ
 * ====================================================================
 * File ini berisi URL dan Anon/Publishable Key untuk koneksi ke Supabase.
 * Dapat diubah sewaktu-waktu sesuai kredensial proyek Supabase Anda.
 */

const SUPABASE_URL = "https://rqxfjptocopgphmcqtel.supabase.co";
const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJxeGZqcHRvY29wZ3BobWNxdGVsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTExMTY5NjgsImV4cCI6MjEwNjY5Mjk2OH0.SAQI8NNK0K8kHPEUI3tvbSArbprr2c-m6FVDytg4zmk";

// Export untuk berbagai environment (Browser / ES Modules / Node.js)
if (typeof window !== "undefined") {
  window.SUPABASE_URL = SUPABASE_URL;
  window.SUPABASE_ANON_KEY = SUPABASE_ANON_KEY;
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = { SUPABASE_URL, SUPABASE_ANON_KEY };
}
