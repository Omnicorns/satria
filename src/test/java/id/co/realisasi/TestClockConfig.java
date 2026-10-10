package id.co.realisasi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfig {

    /** "Hari ini" = 30 Sep 2026 agar status terlambat deterministik. */
    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-09-30T03:00:00Z"), ZoneId.of("Asia/Jakarta"));
    }
}
