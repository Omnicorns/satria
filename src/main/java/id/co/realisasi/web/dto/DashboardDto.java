package id.co.realisasi.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDto {

    private DashboardDto() {
    }

    // ---------- Monitoring RKA ----------

    public record RkaItem(Long programKerjaId, String noCoa, String nama, String divisiNama, BigDecimal planning, BigDecimal pagu,
                          BigDecimal realisasi, double capaianPersen) {
    }

    /** Distribusi capaian: jumlah mata anggaran per bucket 0%, 1-49%, 50-99%, 100%+. */
    public record DistribusiCapaian(long nol, long satuSampai49, long limaPuluhSampai99, long seratusPlus) {
    }

    public record MonitoringRka(BigDecimal totalPlanning, BigDecimal totalPagu, BigDecimal totalRealisasi,
                                List<RkaItem> perMataAnggaran, DistribusiCapaian distribusi,
                                List<RkaItem> progressTerendah, List<CarryOver> carryOver) {
    }

    /**
     * Pekerjaan multi year dari tahun sebelumnya yang membebani RKA tahun yang dimonitor;
     * programKerjaId = RKA tempat pekerjaan ditautkan.
     */
    public record CarryOver(Long pekerjaanId, Long programKerjaId, String nama, String pic, String noCoa,
                            String namaMataAnggaran, String divisiNama, Integer tahunMulai, BigDecimal anggaran,
                            BigDecimal realisasiSebelumnya, BigDecimal realisasiTahunIni, BigDecimal sisaKontrak,
                            Integer progress) {
    }

    // ---------- CAPEX/OPEX ----------

    public record StatusCount(long onTrack, long terlambat, long selesai) {
    }

    public record BebanPic(String pic, long onTrack, long terlambat, long selesai) {
    }

    public record ProgressItem(Long pekerjaanId, String nama, String pic, int progress, String status) {
    }

    public record CapexOpex(StatusCount statusPekerjaan, List<BebanPic> bebanKerjaPerPic,
                            List<ProgressItem> progressPekerjaan) {
    }

    // ---------- Group Overview ----------

    public record Kpi(BigDecimal rka, BigDecimal rkaCapex, BigDecimal rkaOpex, BigDecimal serapan,
                      double serapanPersen, BigDecimal belumTerserap, double belumTerserapPersen,
                      BigDecimal usulanRkap) {
    }

    /** Baris tabel serapan & usulan; dipakai untuk per entitas (induk null) dan per divisi (induk = perusahaan). */
    public record Entitas(Long id, String kode, String nama, String induk, BigDecimal rka, BigDecimal serapan,
                          Double persenOpex, Double persenCapex, double persenTotal, BigDecimal usulan,
                          Double usulanVsSerapan) {
    }

    public record Serapan(BigDecimal realisasi, BigDecimal anggaran, double persen) {
    }

    public record TopItem(String noCoa, String nama, String companyNama, BigDecimal nilai) {
    }

    public record GroupOverview(int tahun, int tahunUsulan, Kpi kpi, List<Entitas> perEntitas,
                                List<Entitas> perDivisi,
                                Map<String, Serapan> opexVsCapex, List<TopItem> topAkunOpexUsulan,
                                List<TopItem> topProgramOpexUsulan, List<TopItem> topCapexUsulan) {
    }
}
