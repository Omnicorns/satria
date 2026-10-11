package id.co.realisasi.web.dto;

import id.co.realisasi.domain.StatusProgram;
import id.co.realisasi.domain.TipeAnggaran;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProgramKerjaDto {

    private ProgramKerjaDto() {
    }

    /**
     * Form "Tambah program kerja — Monitoring RKA". Anggaran diisi per divisi; perusahaan mengikuti divisi.
     * No COA dan nama diisi otomatis dari mata anggaran.
     */
    public record Request(
            @NotNull Long divisiId,
            @NotNull @Min(2000) @Max(2100) Integer tahun,
            TipeAnggaran tipe,
            @NotNull Long mataAnggaranId,
            @NotNull @DecimalMin(value = "0", message = "Planning budget tidak boleh negatif") BigDecimal planningBudget,
            @DecimalMin(value = "0", message = "Pagu tidak boleh negatif") BigDecimal pagu,
            StatusProgram status,
            @Size(max = 1000) String keterangan) {
    }

    public record Response(
            Long id,
            Long companyId,
            String companyNama,
            Long divisiId,
            String divisiKode,
            String divisiNama,
            Integer tahun,
            TipeAnggaran tipe,
            Long mataAnggaranId,
            String noCoa,
            String namaMataAnggaran,
            BigDecimal planningBudget,
            BigDecimal pagu,
            BigDecimal realisasi,
            BigDecimal sisa,
            double capaianPersen,
            StatusProgram status,
            String keterangan,
            LocalDateTime createdAt) {
    }

    /** Total untuk kartu KPI atas seluruh data yang cocok dengan filter; plafon = pagu atau planning. */
    public record Summary(BigDecimal totalPlanning, BigDecimal totalPagu, BigDecimal totalRealisasi,
                          BigDecimal totalSisa) {
    }
}
