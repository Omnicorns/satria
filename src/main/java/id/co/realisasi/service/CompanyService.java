package id.co.realisasi.service;

import id.co.realisasi.domain.Company;
import id.co.realisasi.repository.CompanyRepository;
import id.co.realisasi.web.dto.CompanyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository repository;

    @Transactional(readOnly = true)
    public List<CompanyDto.Response> findAll() {
        return repository.findAll().stream().map(CompanyService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Company get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Perusahaan", id));
    }

    public CompanyDto.Response create(CompanyDto.Request req) {
        if (repository.existsByKode(req.kode())) {
            throw new BusinessException("Kode perusahaan " + req.kode() + " sudah dipakai");
        }
        Company c = new Company();
        apply(c, req);
        return toResponse(repository.save(c));
    }

    public CompanyDto.Response update(Long id, CompanyDto.Request req) {
        Company c = get(id);
        repository.findByKode(req.kode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("Kode perusahaan " + req.kode() + " sudah dipakai");
                });
        apply(c, req);
        return toResponse(c);
    }

    private static void apply(Company c, CompanyDto.Request req) {
        c.setKode(req.kode());
        c.setNama(req.nama());
        c.setNamaGrup(req.namaGrup());
        c.setLogoUrl(req.logoUrl());
    }

    public static CompanyDto.Response toResponse(Company c) {
        return new CompanyDto.Response(c.getId(), c.getKode(), c.getNama(), c.getNamaGrup(), c.getLogoUrl());
    }
}
