package id.co.realisasi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sync memakai direktori dev (7 user, 3 departemen, 1 user tanpa departemen). */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:realisasi-ldap-sync;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LdapSyncIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Value("${ldap.dev-password:dev}")
    String devPass;

    @Test
    void syncMengelompokkanUserPerDepartemen() throws Exception {
        // User yang sudah login sebelum sync ikut diperbarui, bukan diduplikasi.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "rina.wulandari", "password", devPass))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/ldap/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUserAd").value(7))
                .andExpect(jsonPath("$.userBaru").value(6))
                .andExpect(jsonPath("$.userDiperbarui").value(1))
                .andExpect(jsonPath("$.totalDepartemen").value(3))
                // "Technology Group" sudah dibuat saat rina login.
                .andExpect(jsonPath("$.departemenBaru").value(2));

        mvc.perform(get("/api/ldap/departemen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].nama").value("Finance & Accounting"))
                .andExpect(jsonPath("$[0].jumlahUser").value(2))
                .andExpect(jsonPath("$[2].nama").value("Technology Group"))
                .andExpect(jsonPath("$[2].users[*].username")
                        .value(org.hamcrest.Matchers.contains("agung.pribadi", "dimas.saputra", "rina.wulandari")))
                .andExpect(jsonPath("$[3].id").doesNotExist())
                .andExpect(jsonPath("$[3].nama").value("(Tanpa departemen)"))
                .andExpect(jsonPath("$[3].users[0].username").value("svc.backup"));

        // Login setelah sync tetap menyimpan departemen user.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "agung.pribadi", "password", devPass))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/ldap/departemen"))
                .andExpect(jsonPath("$[2].jumlahUser").value(3));

        // Sync kedua idempoten.
        mvc.perform(post("/api/ldap/sync"))
                .andExpect(jsonPath("$.userBaru").value(0))
                .andExpect(jsonPath("$.departemenBaru").value(0));
    }

    @Test
    void departemenBisaDipetakanKeDivisi() throws Exception {
        mvc.perform(post("/api/ldap/sync")).andExpect(status().isOk());
        long companyId = json.readTree(mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("kode", "INJ", "nama", "InJ Retail"))))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        long divisiId = json.readTree(mvc.perform(post("/api/divisi").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("companyId", companyId, "kode", "TG",
                        "nama", "Technology Group"))))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        long deptId = json.readTree(mvc.perform(get("/api/ldap/departemen"))
                .andReturn().getResponse().getContentAsString()).get(2).get("id").asLong();

        mvc.perform(put("/api/ldap/departemen/{id}/divisi", deptId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"divisiId\":" + divisiId + "}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/ldap/departemen"))
                .andExpect(jsonPath("$[2].divisiNama").value("Technology Group"));
    }
}
