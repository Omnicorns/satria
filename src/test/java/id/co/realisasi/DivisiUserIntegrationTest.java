package id.co.realisasi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Program kerja dikunci ke divisi user (pemetaan departemen AD); admin-users bebas memilih divisi. */
@SpringBootTest(properties = {"app.auth.required=true", "app.auth.admin-users=agung.pribadi",
        "spring.datasource.url=jdbc:h2:mem:realisasi-divisi-user;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DivisiUserIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    /** Password autentikator dev (ldap.enabled=false), bukan kredensial asli. */
    @Value("${ldap.dev-password:dev}")
    String devPass;

    String admin;
    long finance;
    long tech;
    long coa;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("agung.pribadi");
        long company = id(postJson(admin, "/api/companies", Map.of("kode", "SRN", "nama", "PT Sarinah")));
        finance = id(postJson(admin, "/api/divisi", Map.of("companyId", company, "kode", "SRN-FA",
                "nama", "Finance & Accounting")));
        tech = id(postJson(admin, "/api/divisi", Map.of("companyId", company, "kode", "SRN-TG",
                "nama", "Technology")));
        coa = id(postJson(admin, "/api/mata-anggaran", Map.of("noCoa", "6104050001", "nama", "Beban Sewa",
                "tipe", "OPEX")));
        // Admin membuat program kerja untuk divisi lain (Technology): admin tidak dikunci.
        postJson(admin, "/api/rka", Map.of("divisiId", tech, "tahun", 2026, "mataAnggaranId", coa,
                "planningBudget", 500));
    }

    @Test
    void userBiasaDikunciKeDivisinya() throws Exception {
        String budi = login("budi.santoso");
        // Departemen AD "Finance & Accounting" belum dipetakan: ditolak dengan pesan jelas.
        mvc.perform(get("/api/rka").header("Authorization", "Bearer " + budi))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("belum dipetakan")));

        petakan("Finance & Accounting", finance);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + budi))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.divisiId").value(finance))
                .andExpect(jsonPath("$.divisiNama").value("Finance & Accounting"));

        // divisiId di request diabaikan: tersimpan ke divisi user.
        long own = id(postJson(budi, "/api/rka", Map.of("divisiId", tech, "tahun", 2026, "mataAnggaranId", coa,
                "planningBudget", 100)));
        mvc.perform(get("/api/rka/" + own).header("Authorization", "Bearer " + budi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.divisiId").value(finance));

        // Daftar hanya divisinya walau filter meminta divisi lain.
        mvc.perform(get("/api/rka").param("divisiId", String.valueOf(tech)).header("Authorization", "Bearer " + budi))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].divisiId").value(finance));
        mvc.perform(get("/api/rka").param("page", "0").header("Authorization", "Bearer " + budi))
                .andExpect(jsonPath("$.total").value(1));

        long other = techProgram();
        mvc.perform(get("/api/rka/" + other).header("Authorization", "Bearer " + budi))
                .andExpect(status().isForbidden());
        send(budi, put("/api/rka/" + other), Map.of("divisiId", finance, "tahun", 2026, "mataAnggaranId", coa,
                "planningBudget", 1)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/rka/" + other).header("Authorization", "Bearer " + budi))
                .andExpect(status().isForbidden());

        // Import Excel: baris divisi lain ditolak.
        mvc.perform(multipart("/api/rka/excel/import").file(xlsx("SRN-TG")).header("Authorization", "Bearer " + budi))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0]").value("Baris 2: Kode Divisi SRN-TG bukan divisi Anda"));
        mvc.perform(multipart("/api/rka/excel/import").file(xlsx("SRN-FA")).header("Authorization", "Bearer " + budi))
                .andExpect(status().isOk());
    }

    @Test
    void pekerjaanHanyaDivisiSendiri() throws Exception {
        String budi = login("budi.santoso");
        petakan("Finance & Accounting", finance);
        long techPk = techProgram();
        long ownPk = id(postJson(budi, "/api/rka", Map.of("divisiId", finance, "tahun", 2026,
                "mataAnggaranId", coa, "planningBudget", 100)));
        long techJob = id(postJson(admin, "/api/pekerjaan", Map.of("programKerjaId", techPk, "nama", "Server",
                "pic", "Agung", "anggaran", 10)));
        postJson(budi, "/api/pekerjaan", Map.of("programKerjaId", ownPk, "nama", "Audit", "pic", "Budi",
                "anggaran", 5));

        // Daftar dan halaman hanya divisinya walau filter meminta divisi lain.
        mvc.perform(get("/api/pekerjaan").param("divisiId", String.valueOf(tech))
                        .header("Authorization", "Bearer " + budi))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nama").value("Audit"));
        mvc.perform(get("/api/pekerjaan").param("page", "0").header("Authorization", "Bearer " + budi))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/pekerjaan").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(2));

        // Pekerjaan / program divisi lain ditolak.
        mvc.perform(get("/api/pekerjaan/" + techJob).header("Authorization", "Bearer " + budi))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/pekerjaan/" + techJob + "/financial").header("Authorization", "Bearer " + budi))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/pekerjaan/" + techJob).header("Authorization", "Bearer " + budi))
                .andExpect(status().isForbidden());
        send(budi, post("/api/pekerjaan"), Map.of("programKerjaId", techPk, "nama", "X", "pic", "Budi",
                "anggaran", 1)).andExpect(status().isForbidden());
    }

    @Test
    void adminBebasMemilihDivisi() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.admin").value(true));
        postJson(admin, "/api/rka", Map.of("divisiId", finance, "tahun", 2026, "mataAnggaranId", coa,
                "planningBudget", 100));
        mvc.perform(get("/api/rka").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(2));
    }

    private long techProgram() throws Exception {
        JsonNode list = json.readTree(mvc.perform(get("/api/rka").param("divisiId", String.valueOf(tech))
                .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString());
        return list.get(0).get("id").asLong();
    }

    private void petakan(String departemen, long divisiId) throws Exception {
        JsonNode groups = json.readTree(mvc.perform(get("/api/ldap/departemen")
                .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString());
        for (JsonNode g : groups) {
            if (departemen.equals(g.get("nama").asText())) {
                send(admin, put("/api/ldap/departemen/" + g.get("id").asLong() + "/divisi"),
                        Map.of("divisiId", divisiId)).andExpect(status().isNoContent());
                return;
            }
        }
        throw new AssertionError("Departemen " + departemen + " tidak ada");
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", devPass))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private ResultActions send(String token,
                               org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                               Object body) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private JsonNode postJson(String token, String url, Object body) throws Exception {
        String res = send(token, post(url), body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private static long id(JsonNode node) {
        return node.get("id").asLong();
    }

    private static MockMultipartFile xlsx(String kodeDivisi) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("RKA");
            Row h = s.createRow(0);
            h.createCell(0).setCellValue("Kode Divisi");
            Row r = s.createRow(1);
            r.createCell(0).setCellValue(kodeDivisi);
            r.createCell(1).setCellValue("2027");
            r.createCell(2).setCellValue("6104050001");
            r.createCell(4).setCellValue(250);
            wb.write(out);
            return new MockMultipartFile("file", "rka.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
