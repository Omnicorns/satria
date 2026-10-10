package id.co.realisasi.repository;

import id.co.realisasi.domain.ProgramKerja;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgramKerjaRepository extends JpaRepository<ProgramKerja, Long> {

    /** Parameter null berarti tanpa filter (misal "Semua tahun"). */
    @Query("""
            select p from ProgramKerja p
              join fetch p.divisi d
              join fetch d.company c
              join fetch p.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or p.tahun = :tahun)
              and (:tipe is null or p.tipe = :tipe)
            order by p.tahun desc, p.createdAt desc
            """)
    List<ProgramKerja> search(@Param("companyId") Long companyId,
                              @Param("divisiId") Long divisiId,
                              @Param("tahun") Integer tahun,
                              @Param("tipe") TipeAnggaran tipe);

    @Query("""
            select p from ProgramKerja p
              join fetch p.divisi d
              join fetch d.company c
              join fetch p.mataAnggaran m
            where p.id = :id
            """)
    Optional<ProgramKerja> findDetail(@Param("id") Long id);

    Optional<ProgramKerja> findByDivisiIdAndTahunAndMataAnggaranId(Long divisiId, Integer tahun,
                                                                  Long mataAnggaranId);
}
