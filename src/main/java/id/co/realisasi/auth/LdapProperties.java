package id.co.realisasi.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfigurasi Active Directory (prefix {@code ldap.*}).
 *
 * @param enabled          false = pakai autentikator dev (tanpa AD), hanya untuk dev/test
 * @param url              contoh ldap://10.0.7.12:389
 * @param domain           domain AD untuk bind sebagai user@domain (contoh sarinah.net)
 * @param baseDn           contoh DC=sarinah,DC=net
 * @param userSearchBase   opsional, batasi pencarian ke OU tertentu; kosong = baseDn
 * @param managerDn        service account (untuk pencarian user tanpa password user, mis. fitur kelola akses)
 * @param managerPassword  isi lewat env LDAP_BIND_PASSWORD, jangan di-commit
 * @param connectTimeoutMs batas waktu koneksi ke AD
 * @param readTimeoutMs    batas waktu menunggu jawaban AD
 */
@ConfigurationProperties(prefix = "ldap")
public record LdapProperties(boolean enabled, String url, String domain, String baseDn, String userSearchBase,
                             String managerDn, String managerPassword, Integer connectTimeoutMs,
                             Integer readTimeoutMs) {

    public String searchBase() {
        return userSearchBase != null && !userSearchBase.isBlank() ? userSearchBase : baseDn;
    }
}
