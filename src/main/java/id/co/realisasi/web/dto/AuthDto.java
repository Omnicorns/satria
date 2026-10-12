package id.co.realisasi.web.dto;

import id.co.realisasi.domain.SumberUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDto {

    private AuthDto() {
    }

    /** Username AD (sAMAccountName); "SARINAH\nama" dan "nama@sarinah.net" juga diterima. */
    public record LoginRequest(@NotBlank @Size(max = 100) String username,
                               @NotBlank @Size(max = 256) String password) {

        @Override
        public String toString() {
            return "LoginRequest[username=" + username + "]";
        }
    }

    public record LoginResponse(String token, String tokenType, LocalDateTime expiresAt, UserResponse user) {
    }

    /**
     * @param admin     boleh mengelola program kerja semua divisi ({@code app.auth.admin-users})
     * @param divisiId  divisi anggaran user, dari pemetaan departemen AD ke divisi (null bila belum dipetakan)
     */
    public record UserResponse(Long id, String username, String nama, String email, String departemen,
                               String jabatan, SumberUser sumber, boolean aktif, LocalDateTime lastLoginAt,
                               boolean admin, Long divisiId, String divisiNama, Long companyId,
                               String companyNama) {
    }
}
