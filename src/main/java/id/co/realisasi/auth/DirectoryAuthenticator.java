package id.co.realisasi.auth;

import id.co.realisasi.domain.SumberUser;

/** Memeriksa username + password ke direktori dan mengembalikan atribut user. */
public interface DirectoryAuthenticator {

    /**
     * @throws AuthException          username/password salah
     * @throws DirectoryUnavailableException server direktori tidak dapat dihubungi
     */
    DirectoryUser authenticate(String username, String password);

    SumberUser sumber();
}
