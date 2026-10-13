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

    /**
     * Baris RKA yang dibebani sebuah realisasi: pekerjaan single year membebani RKA-nya sendiri; pekerjaan multi
     * year membebani RKA divisi dan mata anggaran yang sama pada tahun tanggal realisasi.
     * Dipakai sebagai kondisi join {@code r -> k -> p -> t} (t = RKA terbeban).
     */
    String RKA_TERBEBAN = """
            ((k.jenisPeriode = id.co.realisasi.domain.JenisPeriode.SINGLE_YEAR and t.id = p.id)
              or (k.jenisPeriode = id.co.realisasi.domain.JenisPeriode.MULTI_YEAR
                  and t.divisi.id = p.divisi.id and t.mataAnggaran.id = p.mataAnggaran.id
                  and t.tahun = r.periodeTahun))
            """;

    /** Total realisasi per RKA terbeban: baris [programKerjaId, total]. */
    @Query("""
            select t.id, coalesce(sum(r.nilai), 0)
            from Realisasi r
              join r.pekerjaan k
              join k.programKerja p
              join ProgramKerja t on
            """ + RKA_TERBEBAN + """
            where t.id in :programIds
            group by t.id
            """)
    List<Object[]> sumByProgramKerja(@Param("programIds") Collection<Long> programIds);

    /** Realisasi pekerjaan multi year yang membebani RKA ini lewat tahun realisasinya. */
    @Query("""
            select count(r) > 0
            from Realisasi r
              join r.pekerjaan k
              join k.programKerja p
              join ProgramKerja t on
            """ + RKA_TERBEBAN + """
            where t.id = :programId and k.jenisPeriode = id.co.realisasi.domain.JenisPeriode.MULTI_YEAR
            """)
    boolean existsMultiYearCharge(@Param("programId") Long programId);

    /** Tahun-tahun realisasi sebuah pekerjaan. */
    @Query("select distinct r.periodeTahun from Realisasi r where r.pekerjaan.id = :pekerjaanId")
    List<Integer> tahunRealisasi(@Param("pekerjaanId") Long pekerjaanId);

    /** Realisasi per pekerjaan dipisah sebelum / pada tahun tertentu: baris [pekerjaanId, sebelum, padaTahun]. */
    @Query("""
            select r.pekerjaan.id,
                   coalesce(sum(case when r.periodeTahun < :tahun then r.nilai else 0 end), 0),
                   coalesce(sum(case when r.periodeTahun = :tahun then r.nilai else 0 end), 0)
            from Realisasi r
            where r.pekerjaan.id in :pekerjaanIds
            group by r.pekerjaan.id
            """)
    List<Object[]> sumByPekerjaanPerTahun(@Param("pekerjaanIds") Collection<Long> pekerjaanIds,
                                          @Param("tahun") Integer tahun);

    /** Total realisasi per pekerjaan: baris [pekerjaanId, total]. */
    @Query("""
            select r.pekerjaan.id, coalesce(sum(r.nilai), 0)
            from Realisasi r
            where r.pekerjaan.id in :pekerjaanIds
            group by r.pekerjaan.id
            """)
    List<Object[]> sumByPekerjaan(@Param("pekerjaanIds") Collection<Long> pekerjaanIds);

    /** Total realisasi atas RKA terbeban yang cocok dengan filter list RKA (lihat ProgramKerjaRepository#page). */
    @Query("""
            select coalesce(sum(r.nilai), 0)
            from Realisasi r
              join r.pekerjaan k
              join k.programKerja p
              join ProgramKerja t on
            """ + RKA_TERBEBAN + """
              join t.divisi d
              join d.company c
              join t.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or t.tahun = :tahun)
              and (:tipe is null or t.tipe = :tipe)
              and (:q is null or lower(m.noCoa) like :q or lower(m.nama) like :q or lower(d.nama) like :q
                   or lower(c.nama) like :q or lower(coalesce(t.keterangan, '')) like :q)
            """)
    BigDecimal sumForRka(@Param("companyId") Long companyId,
                         @Param("divisiId") Long divisiId,
                         @Param("tahun") Integer tahun,
                         @Param("tipe") TipeAnggaran tipe,
                         @Param("q") String q);

    @Query("select coalesce(sum(r.nilai), 0) from Realisasi r where r.pekerjaan.id = :pekerjaanId")
    BigDecimal sumForPekerjaan(@Param("pekerjaanId") Long pekerjaanId);
}
