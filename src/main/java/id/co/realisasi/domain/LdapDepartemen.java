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

import java.time.LocalDateTime;

/**
 * Departemen sesuai atribut {@code department} di Active Directory (teks bebas, hasil sync).
 * Dipisah dari master {@link Divisi} karena penamaan AD tidak sama dengan struktur anggaran;
 * {@link #divisi} opsional untuk memetakan departemen AD ke divisi anggaran.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ldap_departemen")
public class LdapDepartemen extends BaseEntity {

    @Column(nullable = false, unique = true, length = 200)
    private String nama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "divisi_id")
    private Divisi divisi;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
