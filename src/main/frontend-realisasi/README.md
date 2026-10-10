# SATRIA – Sarinah Tracking Realisasi dan Informasi Anggaran

Frontend Angular untuk backend `backend-realisasi`.

## Login
Halaman `/login` memanggil `POST /api/auth/login` (username + password Active Directory). Token disimpan di browser
(`localStorage` bila "Ingat saya", selain itu `sessionStorage`) dan dikirim sebagai `Authorization: Bearer` oleh
`src/app/core/auth.interceptor.ts`. Jawaban 401 dari API (sesi habis) mengembalikan user ke halaman login.
Di backend profil dev, username apa pun bisa login dengan password `dev`.

Foto latar login: `public/sarinah-building.jpg` (ganti dengan foto resolusi tinggi bila ada).

## Catatan lama

Frontend Angular 19 (standalone components) untuk backend Spring Boot `backend-realisasi`.

## Fitur
- **Executive Dashboard (Group Overview)**: KPI RKA / serapan / belum terserap / usulan RKAP tahun berikutnya, serapan per divisi, komposisi OPEX vs CAPEX, status pekerjaan, progress RKA terendah, tabel serapan & usulan per entitas dan per divisi, top akun/program OPEX dan top CAPEX usulan.
- **Monitoring RKA**: filter perusahaan/divisi/tahun/tipe, grafik Planning · Pagu · Realisasi dan distribusi capaian, tambah/ubah/hapus program kerja (per divisi, COA dari master mata anggaran), template/import/export Excel (error import per baris ditampilkan), tombol ke daftar pekerjaan program.
- **CAPEX / OPEX**: filter perusahaan/divisi/tahun/tipe/PIC, beban kerja per PIC, progress pekerjaan, tambah/ubah/hapus pekerjaan, detail dengan tab Progress (update progress), Timeline (tambah/ubah/hapus milestone), Financial (catat/hapus realisasi).
- **Master Data**: perusahaan, divisi (level, induk, path, ID departemen Odoo, status) dan mata anggaran (COA), dengan filter & pencarian.
- Dropdown divisi untuk pengisian anggaran hanya menampilkan unit level `DIVISI` yang aktif.

## Menjalankan (development)
1. Jalankan backend Spring Boot di port 8080 (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`).
2. Di folder ini:

```bash
npm install
npm start
```

Buka `http://localhost:4200`. Request `/api` diteruskan ke `http://localhost:8080` lewat `proxy.conf.mjs`.
Proxy menghapus header `Origin`, karena backend hanya mengizinkan CORS dari `app.cors.allowed-origins`
(default `http://localhost:3000`) sehingga POST/PUT/DELETE dari port 4200 akan ditolak 403 tanpa itu.

## Production
```bash
npm run build   # hasil di dist/realisasi-angular-ui/browser
```
Request memakai path relatif `/api`, jadi paling mudah: sajikan hasil build dan reverse-proxy `/api` ke Spring Boot pada domain yang sama (contoh Nginx: `location /api { proxy_pass http://backend:8080; }` dan `try_files $uri /index.html;` untuk routing Angular).
Jika frontend dan backend beda domain, isi `app.cors.allowed-origins` di backend dengan domain frontend.
