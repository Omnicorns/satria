package id.co.realisasi.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LdapDirectoryAuthenticatorTest {

    private final LdapDirectoryAuthenticator ldap = new LdapDirectoryAuthenticator(new LdapProperties(true,
            "ldap://127.0.0.1:1", "sarinah.net", "DC=sarinah,DC=net", null, null, null, 500, 500));

    @Test
    void passwordKosongDitolakTanpaMenghubungiAd() {
        // AD menganggap bind tanpa password sebagai sukses (unauthenticated bind), jadi harus ditolak lebih dulu.
        assertThatThrownBy(() -> ldap.authenticate("budi", "")).isInstanceOf(AuthException.class);
    }

    @Test
    void serverTidakTerjangkauMenjadi503() {
        assertThatThrownBy(() -> ldap.authenticate("budi", "x".repeat(8)))
                .isInstanceOf(DirectoryUnavailableException.class);
    }

    @Test
    void filterLdapDiEscape() {
        assertThat(LdapDirectoryAuthenticator.escape("a*)(cn=*")).isEqualTo("a\\2a\\29\\28cn=\\2a");
    }

    @Test
    void usernameDinormalisasi() {
        assertThat(AuthService.normalize(" SARINAH\\Budi ")).isEqualTo("budi");
        assertThat(AuthService.normalize("Budi@sarinah.net")).isEqualTo("budi");
    }
}
