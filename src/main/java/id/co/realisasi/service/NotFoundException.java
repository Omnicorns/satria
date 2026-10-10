package id.co.realisasi.service;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entitas, Object id) {
        super(entitas + " dengan id " + id + " tidak ditemukan");
    }
}
