package id.co.realisasi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Master mata anggaran / Chart of Account (contoh: 6104050001 Beban Sewa Mesin dan Peralatan). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mata_anggaran")
public class MataAnggaran extends BaseEntity {

    @Column(name = "no_coa", nullable = false, unique = true, length = 30)
    private String noCoa;

    @Column(nullable = false, length = 200)
    private String nama;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipeAnggaran tipe;

    @Column(length = 100)
    private String kategori;

    @Column(nullable = false)
    private boolean aktif = true;
}
