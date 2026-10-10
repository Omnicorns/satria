package id.co.realisasi.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(LdapProperties.class)
public class AuthConfig {

    @Bean
    DirectoryAuthenticator directoryAuthenticator(LdapProperties props,
                                                  @Value("${ldap.dev-password:dev}") String devPassword) {
        if (props.enabled()) {
            log.info("Login memakai Active Directory {} (domain {})", props.url(), props.domain());
            return new LdapDirectoryAuthenticator(props);
        }
        log.warn("ldap.enabled=false: login memakai autentikator DEV (semua username, password '{}'), "
                + "bukan Active Directory", devPassword);
        return new DevDirectoryAuthenticator(devPassword);
    }
}
