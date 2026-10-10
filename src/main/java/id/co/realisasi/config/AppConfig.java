package id.co.realisasi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Jakarta}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${app.cors.allowed-origins:http://localhost:3000}") String[] origins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Disposition");
            }
        };
    }
}
