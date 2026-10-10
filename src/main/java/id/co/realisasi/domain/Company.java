package id.co.realisasi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entitas / perusahaan dalam grup (contoh: InJ Retail). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company")
public class Company extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String kode;

    @Column(nullable = false, length = 150)
    private String nama;

    @Column(name = "nama_grup", length = 150)
    private String namaGrup;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;
}
