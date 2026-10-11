package id.co.realisasi.repository;

import id.co.realisasi.domain.Realisasi;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface RealisasiRepository extends JpaRepository<Realisasi, Long> {

    List<Realisasi> findByPekerjaanIdOrderByTanggalAsc(Long pekerjaanId);

    void deleteByPekerjaanId(Long pekerjaanId);

    /** Total realisasi per program kerja: baris [programKerjaId, total]. */
    @Query("""
            select r.pekerjaan.programKerja.id, coalesce(sum(r.nilai), 0)
            from Realisasi r
            where r.pekerjaan.programKerja.id in :programIds
            group by r.pekerjaan.programKerja.id
            """)
    List<Object[]> sumByProgramKerja(@Param("programIds") Collection<Long> programIds);

    /** Total realisasi per pekerjaan: baris [pekerjaanId, total]. */
    @Query("""
            select r.pekerjaan.id, coalesce(sum(r.nilai), 0)
            from Realisasi r
            where r.pekerjaan.id in :pekerjaanIds
            group by r.pekerjaan.id
            """)
    List<Object[]> sumByPekerjaan(@Param("pekerjaanIds") Collection<Long> pekerjaanIds);

    /** Total realisasi atas program kerja yang cocok dengan filter list RKA (lihat ProgramKerjaRepository#page). */
    @Query("""
            select coalesce(sum(r.nilai), 0)
            from Realisasi r
              join r.pekerjaan k
              join k.programKerja p
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
    BigDecimal sumForRka(@Param("companyId") Long companyId,
                         @Param("divisiId") Long divisiId,
                         @Param("tahun") Integer tahun,
                         @Param("tipe") TipeAnggaran tipe,
                         @Param("q") String q);

    @Query("select coalesce(sum(r.nilai), 0) from Realisasi r where r.pekerjaan.id = :pekerjaanId")
    BigDecimal sumForPekerjaan(@Param("pekerjaanId") Long pekerjaanId);
}
