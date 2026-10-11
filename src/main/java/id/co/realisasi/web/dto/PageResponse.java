package id.co.realisasi.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Hasil list berhalaman. page dimulai dari 0; summary berisi total atas seluruh data yang cocok
 * dengan filter (bukan hanya halaman ini), hanya diisi untuk list yang punya KPI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(List<T> items, long total, int page, int size, int totalPages, Object summary) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<List<E>, List<T>> mapper, Object summary) {
        return new PageResponse<>(mapper.apply(page.getContent()), page.getTotalElements(), page.getNumber(),
                page.getSize(), page.getTotalPages(), summary);
    }
}
