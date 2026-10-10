package id.co.realisasi.repository;

import id.co.realisasi.domain.MataAnggaran;
import id.co.realisasi.domain.TipeAnggaran;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MataAnggaranRepository extends JpaRepository<MataAnggaran, Long> {

    Optional<MataAnggaran> findByNoCoa(String noCoa);

    boolean existsByNoCoa(String noCoa);

    List<MataAnggaran> findByTipeOrderByNoCoa(TipeAnggaran tipe);

    List<MataAnggaran> findAllByOrderByNoCoa();
}
