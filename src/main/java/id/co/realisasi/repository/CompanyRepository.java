package id.co.realisasi.repository;

import id.co.realisasi.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByKode(String kode);

    boolean existsByKode(String kode);
}
