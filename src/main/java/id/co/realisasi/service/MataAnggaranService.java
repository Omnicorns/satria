package id.co.realisasi.service;

import id.co.realisasi.domain.MataAnggaran;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.repository.MataAnggaranRepository;
import id.co.realisasi.web.dto.MataAnggaranDto;
import id.co.realisasi.web.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MataAnggaranService {

    private static final Map<String, String> SORT = Map.of(
            "noCoa", "m.noCoa", "nama", "m.nama", "tipe", "m.tipe", "kategori", "m.kategori", "aktif", "m.aktif");

    private final MataAnggaranRepository repository;

    @Transactional(readOnly = true)
    public List<MataAnggaranDto.Response> findAll(TipeAnggaran tipe) {
        List<MataAnggaran> list = tipe == null ? repository.findAllByOrderByNoCoa()
                : repository.findByTipeOrderByNoCoa(tipe);
        return list.stream().map(MataAnggaranService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MataAnggaranDto.Response> page(TipeAnggaran tipe, Boolean aktif, String q,
                                                       Integer page, Integer size, String sort) {
        Pageable pageable = Paging.of(page, size, sort, SORT, Sort.by("m.noCoa", "m.id"));
        return PageResponse.of(repository.page(tipe, aktif, Paging.like(q), pageable),
                list -> list.stream().map(MataAnggaranService::toResponse).toList(), null);
    }

    @Transactional(readOnly = true)
    public MataAnggaran get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Mata anggaran", id));
    }

    public MataAnggaranDto.Response create(MataAnggaranDto.Request req) {
        if (repository.existsByNoCoa(req.noCoa())) {
            throw new BusinessException("No COA " + req.noCoa() + " sudah terdaftar");
        }
        MataAnggaran m = new MataAnggaran();
        apply(m, req);
        return toResponse(repository.save(m));
    }

    public MataAnggaranDto.Response update(Long id, MataAnggaranDto.Request req) {
        MataAnggaran m = get(id);
        repository.findByNoCoa(req.noCoa())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("No COA " + req.noCoa() + " sudah terdaftar");
                });
        apply(m, req);
        return toResponse(m);
    }

    private static void apply(MataAnggaran m, MataAnggaranDto.Request req) {
        m.setNoCoa(req.noCoa());
        m.setNama(req.nama());
        m.setTipe(req.tipe());
        m.setKategori(req.kategori());
        m.setAktif(req.aktif() == null || req.aktif());
    }

    public static MataAnggaranDto.Response toResponse(MataAnggaran m) {
        return new MataAnggaranDto.Response(m.getId(), m.getNoCoa(), m.getNama(), m.getTipe(), m.getKategori(),
                m.isAktif());
    }
}
