package id.co.realisasi.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CompanyDto {

    private CompanyDto() {
    }

    public record Request(
            @NotBlank @Size(max = 30) String kode,
            @NotBlank @Size(max = 150) String nama,
            @Size(max = 150) String namaGrup,
            @Size(max = 500) String logoUrl) {
    }

    public record Response(Long id, String kode, String nama, String namaGrup, String logoUrl) {
    }
}
