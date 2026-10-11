package id.co.realisasi.repository;

import id.co.realisasi.domain.MataAnggaran;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MataAnggaranRepository extends JpaRepository<MataAnggaran, Long> {

    Optional<MataAnggaran> findByNoCoa(String noCoa);

    boolean existsByNoCoa(String noCoa);

    List<MataAnggaran> findByTipeOrderByNoCoa(TipeAnggaran tipe);

    List<MataAnggaran> findAllByOrderByNoCoa();

    /** Versi berhalaman untuk tabel master; q mencari no COA / nama / kategori. */
    @Query("""
            select m from MataAnggaran m
            where (:tipe is null or m.tipe = :tipe)
              and (:aktif is null or m.aktif = :aktif)
              and (:q is null or lower(m.noCoa) like :q or lower(m.nama) like :q
                   or lower(coalesce(m.kategori, '')) like :q)
            """)
    Page<MataAnggaran> page(@Param("tipe") TipeAnggaran tipe,
                            @Param("aktif") Boolean aktif,
                            @Param("q") String q,
                            Pageable pageable);
}
