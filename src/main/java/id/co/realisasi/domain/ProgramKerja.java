package id.co.realisasi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Baris RKA: satu mata anggaran untuk satu divisi dan tahun. Perusahaan diturunkan dari divisi.
 * Planning = nilai usulan RKA, pagu = plafon yang disetujui (boleh kosong).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "program_kerja",
        uniqueConstraints = @UniqueConstraint(name = "uk_program_divisi_tahun_coa",
                columnNames = {"divisi_id", "tahun", "mata_anggaran_id"}))
public class ProgramKerja extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "divisi_id")
    private Divisi divisi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mata_anggaran_id")
    private MataAnggaran mataAnggaran;

    @Column(nullable = false)
    private Integer tahun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipeAnggaran tipe;

    @Column(name = "planning_budget", nullable = false, precision = 19, scale = 2)
    private BigDecimal planningBudget = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal pagu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusProgram status = StatusProgram.OPEN;

    @Column(length = 1000)
    private String keterangan;

    public Company getCompany() {
        return divisi.getCompany();
    }

    /** Plafon efektif: pagu bila sudah ditetapkan, selain itu planning. */
    public BigDecimal plafon() {
        return pagu != null ? pagu : planningBudget;
    }
}
