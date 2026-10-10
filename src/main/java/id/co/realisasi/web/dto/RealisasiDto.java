package id.co.realisasi.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RealisasiDto {

    private RealisasiDto() {
    }

    public record Request(
            @NotNull LocalDate tanggal,
            @NotNull @DecimalMin(value = "0.01", message = "Nilai realisasi harus lebih dari 0") BigDecimal nilai,
            @Size(max = 100) String noDokumen,
            @Size(max = 1000) String keterangan) {
    }

    public record Response(Long id, LocalDate tanggal, Integer periodeTahun, Integer periodeBulan,
                           BigDecimal nilai, String noDokumen, String keterangan) {
    }

    /** Ringkasan tab "Financial" sebuah pekerjaan. */
    public record Financial(Long pekerjaanId, BigDecimal anggaran, BigDecimal totalRealisasi, BigDecimal sisa,
                            double serapanPersen, List<Response> transaksi) {
    }
}
