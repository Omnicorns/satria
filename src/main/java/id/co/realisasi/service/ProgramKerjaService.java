package id.co.realisasi.service;

import id.co.realisasi.domain.Divisi;
import id.co.realisasi.domain.MataAnggaran;
import id.co.realisasi.domain.ProgramKerja;
import id.co.realisasi.domain.StatusProgram;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.repository.PekerjaanRepository;
import id.co.realisasi.repository.ProgramKerjaRepository;
import id.co.realisasi.repository.RealisasiRepository;
import id.co.realisasi.web.dto.PageResponse;
import id.co.realisasi.web.dto.ProgramKerjaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static id.co.realisasi.service.Money.nz;
import static id.co.realisasi.service.Money.percent;

/** Monitoring RKA: program kerja per mata anggaran. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgramKerjaService {

    private static final Map<String, String> SORT = Map.of(
            "noCoa", "m.noCoa", "namaMataAnggaran", "m.nama", "divisiNama", "d.nama", "tahun", "p.tahun",
            "tipe", "p.tipe", "planningBudget", "p.planningBudget", "pagu", "p.pagu", "status", "p.status",
            "createdAt", "p.createdAt");

    private final ProgramKerjaRepository repository;
    private final PekerjaanRepository pekerjaanRepository;
    private final RealisasiRepository realisasiRepository;
    private final DivisiService divisiService;
    private final MataAnggaranService mataAnggaranService;

    @Transactional(readOnly = true)
    public List<ProgramKerja> search(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe) {
        return repository.search(companyId, divisiId, tahun, tipe);
    }

    @Transactional(readOnly = true)
    public List<ProgramKerjaDto.Response> list(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe) {
        List<ProgramKerja> list = search(companyId, divisiId, tahun, tipe);
        Map<Long, BigDecimal> realisasi = realisasiPerProgram(list);
        return list.stream().map(p -> toResponse(p, realisasi.get(p.getId()))).toList();
    }

    /** Daftar berhalaman; summary berisi total seluruh data yang cocok untuk kartu KPI. */
    @Transactional(readOnly = true)
    public PageResponse<ProgramKerjaDto.Response> page(Long companyId, Long divisiId, Integer tahun,
                                                       TipeAnggaran tipe, String q,
                                                       Integer page, Integer size, String sort) {
        String like = Paging.like(q);
        Pageable pageable = Paging.of(page, size, sort, SORT, Sort.by(Sort.Order.desc("p.tahun"),
                Sort.Order.desc("p.createdAt"), Sort.Order.desc("p.id")));
        Page<ProgramKerja> result = repository.page(companyId, divisiId, tahun, tipe, like, pageable);

        Object[] totals = repository.totals(companyId, divisiId, tahun, tipe, like).get(0);
        BigDecimal planning = nz((BigDecimal) totals[0]);
        BigDecimal plafon = nz((BigDecimal) totals[1]);
        BigDecimal realisasi = nz(realisasiRepository.sumForRka(companyId, divisiId, tahun, tipe, like));
        var summary = new ProgramKerjaDto.Summary(planning, plafon, realisasi, plafon.subtract(realisasi));

        return PageResponse.of(result, list -> {
            Map<Long, BigDecimal> real = realisasiPerProgram(list);
            return list.stream().map(p -> toResponse(p, real.get(p.getId()))).toList();
        }, summary);
    }

    @Transactional(readOnly = true)
    public ProgramKerja get(Long id) {
        return repository.findDetail(id).orElseThrow(() -> new NotFoundException("Program kerja", id));
    }

    @Transactional(readOnly = true)
    public ProgramKerjaDto.Response getResponse(Long id) {
        ProgramKerja p = get(id);
        return toResponse(p, realisasiPerProgram(List.of(p)).get(id));
    }

    public ProgramKerjaDto.Response create(ProgramKerjaDto.Request req) {
        ProgramKerja p = new ProgramKerja();
        apply(p, req);
        ensureUnique(p);
        return toResponse(repository.save(p), BigDecimal.ZERO);
    }

    public ProgramKerjaDto.Response update(Long id, ProgramKerjaDto.Request req) {
        ProgramKerja p = get(id);
        apply(p, req);
        ensureUnique(p);
        return toResponse(p, realisasiPerProgram(List.of(p)).get(id));
    }

    public void delete(Long id) {
        ProgramKerja p = get(id);
        if (pekerjaanRepository.existsByProgramKerjaId(id)) {
            throw new BusinessException("Program kerja masih memiliki pekerjaan, hapus pekerjaannya terlebih dahulu");
        }
        repository.delete(p);
    }

    /** Dipakai juga oleh import Excel: insert atau update berdasarkan kunci divisi+tahun+COA. */
    public boolean upsert(Divisi divisi, MataAnggaran mata, int tahun, BigDecimal planning,
                          BigDecimal pagu, String keterangan) {
        var existing = repository.findByDivisiIdAndTahunAndMataAnggaranId(divisi.getId(), tahun, mata.getId());
        ProgramKerja p = existing.orElseGet(ProgramKerja::new);
        p.setDivisi(divisi);
        p.setMataAnggaran(mata);
        p.setTahun(tahun);
        p.setTipe(mata.getTipe());
        p.setPlanningBudget(planning);
        p.setPagu(pagu);
        p.setKeterangan(keterangan);
        repository.save(p);
        return existing.isEmpty();
    }

    private void apply(ProgramKerja p, ProgramKerjaDto.Request req) {
        MataAnggaran mata = mataAnggaranService.get(req.mataAnggaranId());
        if (req.tipe() != null && req.tipe() != mata.getTipe()) {
            throw new BusinessException("Tipe " + req.tipe() + " tidak sesuai dengan mata anggaran "
                    + mata.getNoCoa() + " (" + mata.getTipe() + ")");
        }
        p.setDivisi(divisiService.get(req.divisiId()));
        p.setMataAnggaran(mata);
        p.setTahun(req.tahun());
        p.setTipe(mata.getTipe());
        p.setPlanningBudget(req.planningBudget());
        p.setPagu(req.pagu());
        p.setStatus(req.status() == null ? StatusProgram.OPEN : req.status());
        p.setKeterangan(req.keterangan());
    }

    private void ensureUnique(ProgramKerja p) {
        repository.findByDivisiIdAndTahunAndMataAnggaranId(p.getDivisi().getId(), p.getTahun(),
                        p.getMataAnggaran().getId())
                .filter(other -> !other.getId().equals(p.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Mata anggaran " + p.getMataAnggaran().getNoCoa()
                            + " sudah ada untuk divisi " + p.getDivisi().getNama() + " tahun " + p.getTahun());
                });
    }

    public Map<Long, BigDecimal> realisasiPerProgram(List<ProgramKerja> programs) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (programs.isEmpty()) {
            return result;
        }
        List<Long> ids = programs.stream().map(ProgramKerja::getId).toList();
        for (Object[] row : realisasiRepository.sumByProgramKerja(ids)) {
            result.put((Long) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    public static ProgramKerjaDto.Response toResponse(ProgramKerja p, BigDecimal realisasi) {
        BigDecimal real = nz(realisasi);
        BigDecimal plafon = p.plafon();
        return new ProgramKerjaDto.Response(
                p.getId(),
                p.getCompany().getId(),
                p.getCompany().getNama(),
                p.getDivisi().getId(),
                p.getDivisi().getKode(),
                p.getDivisi().getNama(),
                p.getTahun(),
                p.getTipe(),
                p.getMataAnggaran().getId(),
                p.getMataAnggaran().getNoCoa(),
                p.getMataAnggaran().getNama(),
                p.getPlanningBudget(),
                p.getPagu(),
                real,
                plafon.subtract(real),
                percent(real, plafon),
                p.getStatus(),
                p.getKeterangan(),
                p.getCreatedAt());
    }
}
