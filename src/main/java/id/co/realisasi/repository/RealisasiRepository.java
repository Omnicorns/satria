package id.co.realisasi.repository;

import id.co.realisasi.domain.Realisasi;
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

    @Query("select coalesce(sum(r.nilai), 0) from Realisasi r where r.pekerjaan.id = :pekerjaanId")
    BigDecimal sumForPekerjaan(@Param("pekerjaanId") Long pekerjaanId);
}
