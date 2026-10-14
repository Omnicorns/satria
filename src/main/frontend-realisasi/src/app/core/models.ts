export type TipeAnggaran = 'OPEX' | 'CAPEX';
export type StatusProgram = 'OPEN' | 'CLOSED';
export type StatusPekerjaan = 'ON_TRACK' | 'TERLAMBAT' | 'SELESAI';
export type StatusMilestone = 'BELUM_MULAI' | 'BERJALAN' | 'SELESAI';
export type LevelOrganisasi = 'HEAD_OFFICE' | 'DIREKTORAT' | 'DIVISI' | 'UNIT';
export type JenisPeriode = 'SINGLE_YEAR' | 'MULTI_YEAR';

/** Hasil list berhalaman dari backend (?page=...). page dimulai dari 0; summary = total atas seluruh data yang cocok. */
export interface Page<T, S = undefined> { items:T[]; total:number; page:number; size:number; totalPages:number; summary?:S; }
export interface PageQuery { q?:string|null; page:number; size:number; sort?:string|null; }
export interface RkaSummary { totalPlanning:number; totalPagu:number; totalRealisasi:number; totalSisa:number; }
export interface PekerjaanSummary { onTrack:number; terlambat:number; selesai:number; totalAnggaran:number; }

export interface Company { id:number; kode:string; nama:string; namaGrup?:string; logoUrl?:string; }
export interface Divisi {
  id:number; companyId:number; companyNama:string; kode:string; nama:string;
  parentId?:number|null; parentNama?:string|null; level:LevelOrganisasi; path?:string|null; odooDepartmentId?:number|null; aktif:boolean;
}
export interface MataAnggaran { id:number; noCoa:string; nama:string; tipe:TipeAnggaran; kategori?:string; aktif:boolean; }

export interface ProgramKerja {
  id:number; companyId:number; companyNama:string; divisiId:number; divisiKode:string; divisiNama:string;
  tahun:number; tipe:TipeAnggaran; mataAnggaranId:number; noCoa:string; namaMataAnggaran:string;
  planningBudget:number; pagu:number|null; realisasi:number; sisa:number; capaianPersen:number;
  status:StatusProgram; keterangan?:string; createdAt:string;
}

export interface Pekerjaan {
  id:number; programKerjaId:number; noCoa:string; namaMataAnggaran:string; companyId:number; companyNama:string;
  divisiId:number; divisiNama:string; nama:string; deskripsi?:string; pic:string; tahun:number; tipe:TipeAnggaran;
  jenisPeriode:JenisPeriode; anggaran:number; realisasi:number; sisa:number;
  tanggalMulai?:string; estimasiSelesai?:string; progress:number; status:StatusPekerjaan; tindakLanjut?:string;
}

export interface ProgressItem { id:number; tanggal:string; persen:number; catatan?:string; tindakLanjut?:string; dilaporkanOleh?:string; }
export interface Milestone { id:number; nama:string; tanggalMulai?:string; tanggalSelesai:string; status:StatusMilestone; catatan?:string; terlambat:boolean; }
export interface RealisasiItem { id:number; tanggal:string; periodeTahun:number; periodeBulan:number; nilai:number; noDokumen?:string; keterangan?:string; }
export interface Financial { pekerjaanId:number; anggaran:number; totalRealisasi:number; sisa:number; serapanPersen:number; transaksi:RealisasiItem[]; }
export interface ImportResult { totalBaris:number; dibuat:number; diperbarui:number; errors:string[]; }

export interface RkaItem { programKerjaId:number; noCoa:string; nama:string; divisiNama:string; planning:number; pagu:number|null; realisasi:number; capaianPersen:number; }
export interface MonitoringRka {
  totalPlanning:number; totalPagu:number; totalRealisasi:number;
  perMataAnggaran:RkaItem[];
  distribusi:{nol:number; satuSampai49:number; limaPuluhSampai99:number; seratusPlus:number};
  progressTerendah:RkaItem[];
  carryOver:CarryOver[];
}

/** Pekerjaan multi year dari tahun sebelumnya yang membebani RKA tahun yang dimonitor; programKerjaId = RKA asal pekerjaan. */
export interface CarryOver {
  pekerjaanId:number; programKerjaId:number; nama:string; pic:string; noCoa:string; namaMataAnggaran:string;
  divisiNama:string; tahunMulai:number; anggaran:number; realisasiSebelumnya:number; realisasiTahunIni:number;
  sisaKontrak:number; progress:number;
}

export interface CapexOpex {
  statusPekerjaan:{onTrack:number; terlambat:number; selesai:number};
  bebanKerjaPerPic:Array<{pic:string; onTrack:number; terlambat:number; selesai:number}>;
  progressPekerjaan:Array<{pekerjaanId:number; nama:string; pic:string; progress:number; status:string}>;
}

export interface TopItem { noCoa:string; nama:string; companyNama:string; nilai:number; }
export interface GroupOverview {
  tahun:number; tahunUsulan:number;
  kpi:{rka:number; rkaCapex:number; rkaOpex:number; serapan:number; serapanPersen:number; belumTerserap:number; belumTerserapPersen:number; usulanRkap:number};
  perEntitas:OverviewRow[]; perDivisi:OverviewRow[];
  opexVsCapex:Record<string,{realisasi:number; anggaran:number; persen:number}>;
  topAkunOpexUsulan:TopItem[];
  topProgramOpexUsulan:TopItem[];
  topCapexUsulan:TopItem[];
}
export interface OverviewRow { id:number; kode:string; nama:string; induk?:string; rka:number; serapan:number; persenOpex?:number|null; persenCapex?:number|null; persenTotal:number; usulan:number; usulanVsSerapan?:number|null; }

/** Hasil sync user + departemen dari Active Directory. */
export interface LdapSyncResult { totalUserAd:number; userBaru:number; userDiperbarui:number; totalDepartemen:number; departemenBaru:number; userTidakAdaDiAd:number; waktu:string; }
export interface LdapUserRow { id:number; username:string; nama:string|null; email:string|null; jabatan:string|null; aktif:boolean; lastLoginAt:string|null; }
/** id null = grup "(Tanpa departemen)". */
export interface LdapDepartemenGroup { id:number|null; nama:string; divisiId:number|null; divisiNama:string|null; jumlahUser:number; lastSyncAt:string|null; users:LdapUserRow[]; }
