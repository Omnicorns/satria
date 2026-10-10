package id.co.realisasi.auth;

/** Login gagal atau token tidak valid, dipetakan ke HTTP 401. */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
