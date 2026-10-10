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

    public record UserResponse(Long id, String username, String nama, String email, String departemen,
                               String jabatan, SumberUser sumber, boolean aktif, LocalDateTime lastLoginAt) {
    }
}
