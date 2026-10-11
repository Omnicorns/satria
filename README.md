# Backend Realisasi & Monitoring Anggaran

Backend Spring Boot untuk modul **Budget & RKA**: Monitoring RKA, CAPEX/OPEX (pekerjaan) dan Group Overview
(Serapan tahun berjalan vs Usulan RKAP tahun berikutnya).

- Java 17+ (diuji dengan JDK 21), Maven, Spring Boot 3.5
- Spring Data JPA, Bean Validation, Apache POI (Excel), springdoc OpenAPI
- H2 in-memory untuk dev (dengan data contoh), PostgreSQL untuk prod

## Menjalankan

```bash
# Dev: H2 + data contoh InJ Retail
mvn spring-boot:run
# Swagger UI : http://localhost:8080/swagger-ui.html
# H2 console : http://localhost:8080/h2-console  (jdbc:h2:mem:realisasi, user sa)

# Test
mvn test

# Prod: PostgreSQL (skema dari db/schema.sql, Hibernate hanya validate)
docker compose up --build
# atau manual:
DB_URL=jdbc:postgresql://localhost:5432/realisasi DB_USERNAME=realisasi DB_PASSWORD=*** \
  java -jar target/backend-realisasi-0.1.0.jar --spring.profiles.active=prod
```

Variabel lingkungan: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DDL_AUTO` (default `validate`),
`CORS_ALLOWED_ORIGINS` (default `http://localhost:3000,http://localhost:5173`).

## Model data

| Tabel | Isi |
|---|---|
| `company` | Entitas grup (InJ Retail, ...) |
| `divisi` | Divisi pemilik anggaran di bawah perusahaan (Technology Group, ...). **Anggaran diisi per divisi.** |
| `mata_anggaran` | Master COA: no COA, nama, tipe OPEX/CAPEX |
| `program_kerja` | Baris RKA: divisi + tahun + mata anggaran (unik), planning budget, pagu, status OPEN/CLOSED. Perusahaan diturunkan dari divisi |
| `pekerjaan` | Pekerjaan CAPEX/OPEX di bawah program kerja: PIC, single/multi year, anggaran (nilai kontrak), estimasi selesai, progress |
| `progress_pekerjaan` | Riwayat "Lihat progress" + poin tindak lanjut |
| `milestone_pekerjaan` | "Timeline" pekerjaan |
| `realisasi` | "Financial": realisasi/serapan per tanggal (periode tahun & bulan), no dokumen |

Aturan perhitungan:

- **Plafon** program kerja = `pagu` bila diisi, selain itu `planning_budget`.
- **Realisasi** program kerja = jumlah realisasi seluruh pekerjaannya; **Sisa** = plafon − realisasi;
  **Capaian %** = realisasi / plafon.
- Realisasi baru **ditolak (409)** bila total realisasi program kerja akan melebihi plafon.
- **Status pekerjaan**: `SELESAI` bila progress 100, `TERLAMBAT` bila lewat estimasi selesai, selain itu `ON_TRACK`.
- Progress pekerjaan mengikuti laporan progress dengan tanggal terbaru.
- Tipe (OPEX/CAPEX) program kerja dan pekerjaan diwarisi dari mata anggaran.
- Program kerja yang masih punya pekerjaan tidak bisa dihapus (409).

## Login Active Directory (LDAP)

`POST /api/auth/login` dengan `{"username": "budi.santoso", "password": "..."}` (format `SARINAH\budi.santoso`
dan `budi.santoso@sarinah.net` juga diterima). Backend bind ke AD sebagai `username@sarinah.net`, membaca
`displayName`, `mail`, `department`, `title`, lalu menyimpan/memperbarui baris di tabel `app_user`
(plus `last_login_at`). Jawaban berisi `token`; kirim sebagai `Authorization: Bearer <token>` ke semua `/api/**`.
Token berlaku 12 jam (`AUTH_SESSION_HOURS`), hanya hash-nya yang disimpan di tabel `user_session`.

| Method | Path | Keterangan |
|---|---|---|
| POST | `/api/auth/login` | 200 token + data user, 401 password salah / user dinonaktifkan, 503 AD tidak terjangkau |
| GET | `/api/auth/me` | User dari token |
| POST | `/api/auth/logout` | Hapus token |
| GET | `/api/users` | User yang pernah login (data dari AD) |

Set `aktif=false` di `app_user` untuk memblokir user walau password AD-nya benar.

### Sync departemen AD

`POST /api/ldap/sync` membaca **semua user aktif** di AD memakai service account (`LDAP_MANAGER_DN` +
`LDAP_BIND_PASSWORD`, paged 500 per halaman), lalu:

- menyimpan setiap nilai atribut `department` ke tabel `ldap_departemen` (nama unik, tidak peka huruf besar/kecil);
- membuat/memperbarui user di `app_user` (user yang belum pernah login ikut dibuat, `last_login_at` kosong)
  dan menautkannya ke departemennya lewat `app_user.ldap_departemen_id`.

`ldap_departemen` sengaja dipisah dari master `divisi`: nama departemen di AD adalah teks bebas dan tidak sama dengan
struktur anggaran. Kolom `ldap_departemen.divisi_id` (opsional) memetakan departemen AD ke divisi anggaran.

| Method | Path | Keterangan |
|---|---|---|
| POST | `/api/ldap/sync` | Jalankan sync, hasil: jumlah user/departemen baru & diperbarui |
| GET | `/api/ldap/departemen` | Departemen + daftar username di dalamnya; grup terakhir `(Tanpa departemen)` |
| PUT | `/api/ldap/departemen/{id}/divisi` | `{"divisiId": 3}` memetakan ke divisi anggaran (`null` = lepas) |

Sync otomatis: isi `LDAP_SYNC_CRON` (cron Spring, contoh `0 0 2 * * *` = tiap hari jam 02.00); default mati.
Di frontend: menu **Pengaturan → Pengguna AD**.

Variabel lingkungan:

| Variabel | Default |
|---|---|
| `LDAP_URL` | `ldap://10.0.7.12:389` |
| `LDAP_DOMAIN` | `sarinah.net` |
| `LDAP_BASE_DN` | `DC=sarinah,DC=net` |
| `LDAP_USER_SEARCH_BASE` | kosong (pakai base DN) |
| `LDAP_MANAGER_DN` | `itgroup@sarinah.net` |
| `LDAP_BIND_PASSWORD` | kosong. **Isi lewat env, jangan ditulis di file/commit.** |
| `LDAP_ENABLED` | `true` (prod), `false` di profil dev/test |
| `LDAP_SYNC_CRON` | `-` (sync otomatis mati) |
| `AUTH_REQUIRED` | `true` (prod), `false` di profil dev/test |

Profil dev/test tidak memakai AD: username apa pun bisa login dengan password `dev`.
Untuk mencoba AD dari dev: `LDAP_ENABLED=true AUTH_REQUIRED=true mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

Database yang sudah berjalan: jalankan `psql -f db/migrasi_v4_login_ldap.sql` lalu `db/migrasi_v5_sync_departemen_ad.sql`
(atau biarkan `ddl-auto=update`).

## Endpoint

Semua filter `companyId`, `divisiId`, `tahun`, `tipe` opsional (kosong = "Semua tahun" / semua entitas).

| Method | Path | Keterangan |
|---|---|---|
| GET/POST | `/api/companies` | Daftar / tambah perusahaan |
| GET/PUT | `/api/companies/{id}` | |
| GET/POST | `/api/divisi?companyId=` | Master divisi per perusahaan |
| GET/PUT | `/api/divisi/{id}` | |
| GET/POST | `/api/mata-anggaran?tipe=` | Master COA (untuk dropdown & auto-isi No COA / nama) |
| GET/PUT | `/api/mata-anggaran/{id}` | |
| GET/POST | `/api/rka?companyId=&divisiId=&tahun=&tipe=` | Monitoring RKA (form "Tambah program kerja", body berisi `divisiId`) |
| GET/PUT/DELETE | `/api/rka/{id}` | |
| GET | `/api/rka/excel/template` | Template Excel (sheet RKA + Daftar COA + Daftar Divisi) |
| GET | `/api/rka/excel/export?companyId=&divisiId=&tahun=&tipe=` | Export Excel |
| POST | `/api/rka/excel/import` (multipart `file`) | Import Excel (kolom: Kode Divisi, Tahun, No COA, Nama, Planning, Pagu, Keterangan); upsert per divisi+tahun+COA, semua-atau-tidak-sama-sekali (422 + daftar error per baris) |
| GET/POST | `/api/pekerjaan?companyId=&divisiId=&tahun=&tipe=&pic=&programKerjaId=` | CAPEX/OPEX |
| GET/PUT/DELETE | `/api/pekerjaan/{id}` | |
| GET/POST | `/api/pekerjaan/{id}/progress` | Lihat progress / update progress |
| GET/POST | `/api/pekerjaan/{id}/timeline` | Timeline (milestone) |
| PUT/DELETE | `/api/pekerjaan/{id}/timeline/{milestoneId}` | |
| GET | `/api/pekerjaan/{id}/financial` | Anggaran, total realisasi, sisa, % serapan, transaksi |
| POST | `/api/pekerjaan/{id}/realisasi` | Catat realisasi |
| DELETE | `/api/pekerjaan/{id}/realisasi/{realisasiId}` | |
| GET | `/api/dashboard/monitoring-rka` | Planning·Pagu·Realisasi per mata anggaran, distribusi capaian (0 / 1–49 / 50–99 / 100+), 10 progress terendah |
| GET | `/api/dashboard/capex-opex` | Status pekerjaan, beban kerja per PIC, 10 progress pekerjaan (terlambat dulu) |
| GET | `/api/dashboard/group-overview?companyId=&divisiId=&tahun=` | KPI RKA / Serapan / Belum terserap / Usulan RKAP (tahun+1), tabel per entitas dan per divisi, OPEX vs CAPEX, top akun & top program usulan |

Error berformat `{timestamp, status, error, message, details[]}`: 400 validasi, 404 tidak ditemukan,
409 aturan bisnis/duplikat, 422 import Excel gagal.

## Contoh request

```bash
curl -X POST localhost:8080/api/rka -H 'Content-Type: application/json' -d '{
  "divisiId": 1, "tahun": 2027, "mataAnggaranId": 1,
  "planningBudget": 1732464000}'

curl -X POST localhost:8080/api/pekerjaan/1/realisasi -H 'Content-Type: application/json' -d '{
  "tanggal": "2026-08-31", "nilai": 150000000, "noDokumen": "INV-001"}'
```

## Belum termasuk

Otorisasi per peran/scope perusahaan (login AD sudah ada), audit trail, notifikasi, modul Non Budget,
Executive Attention/Escalations. Struktur service sudah dipisah sehingga Spring Security bisa ditambahkan
di atasnya.

## Frontend & deploy (satu jar)

Frontend Angular ada di `src/main/frontend-realisasi`. `mvn clean package` otomatis menjalankan
`npm ci` dan `npm run build` (exec-maven-plugin), hasilnya ditulis ke `src/main/resources/static`
(base href `/satria/`) lalu ikut masuk jar. Folder `static` tidak di-commit.

Syarat mesin build (Jenkins/laptop): `node` (>= 18.19) dan `npm` ada di PATH, serta bisa mengakses
registry npm. Untuk build backend saja tanpa Node: `mvn package -Dfrontend.skip=true`.

Di server aplikasi berjalan di port 8040 di belakang nginx pada path `/satria/`:

```nginx
location = /satria { return 301 /satria/; }
location ^~ /satria/ {
    proxy_pass http://127.0.0.1:8040/;
    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;
    proxy_set_header X-Forwarded-Host   $host;
    proxy_set_header X-Forwarded-Prefix /satria;
    client_max_body_size 10m;
}
```

Untuk development tetap `npm start` (http://localhost:4200, base href `/`, `/api` di-proxy ke backend :8040).
