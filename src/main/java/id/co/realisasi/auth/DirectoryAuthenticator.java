package id.co.realisasi.auth;

import id.co.realisasi.domain.SumberUser;

import java.util.List;

/** Memeriksa username + password ke direktori dan mengembalikan atribut user. */
public interface DirectoryAuthenticator {

    /**
     * @throws AuthException          username/password salah
     * @throws DirectoryUnavailableException server direktori tidak dapat dihubungi
     */
    DirectoryUser authenticate(String username, String password);

    /**
     * Semua user aktif di direktori (untuk sync departemen). Di AD memakai service account
     * ({@code ldap.manager-dn} + env LDAP_BIND_PASSWORD).
     *
     * @throws DirectoryUnavailableException server tidak dapat dihubungi / service account ditolak
     */
    List<DirectoryUser> listUsers();

    SumberUser sumber();
}
