package id.co.realisasi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Riwayat update progress pekerjaan beserta poin tindak lanjut. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "progress_pekerjaan")
public class ProgressPekerjaan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pekerjaan_id")
    private Pekerjaan pekerjaan;

    @Column(nullable = false)
    private LocalDate tanggal;

    @Column(nullable = false)
    private Integer persen;

    @Column(length = 2000)
    private String catatan;

    @Column(name = "tindak_lanjut", length = 2000)
    private String tindakLanjut;

    @Column(name = "dilaporkan_oleh", length = 100)
    private String dilaporkanOleh;
}
