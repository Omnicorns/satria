package id.co.realisasi.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/** Membentuk Pageable dari parameter page/size/sort dengan whitelist kolom sort. */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    private Paging() {
    }

    /**
     * @param sort    "kolom" atau "kolom,desc"; kolom yang tidak dikenal diabaikan (pakai urutan default)
     * @param columns kunci sort yang boleh dipakai klien -> path JPQL (alias query, misal "d.nama")
     */
    public static Pageable of(Integer page, Integer size, String sort, Map<String, String> columns, Sort fallback) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s, sort(sort, columns, fallback));
    }

    /** Sort yang sama dengan {@link #of} tanpa pembagian halaman (dipakai export). */
    public static Sort sort(String sort, Map<String, String> columns, Sort fallback) {
        if (sort == null || sort.isBlank()) {
            return fallback;
        }
        String[] parts = sort.split(",");
        String path = columns.get(parts[0].trim());
        if (path == null) {
            return fallback;
        }
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        // Kolom yang sama nilainya tetap berurutan stabil antar halaman.
        return Sort.by(dir, path).and(fallback);
    }

    /** Kata kunci pencarian untuk "lower(kolom) like :q"; null jika kosong. */
    public static String like(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
    }
}
