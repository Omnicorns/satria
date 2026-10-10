package id.co.realisasi.repository;

import id.co.realisasi.domain.LdapDepartemen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LdapDepartemenRepository extends JpaRepository<LdapDepartemen, Long> {

    Optional<LdapDepartemen> findByNamaIgnoreCase(String nama);

    @EntityGraph(attributePaths = "divisi")
    List<LdapDepartemen> findAllByOrderByNamaAsc();
}
