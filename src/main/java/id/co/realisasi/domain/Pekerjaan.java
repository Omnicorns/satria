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

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pekerjaan CAPEX/OPEX (contoh: Managed Service POS &amp; EDC) yang membebani satu program kerja RKA. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pekerjaan")
public class Pekerjaan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_kerja_id")
    private ProgramKerja programKerja;

    @Column(nullable = false, length = 200)
    private String nama;

    @Column(length = 2000)
    private String deskripsi;

    @Column(nullable = false, length = 100)
    private String pic;

    @Column(nullable = false)
    private Integer tahun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipeAnggaran tipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "jenis_periode", nullable = false, length = 15)
    private JenisPeriode jenisPeriode = JenisPeriode.SINGLE_YEAR;

    /** Nilai anggaran / kontrak pekerjaan (commitment). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal anggaran = BigDecimal.ZERO;

    @Column(name = "tanggal_mulai")
    private LocalDate tanggalMulai;

    @Column(name = "estimasi_selesai")
    private LocalDate estimasiSelesai;

    /** Progress terakhir dalam persen (0-100). */
    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "tindak_lanjut", length = 2000)
    private String tindakLanjut;

    public StatusPekerjaan status(LocalDate today) {
        if (progress != null && progress >= 100) {
            return StatusPekerjaan.SELESAI;
        }
        if (estimasiSelesai != null && estimasiSelesai.isBefore(today)) {
            return StatusPekerjaan.TERLAMBAT;
        }
        return StatusPekerjaan.ON_TRACK;
    }
}
