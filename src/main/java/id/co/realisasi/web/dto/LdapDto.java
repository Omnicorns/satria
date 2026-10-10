package id.co.realisasi.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class LdapDto {

    private LdapDto() {
    }

    /**
     * @param totalUserAd          user aktif yang dibaca dari AD
     * @param userBaru             user AD yang baru ditambahkan ke app_user
     * @param userDiperbarui       user yang sudah ada dan datanya diperbarui
     * @param totalDepartemen      departemen berbeda pada user AD
     * @param departemenBaru       departemen yang baru ditambahkan ke ldap_departemen
     * @param userTidakAdaDiAd     user sumber LDAP di app_user yang tidak ada lagi / disabled di AD
     */
    public record SyncResult(int totalUserAd, int userBaru, int userDiperbarui, int totalDepartemen,
                             int departemenBaru, int userTidakAdaDiAd, LocalDateTime waktu) {
    }

    public record UserRow(Long id, String username, String nama, String email, String jabatan, boolean aktif,
                          LocalDateTime lastLoginAt) {
    }

    /** id null = grup "(Tanpa departemen)". */
    public record DepartemenGroup(Long id, String nama, Long divisiId, String divisiNama, int jumlahUser,
                                  LocalDateTime lastSyncAt, List<UserRow> users) {
    }

    public record MapDivisiRequest(Long divisiId) {
    }
}
