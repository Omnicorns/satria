package id.co.realisasi.config;

import id.co.realisasi.domain.Company;
import id.co.realisasi.domain.Divisi;
import id.co.realisasi.domain.JenisPeriode;
import id.co.realisasi.domain.MataAnggaran;
import id.co.realisasi.domain.Pekerjaan;
import id.co.realisasi.domain.ProgramKerja;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.repository.CompanyRepository;
import id.co.realisasi.repository.DivisiRepository;
import id.co.realisasi.repository.MataAnggaranRepository;
import id.co.realisasi.repository.PekerjaanRepository;
import id.co.realisasi.repository.ProgramKerjaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** Data contoh (profil dev): InJ Retail dengan divisi Technology Group. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final DivisiRepository divisiRepository;
    private final MataAnggaranRepository mataAnggaranRepository;
    private final ProgramKerjaRepository programKerjaRepository;
    private final PekerjaanRepository pekerjaanRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            return;
        }
//        Company inj = new Company();
//        inj.setKode("INJ-RETAIL");
//        inj.setNama("InJ Retail");
//        inj.setNamaGrup("InJourney Group");
//        companyRepository.save(inj);
//
//        Divisi tg = new Divisi();
//        tg.setCompany(inj);
//        tg.setKode("INJ-TG");
//        tg.setNama("Technology Group");
//        divisiRepository.save(tg);
//
//        Map<String, MataAnggaran> coa = new HashMap<>();
//        coa.put("6104050001", mata("6104050001", "Beban Sewa Mesin dan Peralatan", TipeAnggaran.OPEX));
//        coa.put("6104060001", mata("6104060001", "Beban Sewa Penggunaan Lisensi", TipeAnggaran.OPEX));
//        coa.put("6103030001", mata("6103030001", "Beban Internet, Telepon, Dan Telex", TipeAnggaran.OPEX));
//        coa.put("6107010001", mata("6107010001", "Beban Amortisasi Aset Tak Berwujud - Lisensi", TipeAnggaran.OPEX));
//        coa.put("6105020001", mata("6105020001", "Beban Konsultan Manajemen", TipeAnggaran.OPEX));
//        coa.put("1402010001", mata("1402010001", "Aset Tetap - Peralatan Teknologi Informasi", TipeAnggaran.CAPEX));
//
//        // RKA 2026 (berjalan)
//        ProgramKerja sewa26 = program(tg, coa.get("6104050001"), 2026, "3000000000");
//        ProgramKerja lisensi26 = program(tg, coa.get("6104060001"), 2026, "3500000000");
//        ProgramKerja internet26 = program(tg, coa.get("6103030001"), 2026, "2450000000");
//        program(tg, coa.get("6107010001"), 2026, "1500000000");
//        program(tg, coa.get("6105020001"), 2026, "1000000000");
//
//        // Usulan RKAP 2027
//        program(tg, coa.get("6104050001"), 2027, "1732464000");
//        program(tg, coa.get("6104060001"), 2027, "2305659231");
//        program(tg, coa.get("6103030001"), 2027, "2203922750");
//        program(tg, coa.get("6107010001"), 2027, "1000000000");
//        program(tg, coa.get("6105020001"), 2027, "900000000");
//        program(tg, coa.get("1402010001"), 2027, "750000000");
//
//        pekerjaan(sewa26, "Managed Service POS & EDC", "200000000");
//        pekerjaan(internet26, "Managed Service IT Helpdesk & IT Support", "407000000");
//        pekerjaan(sewa26, "Managed Service Printer", "109000000");
//        pekerjaan(lisensi26, "Lisensi Microsoft Office 365", "476000000");
//        pekerjaan(sewa26, "Biaya Sparepart UPS", "85000000");
//
//        log.info("Data contoh dev dimuat: {} program kerja, {} pekerjaan",
//                programKerjaRepository.count(), pekerjaanRepository.count());
    }

    private MataAnggaran mata(String noCoa, String nama, TipeAnggaran tipe) {
        MataAnggaran m = new MataAnggaran();
        m.setNoCoa(noCoa);
        m.setNama(nama);
        m.setTipe(tipe);
        return mataAnggaranRepository.save(m);
    }

    private ProgramKerja program(Divisi d, MataAnggaran m, int tahun, String planning) {
        ProgramKerja p = new ProgramKerja();
        p.setDivisi(d);
        p.setMataAnggaran(m);
        p.setTahun(tahun);
        p.setTipe(m.getTipe());
        p.setPlanningBudget(new BigDecimal(planning));
        return programKerjaRepository.save(p);
    }

    private void pekerjaan(ProgramKerja p, String nama, String anggaran) {
        Pekerjaan k = new Pekerjaan();
        k.setProgramKerja(p);
        k.setNama(nama);
        k.setPic("agung.pribadi");
        k.setTahun(p.getTahun());
        k.setTipe(p.getTipe());
        k.setJenisPeriode(JenisPeriode.SINGLE_YEAR);
        k.setAnggaran(new BigDecimal(anggaran));
        pekerjaanRepository.save(k);
    }
}
