package id.co.realisasi.repository;

import id.co.realisasi.domain.Divisi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DivisiRepository extends JpaRepository<Divisi, Long> {

    @Query("""
            select d from Divisi d join fetch d.company c
            where (:companyId is null or c.id = :companyId)
            order by c.nama, d.nama
            """)
    List<Divisi> search(@Param("companyId") Long companyId);

    @Query("select d from Divisi d join fetch d.company where d.id = :id")
    Optional<Divisi> findDetail(@Param("id") Long id);

    @Query("select d from Divisi d join fetch d.company where d.kode = :kode")
    Optional<Divisi> findByKode(@Param("kode") String kode);

    boolean existsByKode(String kode);
}
