package id.co.realisasi.web.dto;

import id.co.realisasi.domain.TipeAnggaran;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MataAnggaranDto {

    private MataAnggaranDto() {
    }

    public record Request(
            @NotBlank @Pattern(regexp = "\\d{4,30}", message = "No COA harus berupa angka") String noCoa,
            @NotBlank @Size(max = 200) String nama,
            @NotNull TipeAnggaran tipe,
            @Size(max = 100) String kategori,
            Boolean aktif) {
    }

    public record Response(Long id, String noCoa, String nama, TipeAnggaran tipe, String kategori, boolean aktif) {
    }
}
