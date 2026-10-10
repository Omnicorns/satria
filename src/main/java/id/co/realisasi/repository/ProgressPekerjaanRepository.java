package id.co.realisasi.repository;

import id.co.realisasi.domain.ProgressPekerjaan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressPekerjaanRepository extends JpaRepository<ProgressPekerjaan, Long> {

    List<ProgressPekerjaan> findByPekerjaanIdOrderByTanggalDescIdDesc(Long pekerjaanId);

    void deleteByPekerjaanId(Long pekerjaanId);
}
