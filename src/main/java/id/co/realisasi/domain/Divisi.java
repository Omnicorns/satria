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

/** Divisi / unit pemilik anggaran di bawah perusahaan (contoh: Technology Group di InJ Retail). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "divisi")
public class Divisi extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, unique = true, length = 30)
    private String kode;

    @Column(nullable = false, length = 150)
    private String nama;

    @Column(nullable = false)
    private boolean aktif = true;
}
