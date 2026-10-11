package id.co.realisasi.repository;

import id.co.realisasi.domain.ProgramKerja;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /** Versi berhalaman untuk tabel Daftar Program Kerja; q mencari COA / mata anggaran / divisi / keterangan. */
    @EntityGraph(attributePaths = {"divisi", "divisi.company", "mataAnggaran"})
    @Query("""
            select p from ProgramKerja p
              join p.divisi d
              join d.company c
              join p.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or p.tahun = :tahun)
              and (:tipe is null or p.tipe = :tipe)
              and (:q is null or lower(m.noCoa) like :q or lower(m.nama) like :q or lower(d.nama) like :q
                   or lower(c.nama) like :q or lower(coalesce(p.keterangan, '')) like :q)
            """)
    Page<ProgramKerja> page(@Param("companyId") Long companyId,
                              @Param("divisiId") Long divisiId,
                              @Param("tahun") Integer tahun,
                              @Param("tipe") TipeAnggaran tipe,
                              @Param("q") String q,
                              Pageable pageable);

    /** Total planning dan plafon (pagu, atau planning jika pagu kosong) atas seluruh data yang cocok. */
    @Query("""
            select coalesce(sum(p.planningBudget), 0), coalesce(sum(coalesce(p.pagu, p.planningBudget)), 0)
            from ProgramKerja p
              join p.divisi d
              join d.company c
              join p.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or p.tahun = :tahun)
              and (:tipe is null or p.tipe = :tipe)
              and (:q is null or lower(m.noCoa) like :q or lower(m.nama) like :q or lower(d.nama) like :q
                   or lower(c.nama) like :q or lower(coalesce(p.keterangan, '')) like :q)
            """)
    List<Object[]> totals(@Param("companyId") Long companyId,
                              @Param("divisiId") Long divisiId,
                              @Param("tahun") Integer tahun,
                              @Param("tipe") TipeAnggaran tipe,
                              @Param("q") String q);

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
