package id.co.realisasi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.realisasi.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login (autentikator dev, tanpa AD) dengan app.auth.required=true seperti di production. */
@SpringBootTest(properties = {"app.auth.required=true",
        "spring.datasource.url=jdbc:h2:mem:realisasi-auth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class AuthApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    AppUserRepository users;

    /** Password autentikator dev (ldap.enabled=false), bukan kredensial asli. */
    @Value("${ldap.dev-password:dev}")
    String devPass;

    @Test
    void apiTanpaTokenDitolak() throws Exception {
        mvc.perform(get("/api/companies")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Silakan login terlebih dahulu"));
        mvc.perform(get("/api/companies").header("Authorization", "Bearer palsu"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginMenyimpanUserDanTokenBisaDipakai() throws Exception {
        String token = login("SARINAH\\Budi.Santoso", devPass);

        assertThat(users.findByUsername("budi.santoso")).hasValueSatisfying(u -> {
            assertThat(u.getEmail()).isEqualTo("budi.santoso@sarinah.net");
            assertThat(u.getLastLoginAt()).isNotNull();
        });

        mvc.perform(get("/api/companies").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("budi.santoso"))
                .andExpect(jsonPath("$.sumber").value("DEV"));

        // Login kedua memperbarui baris yang sama, bukan menambah user baru.
        login("budi.santoso@sarinah.net", devPass);
        assertThat(users.findAll()).filteredOn(u -> u.getUsername().equals("budi.santoso")).hasSize(1);

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordSalahDanUserNonaktif() throws Exception {
        postLogin("ani", devPass + "-salah")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Username atau password salah"));
        postLogin("ani", "")
                .andExpect(status().isBadRequest());

        login("citra", devPass);
        var u = users.findByUsername("citra").orElseThrow();
        u.setAktif(false);
        users.save(u);
        postLogin("citra", devPass)
                .andExpect(status().isUnauthorized());
    }

    private ResultActions postLogin(String username, String pass) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", pass))));
    }

    private String login(String username, String pass) throws Exception {
        String body = postLogin(username, pass)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(body);
        return n.get("token").asText();
    }
}
