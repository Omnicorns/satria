package id.co.realisasi.repository;

import id.co.realisasi.domain.Pekerjaan;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PekerjaanRepository extends JpaRepository<Pekerjaan, Long> {

    @Query("""
            select k from Pekerjaan k
              join fetch k.programKerja p
              join fetch p.divisi d
              join fetch d.company c
              join fetch p.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or k.tahun = :tahun)
              and (:tipe is null or k.tipe = :tipe)
              and (:pic is null or k.pic = :pic)
              and (:programKerjaId is null or p.id = :programKerjaId)
            order by k.createdAt desc
            """)
    List<Pekerjaan> search(@Param("companyId") Long companyId,
                           @Param("divisiId") Long divisiId,
                           @Param("tahun") Integer tahun,
                           @Param("tipe") TipeAnggaran tipe,
                           @Param("pic") String pic,
                           @Param("programKerjaId") Long programKerjaId);

    @Query("""
            select k from Pekerjaan k
              join fetch k.programKerja p
              join fetch p.divisi d
              join fetch d.company c
              join fetch p.mataAnggaran m
            where k.id = :id
            """)
    Optional<Pekerjaan> findDetail(@Param("id") Long id);

    boolean existsByProgramKerjaId(Long programKerjaId);
}
