package id.co.realisasi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** List dengan parameter page mengembalikan halaman + total; tanpa page tetap list penuh. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaginationApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    long companyId;

    @BeforeEach
    void masterData() throws Exception {
        companyId = id(postJson("/api/companies", Map.of("kode", "SRN", "nama", "PT Sarinah")));
        for (int i = 1; i <= 25; i++) {
            postJson("/api/divisi", Map.of("companyId", companyId, "kode", String.format("DEPT-%03d", i),
                    "nama", "Divisi " + String.format("%02d", i), "aktif", i % 5 != 0));
        }
    }

    @Test
    void divisiBerhalamanDenganFilterCariDanSort() throws Exception {
        mvc.perform(get("/api/divisi").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items[0].nama").value("Divisi 01"))
                .andExpect(jsonPath("$.summary").doesNotExist());

        mvc.perform(get("/api/divisi").param("page", "2").param("size", "10"))
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.page").value(2));

        mvc.perform(get("/api/divisi").param("page", "0").param("sort", "kode,desc"))
                .andExpect(jsonPath("$.items[0].kode").value("DEPT-025"));

        mvc.perform(get("/api/divisi").param("page", "0").param("aktif", "false"))
                .andExpect(jsonPath("$.total").value(5));

        mvc.perform(get("/api/divisi").param("page", "0").param("q", "dept-01"))
                .andExpect(jsonPath("$.total").value(10));

        // Kolom sort tidak dikenal diabaikan; size dibatasi maksimal 200.
        mvc.perform(get("/api/divisi").param("page", "0").param("size", "1000").param("sort", "hack;drop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(200));

        // Tanpa page: tetap array penuh untuk dropdown.
        mvc.perform(get("/api/divisi")).andExpect(jsonPath("$.length()").value(25));
    }

    @Test
    void mataAnggaranBerhalaman() throws Exception {
        for (int i = 1; i <= 12; i++) {
            postJson("/api/mata-anggaran", Map.of("noCoa", "61010100" + String.format("%02d", i),
                    "nama", "Beban " + i, "tipe", i <= 3 ? "CAPEX" : "OPEX", "kategori", "Umum"));
        }
        mvc.perform(get("/api/mata-anggaran").param("page", "1").param("size", "5"))
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[0].noCoa").value("6101010006"))
                .andExpect(jsonPath("$.total").value(12));
        mvc.perform(get("/api/mata-anggaran").param("page", "0").param("tipe", "CAPEX"))
                .andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/api/mata-anggaran").param("page", "0").param("q", "umum"))
                .andExpect(jsonPath("$.total").value(12));
        mvc.perform(get("/api/mata-anggaran")).andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void rkaDanPekerjaanBerhalamanDenganSummarySeluruhData() throws Exception {
        long divisiId = id(postJson("/api/divisi", Map.of("companyId", companyId, "kode", "TI", "nama", "Teknologi")));
        long[] programs = new long[3];
        for (int i = 0; i < 3; i++) {
            long coa = id(postJson("/api/mata-anggaran",
                    Map.of("noCoa", "610303000" + i, "nama", "Beban Internet " + i, "tipe", "OPEX")));
            Map<String, Object> body = new HashMap<>(Map.of("divisiId", divisiId, "tahun", 2026,
                    "mataAnggaranId", coa, "planningBudget", 100_000_000));
            if (i == 0) {
                body.put("pagu", 80_000_000);
            }
            programs[i] = id(postJson("/api/rka", body));
        }
        long selesai = id(postJson("/api/pekerjaan", Map.of("programKerjaId", programs[0], "nama", "Pasang alarm",
                "pic", "chrollo", "anggaran", 15_000_000)));
        postJson("/api/pekerjaan/" + selesai + "/progress", Map.of("persen", 100));
        postJson("/api/pekerjaan/" + selesai + "/realisasi",
                Map.of("tanggal", "2026-09-01", "nilai", 10_000_000));
        postJson("/api/pekerjaan", Map.of("programKerjaId", programs[1], "nama", "Lisensi IDE", "pic", "killua",
                "anggaran", 20_000_000, "estimasiSelesai", "2026-09-01"));
        postJson("/api/pekerjaan", Map.of("programKerjaId", programs[2], "nama", "Install DB", "pic", "killua",
                "anggaran", 30_000_000, "estimasiSelesai", "2026-12-31"));

        mvc.perform(get("/api/rka").param("page", "0").param("size", "2").param("tahun", "2026"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.summary.totalPlanning").value(300_000_000.0))
                .andExpect(jsonPath("$.summary.totalPagu").value(280_000_000.0))
                .andExpect(jsonPath("$.summary.totalRealisasi").value(10_000_000.0))
                .andExpect(jsonPath("$.summary.totalSisa").value(270_000_000.0));
        mvc.perform(get("/api/rka").param("page", "0").param("q", "internet 1"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.summary.totalRealisasi").value(0));
        mvc.perform(get("/api/rka").param("page", "0").param("sort", "noCoa,asc"))
                .andExpect(jsonPath("$.items[0].noCoa").value("6103030000"))
                .andExpect(jsonPath("$.items[0].realisasi").value(10_000_000.0));

        mvc.perform(get("/api/pekerjaan").param("page", "0").param("size", "1"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.summary.selesai").value(1))
                .andExpect(jsonPath("$.summary.terlambat").value(1))
                .andExpect(jsonPath("$.summary.onTrack").value(1))
                .andExpect(jsonPath("$.summary.totalAnggaran").value(65_000_000.0));
        mvc.perform(get("/api/pekerjaan").param("page", "0").param("q", "KILLUA").param("sort", "anggaran,desc"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].nama").value("Install DB"));
        mvc.perform(get("/api/pekerjaan/pic"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("chrollo"));
        mvc.perform(get("/api/pekerjaan")).andExpect(jsonPath("$.length()").value(3));
    }

    private JsonNode postJson(String url, Object body) throws Exception {
        String res = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private static long id(JsonNode node) {
        return node.get("id").asLong();
    }
}
