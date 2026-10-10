package id.co.realisasi.service;

/** Pelanggaran aturan bisnis (duplikat, data masih dipakai, dsb.), dipetakan ke HTTP 409. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
