package id.co.realisasi.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Mengaktifkan sync LDAP terjadwal ({@code ldap.sync-cron}, default "-" = mati). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
