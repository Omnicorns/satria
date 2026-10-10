package id.co.realisasi.repository;

import id.co.realisasi.domain.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAllByOrderByUsernameAsc();

    @EntityGraph(attributePaths = "ldapDepartemen")
    List<AppUser> findAllWithDepartemenByOrderByUsernameAsc();
}
