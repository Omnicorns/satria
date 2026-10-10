package id.co.realisasi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * User aplikasi. Dibuat/diperbarui otomatis dari Active Directory setiap kali login LDAP berhasil
 * (nama, email, departemen, jabatan ikut data AD terbaru). {@code aktif=false} memblokir login walau password AD benar.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    /** sAMAccountName, huruf kecil (contoh: agung.pribadi). */
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(length = 200)
    private String nama;

    @Column(length = 200)
    private String email;

    @Column(length = 200)
    private String departemen;

    @Column(length = 200)
    private String jabatan;

    /** Departemen AD (null bila atribut department kosong). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ldap_departemen_id")
    private LdapDepartemen ldapDepartemen;

    @Column(name = "user_principal_name", length = 200)
    private String userPrincipalName;

    @Column(name = "distinguished_name", length = 500)
    private String distinguishedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SumberUser sumber;

    @Column(nullable = false)
    private boolean aktif = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** Terakhir kali data user diperbarui dari AD lewat sync (bukan login). */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
