package id.co.realisasi.service;

import id.co.realisasi.domain.MilestonePekerjaan;
import id.co.realisasi.domain.Pekerjaan;
import id.co.realisasi.domain.ProgramKerja;
import id.co.realisasi.domain.ProgressPekerjaan;
import id.co.realisasi.domain.Realisasi;
import id.co.realisasi.domain.StatusMilestone;
import id.co.realisasi.domain.StatusProgram;
import id.co.realisasi.domain.JenisPeriode;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.repository.MilestonePekerjaanRepository;
import id.co.realisasi.repository.PekerjaanRepository;
import id.co.realisasi.repository.ProgressPekerjaanRepository;
import id.co.realisasi.repository.RealisasiRepository;
import id.co.realisasi.web.dto.PageResponse;
import id.co.realisasi.web.dto.PekerjaanDto;
import id.co.realisasi.web.dto.RealisasiDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static id.co.realisasi.service.Money.nz;
import static id.co.realisasi.service.Money.percent;

/** Pekerjaan CAPEX/OPEX beserta progress, timeline dan financial (realisasi). */
@Service
@RequiredArgsConstructor
@Transactional
public class PekerjaanService {

    private static final Map<String, String> SORT = Map.of(
            "nama", "k.nama", "pic", "k.pic", "divisiNama", "d.nama", "tahun", "k.tahun", "anggaran", "k.anggaran",
            "progress", "k.progress", "tanggalMulai", "k.tanggalMulai", "estimasiSelesai", "k.estimasiSelesai",
            "createdAt", "k.createdAt");

    private final PekerjaanRepository repository;
    private final ProgressPekerjaanRepository progressRepository;
    private final MilestonePekerjaanRepository milestoneRepository;
    private final RealisasiRepository realisasiRepository;
    private final ProgramKerjaService programKerjaService;
    private final Clock clock;

    // ---------- Pekerjaan ----------

    @Transactional(readOnly = true)
    public List<Pekerjaan> search(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe, String pic,
                                  Long programKerjaId) {
        return repository.search(companyId, divisiId, tahun, tipe, pic, programKerjaId);
    }

    @Transactional(readOnly = true)
    public List<PekerjaanDto.Response> list(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe,
                                            String pic, Long programKerjaId) {
        List<Pekerjaan> list = search(companyId, divisiId, tahun, tipe, pic, programKerjaId);
        Map<Long, BigDecimal> realisasi = realisasiPerPekerjaan(list);
        LocalDate today = LocalDate.now(clock);
        return list.stream().map(k -> toResponse(k, realisasi.get(k.getId()), today)).toList();
    }

    /** Daftar berhalaman; summary menghitung status atas seluruh data yang cocok, bukan hanya halaman ini. */
    @Transactional(readOnly = true)
    public PageResponse<PekerjaanDto.Response> page(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe,
                                                    String pic, Long programKerjaId, String q,
                                                    Integer page, Integer size, String sort) {
        String like = Paging.like(q);
        LocalDate today = LocalDate.now(clock);
        Pageable pageable = Paging.of(page, size, sort, SORT,
                Sort.by(Sort.Order.desc("k.createdAt"), Sort.Order.desc("k.id")));
        Page<Pekerjaan> result = repository.page(companyId, divisiId, tahun, tipe, pic, programKerjaId, like,
                pageable);

        Object[] s = repository.summary(companyId, divisiId, tahun, tipe, pic, programKerjaId, like, today).get(0);
        long selesai = s[0] == null ? 0 : ((Number) s[0]).longValue();
        long terlambat = s[1] == null ? 0 : ((Number) s[1]).longValue();
        long total = ((Number) s[2]).longValue();
        var summary = new PekerjaanDto.Summary(total - selesai - terlambat, terlambat, selesai,
                nz((BigDecimal) s[3]));

        return PageResponse.of(result, list -> {
            Map<Long, BigDecimal> realisasi = realisasiPerPekerjaan(list);
            return list.stream().map(k -> toResponse(k, realisasi.get(k.getId()), today)).toList();
        }, summary);
    }

    /** Semua PIC yang pernah dipakai, untuk dropdown filter. */
    @Transactional(readOnly = true)
    public List<String> picList() {
        return repository.findDistinctPic();
    }

    @Transactional(readOnly = true)
    public Pekerjaan get(Long id) {
        return repository.findDetail(id).orElseThrow(() -> new NotFoundException("Pekerjaan", id));
    }

    @Transactional(readOnly = true)
    public PekerjaanDto.Response getResponse(Long id) {
        Pekerjaan k = get(id);
        return toResponse(k, realisasiRepository.sumForPekerjaan(id), LocalDate.now(clock));
    }

    public PekerjaanDto.Response create(PekerjaanDto.Request req) {
        Pekerjaan k = new Pekerjaan();
        apply(k, req);
        return toResponse(repository.save(k), BigDecimal.ZERO, LocalDate.now(clock));
    }

    public PekerjaanDto.Response update(Long id, PekerjaanDto.Request req) {
        Pekerjaan k = get(id);
        apply(k, req);
        return toResponse(k, realisasiRepository.sumForPekerjaan(id), LocalDate.now(clock));
    }

    public void delete(Long id) {
        Pekerjaan k = get(id);
        realisasiRepository.deleteByPekerjaanId(id);
        progressRepository.deleteByPekerjaanId(id);
        milestoneRepository.deleteByPekerjaanId(id);
        repository.delete(k);
    }

    private void apply(Pekerjaan k, PekerjaanDto.Request req) {
        ProgramKerja program = programKerjaService.get(req.programKerjaId());
        if (program.getStatus() == StatusProgram.CLOSED
                && (k.getProgramKerja() == null || !k.getProgramKerja().getId().equals(program.getId()))) {
            throw new BusinessException("Program kerja " + program.getMataAnggaran().getNama() + " sudah ditutup");
        }
        if (req.tanggalMulai() != null && req.estimasiSelesai() != null
                && req.estimasiSelesai().isBefore(req.tanggalMulai())) {
            throw new BusinessException("Estimasi selesai tidak boleh sebelum tanggal mulai");
        }
        int tahun = req.tahun() == null ? program.getTahun() : req.tahun();
        JenisPeriode periode = req.jenisPeriode() == null ? JenisPeriode.SINGLE_YEAR : req.jenisPeriode();
        if (periode == JenisPeriode.SINGLE_YEAR && tahun != program.getTahun()) {
            throw new BusinessException("Pekerjaan single year tahun " + tahun + " harus memakai RKA tahun " + tahun
                    + ", bukan RKA " + program.getTahun());
        }
        k.setProgramKerja(program);
        k.setNama(req.nama());
        k.setDeskripsi(req.deskripsi());
        k.setPic(req.pic());
        k.setTahun(tahun);
        k.setTipe(program.getTipe());
        k.setJenisPeriode(periode);
        k.setAnggaran(req.anggaran());
        k.setTanggalMulai(req.tanggalMulai());
        k.setEstimasiSelesai(req.estimasiSelesai());
        k.setTindakLanjut(req.tindakLanjut());
    }

    // ---------- Progress ----------

    @Transactional(readOnly = true)
    public List<PekerjaanDto.ProgressResponse> listProgress(Long pekerjaanId) {
        get(pekerjaanId);
        return progressRepository.findByPekerjaanIdOrderByTanggalDescIdDesc(pekerjaanId).stream()
                .map(PekerjaanService::toProgressResponse).toList();
    }

    /** Menambah laporan progress; progress pekerjaan mengikuti laporan dengan tanggal terbaru. */
    public PekerjaanDto.ProgressResponse addProgress(Long pekerjaanId, PekerjaanDto.ProgressRequest req) {
        Pekerjaan k = get(pekerjaanId);
        ProgressPekerjaan p = new ProgressPekerjaan();
        p.setPekerjaan(k);
        p.setTanggal(req.tanggal() == null ? LocalDate.now(clock) : req.tanggal());
        p.setPersen(req.persen());
        p.setCatatan(req.catatan());
        p.setTindakLanjut(req.tindakLanjut());
        p.setDilaporkanOleh(req.dilaporkanOleh());
        progressRepository.save(p);

        ProgressPekerjaan latest = progressRepository.findByPekerjaanIdOrderByTanggalDescIdDesc(pekerjaanId).get(0);
        k.setProgress(latest.getPersen());
        if (latest.getTindakLanjut() != null) {
            k.setTindakLanjut(latest.getTindakLanjut());
        }
        return toProgressResponse(p);
    }

    // ---------- Timeline ----------

    @Transactional(readOnly = true)
    public List<PekerjaanDto.MilestoneResponse> listMilestone(Long pekerjaanId) {
        get(pekerjaanId);
        LocalDate today = LocalDate.now(clock);
        return milestoneRepository.findByPekerjaanIdOrderByTanggalSelesaiAsc(pekerjaanId).stream()
                .map(m -> toMilestoneResponse(m, today)).toList();
    }

    public PekerjaanDto.MilestoneResponse addMilestone(Long pekerjaanId, PekerjaanDto.MilestoneRequest req) {
        MilestonePekerjaan m = new MilestonePekerjaan();
        m.setPekerjaan(get(pekerjaanId));
        applyMilestone(m, req);
        return toMilestoneResponse(milestoneRepository.save(m), LocalDate.now(clock));
    }

    public PekerjaanDto.MilestoneResponse updateMilestone(Long pekerjaanId, Long milestoneId,
                                                          PekerjaanDto.MilestoneRequest req) {
        MilestonePekerjaan m = milestone(pekerjaanId, milestoneId);
        applyMilestone(m, req);
        return toMilestoneResponse(m, LocalDate.now(clock));
    }

    public void deleteMilestone(Long pekerjaanId, Long milestoneId) {
        milestoneRepository.delete(milestone(pekerjaanId, milestoneId));
    }

    private MilestonePekerjaan milestone(Long pekerjaanId, Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .filter(m -> m.getPekerjaan().getId().equals(pekerjaanId))
                .orElseThrow(() -> new NotFoundException("Milestone", milestoneId));
    }

    private static void applyMilestone(MilestonePekerjaan m, PekerjaanDto.MilestoneRequest req) {
        if (req.tanggalMulai() != null && req.tanggalSelesai().isBefore(req.tanggalMulai())) {
            throw new BusinessException("Tanggal selesai milestone tidak boleh sebelum tanggal mulai");
        }
        m.setNama(req.nama());
        m.setTanggalMulai(req.tanggalMulai());
        m.setTanggalSelesai(req.tanggalSelesai());
        m.setStatus(req.status() == null ? StatusMilestone.BELUM_MULAI : req.status());
        m.setCatatan(req.catatan());
    }

    // ---------- Financial / Realisasi ----------

    @Transactional(readOnly = true)
    public RealisasiDto.Financial financial(Long pekerjaanId) {
        Pekerjaan k = get(pekerjaanId);
        List<RealisasiDto.Response> trx = realisasiRepository.findByPekerjaanIdOrderByTanggalAsc(pekerjaanId).stream()
                .map(PekerjaanService::toRealisasiResponse).toList();
        BigDecimal total = trx.stream().map(RealisasiDto.Response::nilai).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RealisasiDto.Financial(pekerjaanId, k.getAnggaran(), total, k.getAnggaran().subtract(total),
                percent(total, k.getAnggaran()), trx);
    }

    /**
     * Mencatat realisasi. Ditolak bila total realisasi program kerja akan melebihi plafon (pagu / planning).
     */
    public RealisasiDto.Response addRealisasi(Long pekerjaanId, RealisasiDto.Request req) {
        Pekerjaan k = get(pekerjaanId);
        ProgramKerja program = k.getProgramKerja();
        BigDecimal terpakai = nz(programKerjaService.realisasiPerProgram(List.of(program)).get(program.getId()));
        BigDecimal setelah = terpakai.add(req.nilai());
        if (setelah.compareTo(program.plafon()) > 0) {
            throw new BusinessException("Realisasi melebihi sisa anggaran mata anggaran "
                    + program.getMataAnggaran().getNoCoa() + ": sisa " + program.plafon().subtract(terpakai).toPlainString());
        }
        Realisasi r = new Realisasi();
        r.setPekerjaan(k);
        r.setTanggal(req.tanggal());
        r.setPeriodeTahun(req.tanggal().getYear());
        r.setPeriodeBulan(req.tanggal().getMonthValue());
        r.setNilai(req.nilai());
        r.setNoDokumen(req.noDokumen());
        r.setKeterangan(req.keterangan());
        return toRealisasiResponse(realisasiRepository.save(r));
    }

    public void deleteRealisasi(Long pekerjaanId, Long realisasiId) {
        Realisasi r = realisasiRepository.findById(realisasiId)
                .filter(x -> x.getPekerjaan().getId().equals(pekerjaanId))
                .orElseThrow(() -> new NotFoundException("Realisasi", realisasiId));
        realisasiRepository.delete(r);
    }

    public Map<Long, BigDecimal> realisasiPerPekerjaan(List<Pekerjaan> list) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (list.isEmpty()) {
            return result;
        }
        for (Object[] row : realisasiRepository.sumByPekerjaan(list.stream().map(Pekerjaan::getId).toList())) {
            result.put((Long) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    // ---------- Mapping ----------

    public static PekerjaanDto.Response toResponse(Pekerjaan k, BigDecimal realisasi, LocalDate today) {
        BigDecimal real = nz(realisasi);
        ProgramKerja p = k.getProgramKerja();
        return new PekerjaanDto.Response(
                k.getId(),
                p.getId(),
                p.getMataAnggaran().getNoCoa(),
                p.getMataAnggaran().getNama(),
                p.getCompany().getId(),
                p.getCompany().getNama(),
                p.getDivisi().getId(),
                p.getDivisi().getNama(),
                k.getNama(),
                k.getDeskripsi(),
                k.getPic(),
                k.getTahun(),
                k.getTipe(),
                k.getJenisPeriode(),
                k.getAnggaran(),
                real,
                k.getAnggaran().subtract(real),
                k.getTanggalMulai(),
                k.getEstimasiSelesai(),
                k.getProgress(),
                k.status(today),
                k.getTindakLanjut());
    }

    private static PekerjaanDto.ProgressResponse toProgressResponse(ProgressPekerjaan p) {
        return new PekerjaanDto.ProgressResponse(p.getId(), p.getTanggal(), p.getPersen(), p.getCatatan(),
                p.getTindakLanjut(), p.getDilaporkanOleh());
    }

    private static PekerjaanDto.MilestoneResponse toMilestoneResponse(MilestonePekerjaan m, LocalDate today) {
        boolean terlambat = m.getStatus() != StatusMilestone.SELESAI && m.getTanggalSelesai().isBefore(today);
        return new PekerjaanDto.MilestoneResponse(m.getId(), m.getNama(), m.getTanggalMulai(), m.getTanggalSelesai(),
                m.getStatus(), m.getCatatan(), terlambat);
    }

    private static RealisasiDto.Response toRealisasiResponse(Realisasi r) {
        return new RealisasiDto.Response(r.getId(), r.getTanggal(), r.getPeriodeTahun(), r.getPeriodeBulan(),
                r.getNilai(), r.getNoDokumen(), r.getKeterangan());
    }
}
