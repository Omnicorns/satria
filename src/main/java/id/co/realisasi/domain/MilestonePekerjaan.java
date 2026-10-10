package id.co.realisasi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Tahapan pada timeline pekerjaan. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "milestone_pekerjaan")
public class MilestonePekerjaan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pekerjaan_id")
    private Pekerjaan pekerjaan;

    @Column(nullable = false, length = 200)
    private String nama;

    @Column(name = "tanggal_mulai")
    private LocalDate tanggalMulai;

    @Column(name = "tanggal_selesai", nullable = false)
    private LocalDate tanggalSelesai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusMilestone status = StatusMilestone.BELUM_MULAI;

    @Column(length = 1000)
    private String catatan;
}
