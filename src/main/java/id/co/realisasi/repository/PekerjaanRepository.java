package id.co.realisasi.repository;

import id.co.realisasi.domain.Pekerjaan;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PekerjaanRepository extends JpaRepository<Pekerjaan, Long> {

    /**
     * Filter program kerja: pekerjaan yang ditautkan ke RKA itu, ditambah pekerjaan multi year dengan divisi dan
     * mata anggaran yang sama yang mulai paling lambat tahun RKA tersebut dan membebaninya (kontrak masih bersisa
     * atau sudah ada realisasi di tahun itu). Sama dengan aturan carry-over di Monitoring RKA.
     */
    String FILTER_PROGRAM = """
              and (:programKerjaId is null or p.id = :programKerjaId
                   or (k.jenisPeriode = id.co.realisasi.domain.JenisPeriode.MULTI_YEAR and exists (
                         select f.id from ProgramKerja f
                         where f.id = :programKerjaId and f.divisi.id = p.divisi.id
                           and f.mataAnggaran.id = p.mataAnggaran.id and k.tahun <= f.tahun
                           and ((select coalesce(sum(r.nilai), 0) from Realisasi r where r.pekerjaan.id = k.id)
                                  < k.anggaran
                                or exists (select r2.id from Realisasi r2
                                           where r2.pekerjaan.id = k.id and r2.periodeTahun = f.tahun)))))
            """;

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
            """ + FILTER_PROGRAM + """
            order by k.createdAt desc
            """)
    List<Pekerjaan> search(@Param("companyId") Long companyId,
                           @Param("divisiId") Long divisiId,
                           @Param("tahun") Integer tahun,
                           @Param("tipe") TipeAnggaran tipe,
                           @Param("pic") String pic,
                           @Param("programKerjaId") Long programKerjaId);

    /** Versi berhalaman untuk tabel Daftar Pekerjaan; q mencari nama / PIC / divisi / COA. */
    @EntityGraph(attributePaths = {"programKerja", "programKerja.divisi", "programKerja.divisi.company",
            "programKerja.mataAnggaran"})
    @Query("""
            select k
            from Pekerjaan k
              join k.programKerja p
              join p.divisi d
              join d.company c
              join p.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or k.tahun = :tahun)
              and (:tipe is null or k.tipe = :tipe)
              and (:pic is null or k.pic = :pic)
            """ + FILTER_PROGRAM + """
              and (:q is null or lower(k.nama) like :q or lower(k.pic) like :q or lower(d.nama) like :q
                   or lower(m.noCoa) like :q or lower(m.nama) like :q)
            """)
    Page<Pekerjaan> page(@Param("companyId") Long companyId,
                    @Param("divisiId") Long divisiId,
                    @Param("tahun") Integer tahun,
                    @Param("tipe") TipeAnggaran tipe,
                    @Param("pic") String pic,
                    @Param("programKerjaId") Long programKerjaId,
                    @Param("q") String q,
                    Pageable pageable);

    /**
     * Ringkasan untuk kartu status: [selesai, terlambat, total, totalAnggaran].
     * Aturan status sama dengan {@link Pekerjaan#status}.
     */
    @Query("""
            select sum(case when k.progress >= 100 then 1 else 0 end),
                   sum(case when (k.progress is null or k.progress < 100) and k.estimasiSelesai < :today
                            then 1 else 0 end),
                   count(k),
                   coalesce(sum(k.anggaran), 0)
            from Pekerjaan k
              join k.programKerja p
              join p.divisi d
              join d.company c
              join p.mataAnggaran m
            where (:companyId is null or c.id = :companyId)
              and (:divisiId is null or d.id = :divisiId)
              and (:tahun is null or k.tahun = :tahun)
              and (:tipe is null or k.tipe = :tipe)
              and (:pic is null or k.pic = :pic)
            """ + FILTER_PROGRAM + """
              and (:q is null or lower(k.nama) like :q or lower(k.pic) like :q or lower(d.nama) like :q
                   or lower(m.noCoa) like :q or lower(m.nama) like :q)
            """)
    List<Object[]> summary(@Param("companyId") Long companyId,
                    @Param("divisiId") Long divisiId,
                    @Param("tahun") Integer tahun,
                    @Param("tipe") TipeAnggaran tipe,
                    @Param("pic") String pic,
                    @Param("programKerjaId") Long programKerjaId,
                    @Param("q") String q,
                           @Param("today") LocalDate today);

    @Query("select distinct k.pic from Pekerjaan k where k.pic is not null order by k.pic")
    List<String> findDistinctPic();

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
