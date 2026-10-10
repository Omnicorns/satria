package id.co.realisasi.auth;

/** Server LDAP/AD tidak dapat dihubungi, dipetakan ke HTTP 503. */
public class DirectoryUnavailableException extends RuntimeException {

    public DirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
