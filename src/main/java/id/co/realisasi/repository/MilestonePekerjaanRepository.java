package id.co.realisasi.repository;

import id.co.realisasi.domain.MilestonePekerjaan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestonePekerjaanRepository extends JpaRepository<MilestonePekerjaan, Long> {

    List<MilestonePekerjaan> findByPekerjaanIdOrderByTanggalSelesaiAsc(Long pekerjaanId);

    void deleteByPekerjaanId(Long pekerjaanId);
}
