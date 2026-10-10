package id.co.realisasi.service;

import id.co.realisasi.domain.Divisi;
import id.co.realisasi.repository.DivisiRepository;
import id.co.realisasi.web.dto.DivisiDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DivisiService {

    private final DivisiRepository repository;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public List<DivisiDto.Response> findAll(Long companyId) {
        return repository.search(companyId).stream().map(DivisiService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Divisi get(Long id) {
        return repository.findDetail(id).orElseThrow(() -> new NotFoundException("Divisi", id));
    }

    public DivisiDto.Response create(DivisiDto.Request req) {
        if (repository.existsByKode(req.kode())) {
            throw new BusinessException("Kode divisi " + req.kode() + " sudah dipakai");
        }
        Divisi d = new Divisi();
        apply(d, req);
        return toResponse(repository.save(d));
    }

    public DivisiDto.Response update(Long id, DivisiDto.Request req) {
        Divisi d = get(id);
        repository.findByKode(req.kode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("Kode divisi " + req.kode() + " sudah dipakai");
                });
        apply(d, req);
        return toResponse(d);
    }

    private void apply(Divisi d, DivisiDto.Request req) {
        d.setCompany(companyService.get(req.companyId()));
        d.setKode(req.kode());
        d.setNama(req.nama());
        d.setAktif(req.aktif() == null || req.aktif());
    }

    public static DivisiDto.Response toResponse(Divisi d) {
        return new DivisiDto.Response(d.getId(), d.getCompany().getId(), d.getCompany().getNama(), d.getKode(),
                d.getNama(), d.isAktif());
    }
}
