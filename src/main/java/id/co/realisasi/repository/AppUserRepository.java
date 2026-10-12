package id.co.realisasi.repository;

import id.co.realisasi.domain.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAllByOrderByUsernameAsc();

    /** User beserta departemen AD dan divisi anggaran hasil pemetaannya. */
    @EntityGraph(attributePaths = {"ldapDepartemen", "ldapDepartemen.divisi", "ldapDepartemen.divisi.company"})
    Optional<AppUser> findWithDivisiById(Long id);

    @EntityGraph(attributePaths = {"ldapDepartemen", "ldapDepartemen.divisi"})
    List<AppUser> findAllWithDepartemenByOrderByUsernameAsc();
}
