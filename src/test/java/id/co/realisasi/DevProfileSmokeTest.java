package id.co.realisasi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Profil dev memuat data contoh yang sama dengan screenshot (InJ Retail, usulan RKAP 2027 berisi 5 akun OPEX teratas + 1 CAPEX). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:realisasi-dev-smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestClockConfig.class)
class DevProfileSmokeTest {

    @Autowired
    MockMvc mvc;

    @Test
    void dataContohTerbaca() throws Exception {
        mvc.perform(get("/api/dashboard/group-overview").param("tahun", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi.rka").value(11450000000.0))
                .andExpect(jsonPath("$.kpi.usulanRkap").value(8892045981.0))
                .andExpect(jsonPath("$.topProgramOpexUsulan[0].nama").value("Beban Sewa Penggunaan Lisensi"));

        mvc.perform(get("/api/pekerjaan").param("tahun", "2026"))
                .andExpect(jsonPath("$.length()").value(5));

        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
