package id.co.realisasi.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DivisiDto {

    private DivisiDto() {
    }

    public record Request(
            @NotNull Long companyId,
            @NotBlank @Size(max = 30) String kode,
            @NotBlank @Size(max = 150) String nama,
            Boolean aktif) {
    }

    public record Response(Long id, Long companyId, String companyNama, String kode, String nama, boolean aktif) {
    }
}
