package id.co.realisasi.auth;

/** User sudah login tetapi tidak berhak atas data ini (misal program kerja divisi lain), dipetakan ke HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
