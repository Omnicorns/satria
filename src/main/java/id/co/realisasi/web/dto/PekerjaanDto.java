package id.co.realisasi.web.dto;

import id.co.realisasi.domain.JenisPeriode;
import id.co.realisasi.domain.StatusPekerjaan;
import id.co.realisasi.domain.TipeAnggaran;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PekerjaanDto {

    private PekerjaanDto() {
    }

    public record Request(
            @NotNull Long programKerjaId,
            @NotBlank @Size(max = 200) String nama,
            @Size(max = 2000) String deskripsi,
            @NotBlank @Size(max = 100) String pic,
            @Min(2000) @Max(2100) Integer tahun,
            JenisPeriode jenisPeriode,
            @NotNull @DecimalMin(value = "0", message = "Anggaran tidak boleh negatif") BigDecimal anggaran,
            LocalDate tanggalMulai,
            LocalDate estimasiSelesai,
            @Size(max = 2000) String tindakLanjut) {
    }

    public record Response(
            Long id,
            Long programKerjaId,
            String noCoa,
            String namaMataAnggaran,
            Long companyId,
            String companyNama,
            Long divisiId,
            String divisiNama,
            String nama,
            String deskripsi,
            String pic,
            Integer tahun,
            TipeAnggaran tipe,
            JenisPeriode jenisPeriode,
            BigDecimal anggaran,
            BigDecimal realisasi,
            BigDecimal sisa,
            LocalDate tanggalMulai,
            LocalDate estimasiSelesai,
            Integer progress,
            StatusPekerjaan status,
            String tindakLanjut) {
    }

    public record ProgressRequest(
            LocalDate tanggal,
            @NotNull @Min(0) @Max(100) Integer persen,
            @Size(max = 2000) String catatan,
            @Size(max = 2000) String tindakLanjut,
            @Size(max = 100) String dilaporkanOleh) {
    }

    public record ProgressResponse(Long id, LocalDate tanggal, Integer persen, String catatan,
                                   String tindakLanjut, String dilaporkanOleh) {
    }

    public record MilestoneRequest(
            @NotBlank @Size(max = 200) String nama,
            LocalDate tanggalMulai,
            @NotNull LocalDate tanggalSelesai,
            id.co.realisasi.domain.StatusMilestone status,
            @Size(max = 1000) String catatan) {
    }

    public record MilestoneResponse(Long id, String nama, LocalDate tanggalMulai, LocalDate tanggalSelesai,
                                    id.co.realisasi.domain.StatusMilestone status, String catatan,
                                    boolean terlambat) {
    }
}
