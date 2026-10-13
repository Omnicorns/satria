package id.co.realisasi.service;

import id.co.realisasi.domain.Company;
import id.co.realisasi.domain.Divisi;
import id.co.realisasi.domain.Pekerjaan;
import id.co.realisasi.domain.ProgramKerja;
import id.co.realisasi.domain.StatusPekerjaan;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.web.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static id.co.realisasi.service.Money.nz;
import static id.co.realisasi.service.Money.percent;

/** Agregasi untuk grafik Monitoring RKA, CAPEX/OPEX dan Group Overview. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    static final int MAKS_ITEM_PROGRESS = 10;
    static final int MAKS_TOP = 5;

    private final ProgramKerjaService programKerjaService;
    private final PekerjaanService pekerjaanService;
    private final Clock clock;

    // ---------- Monitoring RKA ----------

    public DashboardDto.MonitoringRka monitoringRka(Long companyId, Long divisiId, Integer tahun,
                                                    TipeAnggaran tipe) {
        List<ProgramKerja> programs = programKerjaService.search(companyId, divisiId, tahun, tipe);
        Map<Long, BigDecimal> realisasi = programKerjaService.realisasiPerProgram(programs);

        List<DashboardDto.RkaItem> items = programs.stream().map(p -> {
            BigDecimal real = nz(realisasi.get(p.getId()));
            return new DashboardDto.RkaItem(p.getId(), p.getMataAnggaran().getNoCoa(), p.getMataAnggaran().getNama(),
                    p.getDivisi().getNama(), p.getPlanningBudget(), p.getPagu(), real, percent(real, p.plafon()));
        }).toList();

        long nol = items.stream().filter(i -> i.capaianPersen() <= 0).count();
        long rendah = items.stream().filter(i -> i.capaianPersen() > 0 && i.capaianPersen() < 50).count();
        long sedang = items.stream().filter(i -> i.capaianPersen() >= 50 && i.capaianPersen() < 100).count();
        long penuh = items.stream().filter(i -> i.capaianPersen() >= 100).count();

        List<DashboardDto.RkaItem> terendah = items.stream()
                .sorted(Comparator.comparingDouble(DashboardDto.RkaItem::capaianPersen))
                .limit(MAKS_ITEM_PROGRESS)
                .toList();

        return new DashboardDto.MonitoringRka(
                sum(items, DashboardDto.RkaItem::planning),
                sum(items, i -> nz(i.pagu())),
                sum(items, DashboardDto.RkaItem::realisasi),
                items,
                new DashboardDto.DistribusiCapaian(nol, rendah, sedang, penuh),
                terendah);
    }

    // ---------- CAPEX/OPEX ----------

        /** Sama dengan filter daftar pekerjaan, supaya widget dan tabel menghitung pekerjaan yang sama. */
    public DashboardDto.CapexOpex capexOpex(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe,
                                            String pic, Long programKerjaId) {
        LocalDate today = LocalDate.now(clock);
        List<Pekerjaan> list = pekerjaanService.search(companyId, divisiId, tahun, tipe, pic, programKerjaId);

        Map<StatusPekerjaan, Long> perStatus = list.stream()
                .collect(Collectors.groupingBy(k -> k.status(today), Collectors.counting()));
        var statusCount = new DashboardDto.StatusCount(
                perStatus.getOrDefault(StatusPekerjaan.ON_TRACK, 0L),
                perStatus.getOrDefault(StatusPekerjaan.TERLAMBAT, 0L),
                perStatus.getOrDefault(StatusPekerjaan.SELESAI, 0L));

        Map<String, List<Pekerjaan>> perPic = list.stream()
                .collect(Collectors.groupingBy(Pekerjaan::getPic, TreeMap::new, Collectors.toList()));
        List<DashboardDto.BebanPic> beban = perPic.entrySet().stream().map(e -> new DashboardDto.BebanPic(
                e.getKey(),
                count(e.getValue(), k -> k.status(today) == StatusPekerjaan.ON_TRACK),
                count(e.getValue(), k -> k.status(today) == StatusPekerjaan.TERLAMBAT),
                count(e.getValue(), k -> k.status(today) == StatusPekerjaan.SELESAI))).toList();

        // Terlambat didahulukan, lalu progress terendah.
        List<DashboardDto.ProgressItem> progress = list.stream()
                .sorted(Comparator.comparing((Pekerjaan k) -> k.status(today) != StatusPekerjaan.TERLAMBAT)
                        .thenComparing(Pekerjaan::getProgress))
                .limit(MAKS_ITEM_PROGRESS)
                .map(k -> new DashboardDto.ProgressItem(k.getId(), k.getNama(), k.getPic(), k.getProgress(),
                        k.status(today).name()))
                .toList();

        return new DashboardDto.CapexOpex(statusCount, beban, progress);
    }

    // ---------- Group Overview ----------

    /**
     * Serapan tahun berjalan vs usulan RKAP tahun berikutnya.
     * RKA = pagu (atau planning bila pagu kosong) tahun {@code tahun}; serapan = total realisasi atas program
     * tahun tersebut; usulan = planning budget tahun {@code tahun + 1}.
     */
    public DashboardDto.GroupOverview groupOverview(Long companyId, Long divisiId, Integer tahun) {
        int th = tahun != null ? tahun : LocalDate.now(clock).getYear();
        int thUsulan = th + 1;

        List<ProgramKerja> rka = programKerjaService.search(companyId, divisiId, th, null);
        List<ProgramKerja> usulan = programKerjaService.search(companyId, divisiId, thUsulan, null);
        Map<Long, BigDecimal> realisasi = programKerjaService.realisasiPerProgram(rka);

        Predicate<ProgramKerja> opex = p -> p.getTipe() == TipeAnggaran.OPEX;
        Predicate<ProgramKerja> capex = p -> p.getTipe() == TipeAnggaran.CAPEX;

        BigDecimal totalRka = plafon(rka, x -> true);
        BigDecimal rkaOpex = plafon(rka, opex);
        BigDecimal rkaCapex = plafon(rka, capex);
        BigDecimal serapan = serapan(rka, realisasi, x -> true);
        BigDecimal serapanOpex = serapan(rka, realisasi, opex);
        BigDecimal serapanCapex = serapan(rka, realisasi, capex);
        BigDecimal belum = totalRka.subtract(serapan);
        BigDecimal totalUsulan = planning(usulan, x -> true);

        var kpi = new DashboardDto.Kpi(totalRka, rkaCapex, rkaOpex, serapan, percent(serapan, totalRka), belum,
                percent(belum, totalRka), totalUsulan);

        // Per entitas dan per divisi (divisi = level pengisian anggaran)
        Map<Long, Company> companies = new LinkedHashMap<>();
        Map<Long, Divisi> divisis = new LinkedHashMap<>();
        for (List<ProgramKerja> l : List.of(rka, usulan)) {
            l.forEach(p -> {
                companies.putIfAbsent(p.getCompany().getId(), p.getCompany());
                divisis.putIfAbsent(p.getDivisi().getId(), p.getDivisi());
            });
        }
        List<DashboardDto.Entitas> perEntitas = new ArrayList<>();
        for (Company c : companies.values()) {
            perEntitas.add(baris(c.getId(), c.getKode(), c.getNama(), null,
                    p -> p.getCompany().getId().equals(c.getId()), rka, usulan, realisasi));
        }
        List<DashboardDto.Entitas> perDivisi = new ArrayList<>();
        for (Divisi d : divisis.values()) {
            perDivisi.add(baris(d.getId(), d.getKode(), d.getNama(), d.getCompany().getNama(),
                    p -> p.getDivisi().getId().equals(d.getId()), rka, usulan, realisasi));
        }

        Map<String, DashboardDto.Serapan> opexVsCapex = new LinkedHashMap<>();
        opexVsCapex.put("OPEX", new DashboardDto.Serapan(serapanOpex, rkaOpex, percent(serapanOpex, rkaOpex)));
        opexVsCapex.put("CAPEX", new DashboardDto.Serapan(serapanCapex, rkaCapex, percent(serapanCapex, rkaCapex)));
        opexVsCapex.put("TOTAL", new DashboardDto.Serapan(serapan, totalRka, percent(serapan, totalRka)));

        // Top akun OPEX usulan: dijumlahkan per mata anggaran lintas entitas/unit.
        Map<String, DashboardDto.TopItem> perAkun = new LinkedHashMap<>();
        usulan.stream().filter(opex).forEach(p -> perAkun.merge(p.getMataAnggaran().getNoCoa(),
                new DashboardDto.TopItem(p.getMataAnggaran().getNoCoa(), p.getMataAnggaran().getNama(), null,
                        p.getPlanningBudget()),
                (a, b) -> new DashboardDto.TopItem(a.noCoa(), a.nama(), null, a.nilai().add(b.nilai()))));
        List<DashboardDto.TopItem> topAkun = perAkun.values().stream()
                .sorted(Comparator.comparing(DashboardDto.TopItem::nilai).reversed())
                .toList();

        return new DashboardDto.GroupOverview(th, thUsulan, kpi, perEntitas, perDivisi, opexVsCapex, topAkun,
                topProgram(usulan, opex), topProgram(usulan, capex));
    }

    private static DashboardDto.Entitas baris(Long id, String kode, String nama, String induk,
                                              Predicate<ProgramKerja> milik, List<ProgramKerja> rka,
                                              List<ProgramKerja> usulan, Map<Long, BigDecimal> realisasi) {
        Predicate<ProgramKerja> opex = p -> p.getTipe() == TipeAnggaran.OPEX;
        Predicate<ProgramKerja> capex = p -> p.getTipe() == TipeAnggaran.CAPEX;
        BigDecimal cRka = plafon(rka, milik);
        BigDecimal cRkaOpex = plafon(rka, milik.and(opex));
        BigDecimal cRkaCapex = plafon(rka, milik.and(capex));
        BigDecimal cSerapan = serapan(rka, realisasi, milik);
        BigDecimal cSerapanOpex = serapan(rka, realisasi, milik.and(opex));
        BigDecimal cSerapanCapex = serapan(rka, realisasi, milik.and(capex));
        BigDecimal cUsulan = planning(usulan, milik);
        return new DashboardDto.Entitas(id, kode, nama, induk, cRka, cSerapan,
                cRkaOpex.signum() == 0 ? null : percent(cSerapanOpex, cRkaOpex),
                cRkaCapex.signum() == 0 ? null : percent(cSerapanCapex, cRkaCapex),
                percent(cSerapan, cRka),
                cUsulan,
                cSerapan.signum() == 0 ? null : ratio(cUsulan, cSerapan));
    }

    private static List<DashboardDto.TopItem> topProgram(List<ProgramKerja> list, Predicate<ProgramKerja> filter) {
        return list.stream().filter(filter)
                .sorted(Comparator.comparing(ProgramKerja::getPlanningBudget).reversed())
                .limit(MAKS_TOP)
                .map(p -> new DashboardDto.TopItem(p.getMataAnggaran().getNoCoa(), p.getMataAnggaran().getNama(),
                        p.getCompany().getNama() + " · " + p.getDivisi().getNama(), p.getPlanningBudget()))
                .toList();
    }

    private static BigDecimal plafon(List<ProgramKerja> list, Predicate<ProgramKerja> filter) {
        return list.stream().filter(filter).map(ProgramKerja::plafon).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal planning(List<ProgramKerja> list, Predicate<ProgramKerja> filter) {
        return list.stream().filter(filter).map(ProgramKerja::getPlanningBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal serapan(List<ProgramKerja> list, Map<Long, BigDecimal> realisasi,
                                      Predicate<ProgramKerja> filter) {
        return list.stream().filter(filter).map(p -> nz(realisasi.get(p.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double ratio(BigDecimal a, BigDecimal b) {
        return percent(a, b) / 100d;
    }

    private static <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> f) {
        return list.stream().map(f).map(Money::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static long count(List<Pekerjaan> list, Predicate<Pekerjaan> filter) {
        return list.stream().filter(filter).count();
    }
}
