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

import java.math.BigDecimal;
import java.time.LocalDate;

/** Realisasi / serapan anggaran (financial) atas sebuah pekerjaan pada periode tertentu. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "realisasi")
public class Realisasi extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pekerjaan_id")
    private Pekerjaan pekerjaan;

    @Column(nullable = false)
    private LocalDate tanggal;

    @Column(name = "periode_tahun", nullable = false)
    private Integer periodeTahun;

    @Column(name = "periode_bulan", nullable = false)
    private Integer periodeBulan;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal nilai;

    @Column(name = "no_dokumen", length = 100)
    private String noDokumen;

    @Column(length = 1000)
    private String keterangan;
}
