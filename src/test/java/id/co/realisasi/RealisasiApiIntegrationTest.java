package id.co.realisasi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RealisasiApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    long companyId;
    long divisiId;
    long coaOpexId;
    long coaCapexId;

    @BeforeEach
    void masterData() throws Exception {
        companyId = id(postJson("/api/companies",
                Map.of("kode", "INJ-RETAIL", "nama", "InJ Retail", "namaGrup", "InJourney Group")));
        divisiId = id(postJson("/api/divisi",
                Map.of("companyId", companyId, "kode", "INJ-TG", "nama", "Technology Group")));
        coaOpexId = id(postJson("/api/mata-anggaran",
                Map.of("noCoa", "6104050001", "nama", "Beban Sewa Mesin dan Peralatan", "tipe", "OPEX")));
        coaCapexId = id(postJson("/api/mata-anggaran",
                Map.of("noCoa", "1402010001", "nama", "Aset Tetap - Peralatan TI", "tipe", "CAPEX")));
    }

    @Test
    void alurRkaPekerjaanProgressDanRealisasi() throws Exception {
        long programId = id(postJson("/api/rka", Map.of(
                "divisiId", divisiId, "tahun", 2026, "mataAnggaranId", coaOpexId,
                "planningBudget", 1_000_000_000)));

        long pekerjaanId = id(postJson("/api/pekerjaan", Map.of(
                "programKerjaId", programId, "nama", "Managed Service POS & EDC", "pic", "agung.pribadi",
                "anggaran", 200_000_000, "estimasiSelesai", "2026-09-01")));

        // Lewat estimasi dan progress 0 -> terlambat; tipe diwarisi dari mata anggaran.
        mvc.perform(get("/api/pekerjaan/{id}", pekerjaanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipe").value("OPEX"))
                .andExpect(jsonPath("$.tahun").value(2026))
                .andExpect(jsonPath("$.status").value("TERLAMBAT"));

        postJson("/api/pekerjaan/" + pekerjaanId + "/progress",
                Map.of("tanggal", "2026-09-20", "persen", 100, "tindakLanjut", "BAST ditandatangani"));
        mvc.perform(get("/api/pekerjaan/{id}", pekerjaanId))
                .andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.status").value("SELESAI"))
                .andExpect(jsonPath("$.tindakLanjut").value("BAST ditandatangani"));

        postJson("/api/pekerjaan/" + pekerjaanId + "/realisasi",
                Map.of("tanggal", "2026-08-31", "nilai", 150_000_000, "noDokumen", "INV-001"));
        postJson("/api/pekerjaan/" + pekerjaanId + "/timeline",
                Map.of("nama", "Pengadaan", "tanggalSelesai", "2026-06-30"));

        mvc.perform(get("/api/pekerjaan/{id}/financial", pekerjaanId))
                .andExpect(jsonPath("$.totalRealisasi").value(150_000_000))
                .andExpect(jsonPath("$.sisa").value(50_000_000))
                .andExpect(jsonPath("$.serapanPersen").value(75.0))
                .andExpect(jsonPath("$.transaksi[0].periodeBulan").value(8));
        mvc.perform(get("/api/pekerjaan/{id}/timeline", pekerjaanId))
                .andExpect(jsonPath("$[0].terlambat").value(true));

        mvc.perform(get("/api/rka").param("companyId", String.valueOf(companyId)).param("tahun", "2026"))
                .andExpect(jsonPath("$[0].noCoa").value("6104050001"))
                .andExpect(jsonPath("$[0].realisasi").value(150_000_000))
                .andExpect(jsonPath("$[0].sisa").value(850_000_000))
                .andExpect(jsonPath("$[0].capaianPersen").value(15.0));

        // Program masih dipakai pekerjaan -> tidak boleh dihapus.
        mvc.perform(delete("/api/rka/{id}", programId)).andExpect(status().isConflict());
    }

    @Test
    void realisasiTidakBolehMelebihiPlafon() throws Exception {
        long programId = id(postJson("/api/rka", Map.of(
                "divisiId", divisiId, "tahun", 2026, "mataAnggaranId", coaOpexId,
                "planningBudget", 500, "pagu", 100)));
        long pekerjaanId = id(postJson("/api/pekerjaan", Map.of(
                "programKerjaId", programId, "nama", "Sewa UPS", "pic", "budi", "anggaran", 100)));

        postJson("/api/pekerjaan/" + pekerjaanId + "/realisasi", Map.of("tanggal", "2026-03-01", "nilai", 80));
        send("/api/pekerjaan/" + pekerjaanId + "/realisasi", Map.of("tanggal", "2026-04-01", "nilai", 30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sisa 20")));
    }

    @Test
    void pekerjaanSingleYearHarusMemakaiRkaTahunYangSama() throws Exception {
        long rka2027 = id(postJson("/api/rka", Map.of(
                "divisiId", divisiId, "tahun", 2027, "mataAnggaranId", coaCapexId, "planningBudget", 650)));

        send("/api/pekerjaan", Map.of("programKerjaId", rka2027, "nama", "E-Commerce Fase 1", "pic", "iful",
                "anggaran", 250, "tahun", 2026, "jenisPeriode", "SINGLE_YEAR"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RKA tahun 2026")));

        // Multi year boleh lanjut ke RKA tahun berikutnya.
        postJson("/api/pekerjaan", Map.of("programKerjaId", rka2027, "nama", "E-Commerce Lanjutan", "pic", "iful",
                "anggaran", 250, "tahun", 2026, "jenisPeriode", "MULTI_YEAR"));
    }

    @Test
    void validasiDanDuplikat() throws Exception {
        send("/api/rka", Map.of("divisiId", divisiId, "tahun", 1999, "mataAnggaranId", coaOpexId,
                "planningBudget", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));

        Map<String, Object> rka = Map.of("divisiId", divisiId, "tahun", 2026, "mataAnggaranId", coaOpexId,
                "planningBudget", 10);
        postJson("/api/rka", rka);
        send("/api/rka", rka).andExpect(status().isConflict());

        // Tipe tidak cocok dengan COA.
        send("/api/rka", Map.of("divisiId", divisiId, "tahun", 2027, "tipe", "CAPEX", "mataAnggaranId", coaOpexId,
                "planningBudget", 10))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/pekerjaan/{id}", 999)).andExpect(status().isNotFound());
    }

    @Test
    void dashboardGroupOverviewDanCapexOpex() throws Exception {
        long opex26 = id(postJson("/api/rka", Map.of("divisiId", divisiId, "tahun", 2026,
                "mataAnggaranId", coaOpexId, "planningBudget", 800)));
        long capex26 = id(postJson("/api/rka", Map.of("divisiId", divisiId, "tahun", 2026,
                "mataAnggaranId", coaCapexId, "planningBudget", 200)));
        postJson("/api/rka", Map.of("divisiId", divisiId, "tahun", 2027,
                "mataAnggaranId", coaOpexId, "planningBudget", 600));
        postJson("/api/rka", Map.of("divisiId", divisiId, "tahun", 2027,
                "mataAnggaranId", coaCapexId, "planningBudget", 300));

        long k1 = id(postJson("/api/pekerjaan", Map.of("programKerjaId", opex26, "nama", "A", "pic", "agung",
                "anggaran", 400, "estimasiSelesai", "2026-01-31")));
        id(postJson("/api/pekerjaan", Map.of("programKerjaId", opex26, "nama", "B", "pic", "agung",
                "anggaran", 100)));
        id(postJson("/api/pekerjaan", Map.of("programKerjaId", capex26, "nama", "C", "pic", "budi",
                "anggaran", 50)));
        postJson("/api/pekerjaan/" + k1 + "/realisasi", Map.of("tanggal", "2026-05-01", "nilai", 400));

        mvc.perform(get("/api/dashboard/group-overview").param("tahun", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tahunUsulan").value(2027))
                .andExpect(jsonPath("$.kpi.rka").value(1000))
                .andExpect(jsonPath("$.kpi.rkaOpex").value(800))
                .andExpect(jsonPath("$.kpi.serapan").value(400))
                .andExpect(jsonPath("$.kpi.serapanPersen").value(40.0))
                .andExpect(jsonPath("$.kpi.belumTerserap").value(600))
                .andExpect(jsonPath("$.kpi.usulanRkap").value(900))
                .andExpect(jsonPath("$.perEntitas[0].nama").value("InJ Retail"))
                .andExpect(jsonPath("$.perDivisi[0].nama").value("Technology Group"))
                .andExpect(jsonPath("$.perDivisi[0].induk").value("InJ Retail"))
                .andExpect(jsonPath("$.perEntitas[0].persenOpex").value(50.0))
                .andExpect(jsonPath("$.perEntitas[0].persenCapex").value(0.0))
                .andExpect(jsonPath("$.perEntitas[0].usulanVsSerapan").value(2.25))
                .andExpect(jsonPath("$.opexVsCapex.OPEX.persen").value(50.0))
                .andExpect(jsonPath("$.topAkunOpexUsulan[0].nilai").value(600))
                .andExpect(jsonPath("$.topCapexUsulan[0].noCoa").value("1402010001"));

        mvc.perform(get("/api/dashboard/capex-opex").param("companyId", String.valueOf(companyId)))
                .andExpect(jsonPath("$.statusPekerjaan.terlambat").value(1))
                .andExpect(jsonPath("$.statusPekerjaan.onTrack").value(2))
                .andExpect(jsonPath("$.bebanKerjaPerPic[0].pic").value("agung"))
                .andExpect(jsonPath("$.progressPekerjaan[0].nama").value("A"));

        // Widget mengikuti filter program kerja dan PIC seperti daftar pekerjaan.
        mvc.perform(get("/api/dashboard/capex-opex").param("programKerjaId", String.valueOf(capex26)))
                .andExpect(jsonPath("$.statusPekerjaan.terlambat").value(0))
                .andExpect(jsonPath("$.statusPekerjaan.onTrack").value(1))
                .andExpect(jsonPath("$.bebanKerjaPerPic.length()").value(1))
                .andExpect(jsonPath("$.bebanKerjaPerPic[0].pic").value("budi"))
                .andExpect(jsonPath("$.progressPekerjaan.length()").value(1))
                .andExpect(jsonPath("$.progressPekerjaan[0].nama").value("C"));

        mvc.perform(get("/api/dashboard/capex-opex").param("pic", "agung"))
                .andExpect(jsonPath("$.bebanKerjaPerPic.length()").value(1))
                .andExpect(jsonPath("$.progressPekerjaan.length()").value(2));

        mvc.perform(get("/api/dashboard/monitoring-rka").param("tahun", "2026"))
                .andExpect(jsonPath("$.totalPlanning").value(1000))
                .andExpect(jsonPath("$.distribusi.nol").value(1))
                .andExpect(jsonPath("$.distribusi.limaPuluhSampai99").value(1))
                .andExpect(jsonPath("$.progressTerendah[0].noCoa").value("1402010001"));
    }

    @Test
    void excelTemplateImportExport() throws Exception {
        byte[] template = mvc.perform(get("/api/rka/excel/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("template-rka.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(template))) {
            assertThat(wb.getSheet("Daftar COA").getLastRowNum()).isEqualTo(2);
            assertThat(wb.getSheet("Daftar Divisi").getRow(1).getCell(0).getStringCellValue()).isEqualTo("INJ-TG");
        }

        // Baris tidak valid -> 422 dan tidak ada yang tersimpan.
        byte[] invalid = workbook(new Object[][]{
                {"INJ-TG", 2027, "6104050001", "", 1000, null, null},
                {"TIDAK-ADA", 2027, "9999", "", "abc", null, null}});
        mvc.perform(multipart("/api/rka/excel/import").file(file(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.dibuat").value(0))
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(get("/api/rka")).andExpect(jsonPath("$.length()").value(0));

        byte[] valid = workbook(new Object[][]{
                {"INJ-TG", 2027, "6104050001", "", 1732464000, null, "usulan"},
                {"INJ-TG", 2027, "1402010001", "", "750.000.000", 700000000, null}});
        mvc.perform(multipart("/api/rka/excel/import").file(file(valid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dibuat").value(2));
        // Import ulang = update, bukan duplikat.
        mvc.perform(multipart("/api/rka/excel/import").file(file(valid)))
                .andExpect(jsonPath("$.dibuat").value(0))
                .andExpect(jsonPath("$.diperbarui").value(2));

        mvc.perform(get("/api/rka").param("tahun", "2027").param("tipe", "CAPEX"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].planningBudget").value(750000000))
                .andExpect(jsonPath("$[0].pagu").value(700000000))
                .andExpect(jsonPath("$[0].divisiNama").value("Technology Group"))
                .andExpect(jsonPath("$[0].sisa").value(700000000));

        byte[] export = mvc.perform(get("/api/rka/excel/export").param("tahun", "2027"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(export))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getLastRowNum()).isEqualTo(2);
            assertThat(s.getRow(0).getCell(5).getStringCellValue()).isEqualTo("No COA");
            assertThat(s.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Technology Group");
        }
    }

    @Test
    void mataAnggaranExcelTemplateImportExport() throws Exception {
        byte[] template = mvc.perform(get("/api/mata-anggaran/excel/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("template-mata-anggaran.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(template))) {
            assertThat(wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("No COA");
            assertThat(wb.getSheet("Petunjuk")).isNotNull();
        }

        // Baris tidak valid atau No COA dobel -> 422 dan tidak ada yang tersimpan.
        byte[] invalid = workbook(new Object[][]{
                {"12ab", "", "XYZ", null, "mungkin"},
                {"6104050001", "Beban Sewa Baru", "OPEX"},
                {"6104050001", "Beban Sewa Lagi", "OPEX"}});
        mvc.perform(multipart("/api/mata-anggaran/excel/import").file(file(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.dibuat").value(0))
                .andExpect(jsonPath("$.errors.length()").value(5))
                .andExpect(jsonPath("$.errors[0]").value("Baris 2: Tipe harus OPEX atau CAPEX, bukan 'XYZ'"))
                .andExpect(jsonPath("$.errors[4]").value("Baris 4: No COA 6104050001 duplikat dengan baris 3"));
        mvc.perform(get("/api/mata-anggaran/{id}", coaOpexId))
                .andExpect(jsonPath("$.nama").value("Beban Sewa Mesin dan Peralatan"));

        // Upsert per No COA: yang ada diperbarui, yang baru dibuat; No COA numerik pun diterima.
        byte[] valid = workbook(new Object[][]{
                {"6104050001", "Beban Sewa Mesin", "opex", "Beban Sewa", "Nonaktif"},
                {5101010001L, "Beban Gaji", "OPEX", "Beban Pegawai", null}});
        mvc.perform(multipart("/api/mata-anggaran/excel/import").file(file(valid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dibuat").value(1))
                .andExpect(jsonPath("$.diperbarui").value(1));
        mvc.perform(get("/api/mata-anggaran/{id}", coaOpexId))
                .andExpect(jsonPath("$.nama").value("Beban Sewa Mesin"))
                .andExpect(jsonPath("$.kategori").value("Beban Sewa"))
                .andExpect(jsonPath("$.aktif").value(false));
        mvc.perform(get("/api/mata-anggaran")).andExpect(jsonPath("$.length()").value(3));

        // Export mengikuti filter tabel dan bisa diimpor ulang tanpa perubahan.
        byte[] export = mvc.perform(get("/api/mata-anggaran/excel/export").param("tipe", "OPEX"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(export))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getLastRowNum()).isEqualTo(2);
            assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("5101010001");
            assertThat(s.getRow(2).getCell(4).getStringCellValue()).isEqualTo("Nonaktif");
        }
        mvc.perform(multipart("/api/mata-anggaran/excel/import").file(file(export)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dibuat").value(0))
                .andExpect(jsonPath("$.diperbarui").value(2));
        mvc.perform(get("/api/mata-anggaran/excel/export").param("aktif", "false").param("q", "sewa"))
                .andExpect(status().isOk());
    }

    @Test
    void anggaranPerDivisiDijumlahkanKePerusahaan() throws Exception {
        long finance = id(postJson("/api/divisi",
                Map.of("companyId", companyId, "kode", "INJ-FIN", "nama", "Finance")));
        // COA yang sama boleh dipakai divisi berbeda pada tahun yang sama.
        postJson("/api/rka", Map.of("divisiId", divisiId, "tahun", 2026, "mataAnggaranId", coaOpexId,
                "planningBudget", 300));
        postJson("/api/rka", Map.of("divisiId", finance, "tahun", 2026, "mataAnggaranId", coaOpexId,
                "planningBudget", 200));

        mvc.perform(get("/api/rka").param("divisiId", String.valueOf(finance)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].divisiKode").value("INJ-FIN"))
                .andExpect(jsonPath("$[0].companyNama").value("InJ Retail"));

        mvc.perform(get("/api/dashboard/group-overview").param("tahun", "2026"))
                .andExpect(jsonPath("$.perEntitas.length()").value(1))
                .andExpect(jsonPath("$.perEntitas[0].rka").value(500))
                .andExpect(jsonPath("$.perDivisi.length()").value(2));

        mvc.perform(get("/api/dashboard/group-overview").param("tahun", "2026")
                        .param("divisiId", String.valueOf(finance)))
                .andExpect(jsonPath("$.kpi.rka").value(200));

        mvc.perform(get("/api/divisi").param("companyId", String.valueOf(companyId)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- helpers ----------

    private ResultActions send(String url, Object body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode postJson(String url, Object body) throws Exception {
        String res = send(url, body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private static long id(JsonNode node) {
        return node.get("id").asLong();
    }

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "rka.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    private static byte[] workbook(Object[][] rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("RKA");
            s.createRow(0).createCell(0).setCellValue("header");
            for (int i = 0; i < rows.length; i++) {
                Row r = s.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++) {
                    Object v = rows[i][c];
                    if (v instanceof Number n) {
                        r.createCell(c).setCellValue(n.doubleValue());
                    } else if (v != null) {
                        r.createCell(c).setCellValue(v.toString());
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
