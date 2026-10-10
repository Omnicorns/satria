import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LdapDepartemenGroup, LdapSyncResult } from './models';
import { CapexOpex, Company, Divisi, Financial, GroupOverview, ImportResult, LevelOrganisasi, MataAnggaran, Milestone, MonitoringRka, Pekerjaan, ProgramKerja, ProgressItem, RealisasiItem, TipeAnggaran } from './models';

export interface Filters { companyId?:number|null; divisiId?:number|null; tahun?:number|null; tipe?:TipeAnggaran|null; pic?:string|null; programKerjaId?:number|null; }
export interface DivisiFilter { companyId?:number|null; level?:LevelOrganisasi|null; aktif?:boolean|null; }

@Injectable({providedIn:'root'})
export class ApiService {
  private readonly base = '/api';
  constructor(private http:HttpClient) {}

  private params(filters:object = {}): HttpParams {
    let p = new HttpParams();
    Object.entries(filters).forEach(([key,val]) => { if (val !== null && val !== undefined && val !== '') p = p.set(key, String(val)); });
    return p;
  }

  companies():Observable<Company[]> { return this.http.get<Company[]>(`${this.base}/companies`); }
  createCompany(body:any){ return this.http.post<Company>(`${this.base}/companies`, body); }
  updateCompany(id:number,body:any){ return this.http.put<Company>(`${this.base}/companies/${id}`, body); }

  /** Tanpa filter = semua unit organisasi. Untuk pengisian anggaran pakai {level:'DIVISI', aktif:true}. */
  divisi(filter:DivisiFilter = {}):Observable<Divisi[]> { return this.http.get<Divisi[]>(`${this.base}/divisi`, {params:this.params(filter)}); }
  divisiAnggaran():Observable<Divisi[]> { return this.divisi({level:'DIVISI', aktif:true}); }
  createDivisi(body:any){ return this.http.post<Divisi>(`${this.base}/divisi`, body); }
  updateDivisi(id:number,body:any){ return this.http.put<Divisi>(`${this.base}/divisi/${id}`, body); }

  coa(tipe?:TipeAnggaran|null):Observable<MataAnggaran[]> { return this.http.get<MataAnggaran[]>(`${this.base}/mata-anggaran`, {params:this.params({tipe})}); }
  createCoa(body:any){ return this.http.post<MataAnggaran>(`${this.base}/mata-anggaran`, body); }
  updateCoa(id:number,body:any){ return this.http.put<MataAnggaran>(`${this.base}/mata-anggaran/${id}`, body); }

  rka(filters:Filters={}):Observable<ProgramKerja[]> { return this.http.get<ProgramKerja[]>(`${this.base}/rka`, {params:this.params(filters)}); }
  createRka(body:any){ return this.http.post<ProgramKerja>(`${this.base}/rka`, body); }
  updateRka(id:number,body:any){ return this.http.put<ProgramKerja>(`${this.base}/rka/${id}`, body); }
  deleteRka(id:number){ return this.http.delete<void>(`${this.base}/rka/${id}`); }
  templateRka(){ return this.http.get(`${this.base}/rka/excel/template`, {responseType:'blob'}); }
  exportRka(filters:Filters={}){ return this.http.get(`${this.base}/rka/excel/export`, {params:this.params(filters), responseType:'blob'}); }
  importRka(file:File){ const fd=new FormData(); fd.append('file',file); return this.http.post<ImportResult>(`${this.base}/rka/excel/import`,fd); }

  pekerjaan(filters:Filters={}):Observable<Pekerjaan[]> { return this.http.get<Pekerjaan[]>(`${this.base}/pekerjaan`, {params:this.params(filters)}); }
  getPekerjaan(id:number):Observable<Pekerjaan> { return this.http.get<Pekerjaan>(`${this.base}/pekerjaan/${id}`); }
  createPekerjaan(body:any){ return this.http.post<Pekerjaan>(`${this.base}/pekerjaan`,body); }
  updatePekerjaan(id:number,body:any){ return this.http.put<Pekerjaan>(`${this.base}/pekerjaan/${id}`,body); }
  deletePekerjaan(id:number){ return this.http.delete<void>(`${this.base}/pekerjaan/${id}`); }

  progress(id:number):Observable<ProgressItem[]>{ return this.http.get<ProgressItem[]>(`${this.base}/pekerjaan/${id}/progress`); }
  addProgress(id:number,body:any){ return this.http.post<ProgressItem>(`${this.base}/pekerjaan/${id}/progress`,body); }
  timeline(id:number):Observable<Milestone[]>{ return this.http.get<Milestone[]>(`${this.base}/pekerjaan/${id}/timeline`); }
  addMilestone(id:number,body:any){ return this.http.post<Milestone>(`${this.base}/pekerjaan/${id}/timeline`,body); }
  updateMilestone(id:number,milestoneId:number,body:any){ return this.http.put<Milestone>(`${this.base}/pekerjaan/${id}/timeline/${milestoneId}`,body); }
  deleteMilestone(id:number,milestoneId:number){ return this.http.delete<void>(`${this.base}/pekerjaan/${id}/timeline/${milestoneId}`); }
  financial(id:number):Observable<Financial>{ return this.http.get<Financial>(`${this.base}/pekerjaan/${id}/financial`); }
  addRealisasi(id:number,body:any){ return this.http.post<RealisasiItem>(`${this.base}/pekerjaan/${id}/realisasi`,body); }
  deleteRealisasi(id:number,realisasiId:number){ return this.http.delete<void>(`${this.base}/pekerjaan/${id}/realisasi/${realisasiId}`); }

  monitoring(filters:Filters={}):Observable<MonitoringRka>{ return this.http.get<MonitoringRka>(`${this.base}/dashboard/monitoring-rka`,{params:this.params(filters)}); }
  capexOpex(filters:Filters={}):Observable<CapexOpex>{ return this.http.get<CapexOpex>(`${this.base}/dashboard/capex-opex`,{params:this.params(filters)}); }
  groupOverview(filters:Filters={}):Observable<GroupOverview>{ return this.http.get<GroupOverview>(`${this.base}/dashboard/group-overview`,{params:this.params(filters)}); }

  ldapSync(){ return this.http.post<LdapSyncResult>(`${this.base}/ldap/sync`,{}); }
  ldapDepartemen():Observable<LdapDepartemenGroup[]>{ return this.http.get<LdapDepartemenGroup[]>(`${this.base}/ldap/departemen`); }
  mapLdapDepartemen(id:number,divisiId:number|null){ return this.http.put<void>(`${this.base}/ldap/departemen/${id}/divisi`,{divisiId}); }
}
