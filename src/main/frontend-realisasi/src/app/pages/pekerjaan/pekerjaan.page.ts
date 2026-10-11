import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, Subscription, debounceTime, forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { clamp, errMsg, money, tgl, today, yearOptions } from '../../core/format';
import { PagerComponent } from '../../core/pager.component';
import { CapexOpex, Company, Divisi, Financial, Milestone, Pekerjaan, PekerjaanSummary, ProgramKerja, ProgressItem, StatusMilestone, TipeAnggaran } from '../../core/models';

@Component({selector:'app-pekerjaan',standalone:true,imports:[CommonModule,FormsModule,PagerComponent],template:`
<div class="page-head"><div><div class="eyebrow">EXECUTION</div><h1>CAPEX / OPEX</h1><p>Pantau pekerjaan, progress, timeline, serta realisasi keuangan.</p></div><button class="btn primary" (click)="openCreate()">＋ Tambah Pekerjaan</button></div>
<div class="filter-card">
  <div><label>Perusahaan</label><select [(ngModel)]="companyId" (change)="companyChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let c of companies" [ngValue]="c.id">{{c.nama}}</option></select></div>
  <div><label>Divisi</label><select [(ngModel)]="divisiId" (change)="filterChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let d of filteredDivisi" [ngValue]="d.id">{{d.nama}}</option></select></div>
  <div><label>Tahun</label><select [(ngModel)]="tahun" (change)="filterChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let y of years" [ngValue]="y">{{y}}</option></select></div>
  <div><label>Tipe</label><select [(ngModel)]="tipe" (change)="filterChanged()"><option [ngValue]="null">Semua</option><option value="OPEX">OPEX</option><option value="CAPEX">CAPEX</option></select></div>
  <div><label>PIC</label><select [(ngModel)]="pic" (change)="filterChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let p of picOptions" [ngValue]="p">{{p}}</option></select></div>
  <div class="filter-search"><label>Cari</label><input [(ngModel)]="q" (ngModelChange)="search$.next()" placeholder="Nama pekerjaan / PIC / COA"></div>
</div>
<div *ngIf="programFilter" class="alert info">Menampilkan pekerjaan untuk program <b>{{programFilter.noCoa}} · {{programFilter.namaMataAnggaran}}</b> ({{programFilter.divisiNama}} {{programFilter.tahun}}). <button class="link-btn" (click)="clearProgramFilter()">Tampilkan semua</button></div>
<div *ngIf="error" class="alert danger">{{error}}</div><div *ngIf="message" class="alert success">{{message}}</div>
<section class="status-summary"><div><span class="status-dot green"></span><small>ON TRACK</small><strong>{{summary?.onTrack ?? 0}}</strong></div><div><span class="status-dot red"></span><small>TERLAMBAT</small><strong>{{summary?.terlambat ?? 0}}</strong></div><div><span class="status-dot blue"></span><small>SELESAI</small><strong>{{summary?.selesai ?? 0}}</strong></div><div><span class="status-dot purple"></span><small>TOTAL ANGGARAN</small><strong>{{money(summary?.totalAnggaran)}}</strong></div></section>

<section class="dashboard-grid equal" *ngIf="chart as c">
  <article class="panel">
    <div class="panel-head"><div><h3>Beban Kerja per PIC</h3><p>Jumlah pekerjaan per status</p></div>
      <div class="chart-legend"><span><i class="sw ontrack"></i>On track</span><span><i class="sw late"></i>Terlambat</span><span><i class="sw done"></i>Selesai</span></div></div>
    <div class="stack-list" *ngIf="c.bebanKerjaPerPic.length; else noPic">
      <div *ngFor="let b of c.bebanKerjaPerPic.slice(0,8)"><span class="stack-name">{{b.pic}}</span>
        <div class="stack-bar"><i class="ontrack" [style.width.%]="picPct(b.onTrack,c)"></i><i class="late" [style.width.%]="picPct(b.terlambat,c)"></i><i class="done" [style.width.%]="picPct(b.selesai,c)"></i></div>
        <b>{{b.onTrack+b.terlambat+b.selesai}}</b></div>
    </div><ng-template #noPic><div class="empty">Belum ada pekerjaan.</div></ng-template>
  </article>
  <article class="panel">
    <div class="panel-head"><div><h3>Progress Pekerjaan</h3><p>Progress terakhir yang dilaporkan</p></div></div>
    <div class="mini-table flush" *ngIf="c.progressPekerjaan.length; else noProg"><div class="mini-row" *ngFor="let p of c.progressPekerjaan.slice(0,8)"><div><b>{{p.nama}}</b><small>{{p.pic}} · {{labelStatus(p.status)}}</small></div><div class="mini-progress"><span>{{p.progress}}%</span><div class="bar-bg"><i [class.late]="p.status==='TERLAMBAT'" [style.width.%]="clamp(p.progress)"></i></div></div></div></div>
    <ng-template #noProg><div class="empty">Belum ada pekerjaan.</div></ng-template>
  </article>
</section>

<article class="panel table-panel"><div class="panel-head"><div><h3>Daftar Pekerjaan</h3><p>{{total}} pekerjaan</p></div></div><div class="table-scroll"><table><thead><tr><th>Pekerjaan</th><th>PIC</th><th>Divisi</th><th>Periode</th><th>Anggaran</th><th>Realisasi</th><th>Progress</th><th>Status</th><th>Estimasi Selesai</th><th></th></tr></thead><tbody>
<tr *ngFor="let p of rows"><td><b>{{p.nama}}</b><small class="cell-sub">{{p.noCoa}} · {{p.namaMataAnggaran}} · <span class="badge" [class.capex]="p.tipe==='CAPEX'">{{p.tipe}}</span></small></td><td>{{p.pic}}</td><td>{{p.divisiNama}}<small class="cell-sub">{{p.companyNama}}</small></td><td>{{p.tahun}}<small class="cell-sub">{{p.jenisPeriode==='MULTI_YEAR'?'Multi year':'Single year'}}</small></td><td>{{money(p.anggaran)}}</td><td>{{money(p.realisasi)}}<small class="cell-sub" [class.neg]="p.sisa<0">sisa {{money(p.sisa)}}</small></td><td><div class="progress-cell"><span>{{p.progress}}%</span><div class="bar-bg"><i [style.width.%]="clamp(p.progress)"></i></div></div></td><td><span class="badge" [class.danger]="p.status==='TERLAMBAT'" [class.success]="p.status==='SELESAI'">{{labelStatus(p.status)}}</span></td><td>{{tgl(p.estimasiSelesai)}}</td>
<td class="row-actions"><button class="table-action" (click)="openDetail(p)">Detail</button><button class="table-action" (click)="openEdit(p)">Edit</button><button class="table-action danger" (click)="remove(p)">Hapus</button></td></tr>
<tr *ngIf="!rows.length"><td colspan="10"><div class="empty">Belum ada pekerjaan.</div></td></tr></tbody></table></div>
<app-pager [page]="page" [size]="size" [total]="total" (pageChange)="page=$event;loadRows()" (sizeChange)="size=$event;filterChanged()"></app-pager></article>

<div class="drawer-backdrop" *ngIf="selected" (click)="closeDetail()"></div>
<aside class="drawer" *ngIf="selected as s">
  <div class="drawer-head"><div><small>{{s.tipe}} · {{s.noCoa}} · {{labelStatus(s.status)}}</small><h2>{{s.nama}}</h2><p>{{s.divisiNama}} · PIC {{s.pic}}<span *ngIf="s.tanggalMulai || s.estimasiSelesai"> · {{tgl(s.tanggalMulai)}} – {{tgl(s.estimasiSelesai)}}</span></p></div><button class="icon-btn" (click)="closeDetail()">×</button></div>
  <div class="drawer-kpi"><div><span>Anggaran</span><b>{{money(s.anggaran)}}</b></div><div><span>Realisasi</span><b>{{money(s.realisasi)}}</b></div><div><span>Progress</span><b>{{s.progress}}%</b></div></div>
  <div *ngIf="detailError" class="alert danger drawer-alert">{{detailError}}</div>
  <div class="tabs"><button [class.active]="tab==='progress'" (click)="tab='progress'">Progress</button><button [class.active]="tab==='timeline'" (click)="tab='timeline'">Timeline</button><button [class.active]="tab==='financial'" (click)="tab='financial'">Financial</button></div>

  <div class="drawer-body" *ngIf="tab==='progress'">
    <div class="quick-form"><h4>Update Progress</h4>
      <div class="form-grid tight"><div><label>Tanggal</label><input type="date" [(ngModel)]="progressForm.tanggal"></div><div><label>Progress (%) *</label><input type="number" min="0" max="100" [(ngModel)]="progressForm.persen"></div>
        <div class="span-2"><label>Catatan</label><input [(ngModel)]="progressForm.catatan" placeholder="Apa yang sudah dikerjakan"></div>
        <div><label>Tindak Lanjut</label><input [(ngModel)]="progressForm.tindakLanjut"></div><div><label>Dilaporkan Oleh</label><input [(ngModel)]="progressForm.dilaporkanOleh"></div></div>
      <div class="quick-actions"><button class="btn primary" [disabled]="busy" (click)="addProgress()">Simpan Progress</button></div></div>
    <div class="timeline-list"><div *ngFor="let x of progress"><span class="timeline-pin"></span><div><b>{{x.persen}}% · {{tgl(x.tanggal)}}</b><p>{{x.catatan || 'Tidak ada catatan'}}</p><small><span *ngIf="x.tindakLanjut">Tindak lanjut: {{x.tindakLanjut}}</span><span *ngIf="x.tindakLanjut && x.dilaporkanOleh"> · </span><span *ngIf="x.dilaporkanOleh">oleh {{x.dilaporkanOleh}}</span></small></div></div><div *ngIf="!progress.length" class="empty">Belum ada riwayat progress.</div></div>
  </div>

  <div class="drawer-body" *ngIf="tab==='timeline'">
    <div class="quick-form"><h4>{{milestoneEdit?'Ubah Milestone':'Tambah Milestone'}}</h4>
      <div class="form-grid tight"><div class="span-2"><label>Nama *</label><input [(ngModel)]="milestoneForm.nama" placeholder="Contoh: Pengadaan vendor"></div>
        <div><label>Mulai</label><input type="date" [(ngModel)]="milestoneForm.tanggalMulai"></div><div><label>Selesai *</label><input type="date" [(ngModel)]="milestoneForm.tanggalSelesai"></div>
        <div><label>Status</label><select [(ngModel)]="milestoneForm.status"><option *ngFor="let st of milestoneStatus" [value]="st">{{labelMilestone(st)}}</option></select></div><div><label>Catatan</label><input [(ngModel)]="milestoneForm.catatan"></div></div>
      <div class="quick-actions"><button class="btn ghost" *ngIf="milestoneEdit" (click)="resetMilestone()">Batal</button><button class="btn primary" [disabled]="busy" (click)="saveMilestone()">Simpan Milestone</button></div></div>
    <div class="timeline-list"><div *ngFor="let m of milestones"><span class="timeline-pin" [class.late]="m.terlambat" [class.done]="m.status==='SELESAI'"></span><div class="tl-body"><div><b>{{m.nama}}</b> <span class="badge" [class.success]="m.status==='SELESAI'" [class.danger]="m.terlambat">{{m.terlambat?'TERLAMBAT':labelMilestone(m.status)}}</span><p>{{tgl(m.tanggalMulai)}} → {{tgl(m.tanggalSelesai)}}</p><small>{{m.catatan || ''}}</small></div>
      <div class="tl-actions"><button class="table-action" (click)="editMilestone(m)">Edit</button><button class="table-action danger" (click)="removeMilestone(m)">Hapus</button></div></div></div><div *ngIf="!milestones.length" class="empty">Belum ada milestone.</div></div>
  </div>

  <div class="drawer-body" *ngIf="tab==='financial'"><ng-container *ngIf="financial as f">
    <div class="finance-hero"><div><span>Serapan</span><strong>{{f.serapanPersen | number:'1.1-1'}}%</strong></div><div class="bar-bg"><i [style.width.%]="clamp(f.serapanPersen)"></i></div><small>{{money(f.totalRealisasi)}} dari {{money(f.anggaran)}} · sisa {{money(f.sisa)}}</small></div>
    <div class="quick-form"><h4>Catat Realisasi</h4>
      <div class="form-grid tight"><div><label>Tanggal *</label><input type="date" [(ngModel)]="realisasiForm.tanggal"></div><div><label>Nilai (Rp) *</label><input type="number" min="0" [(ngModel)]="realisasiForm.nilai"><small class="hint">{{money(realisasiForm.nilai)}}</small></div>
        <div><label>No Dokumen</label><input [(ngModel)]="realisasiForm.noDokumen" placeholder="No SPP / invoice"></div><div><label>Keterangan</label><input [(ngModel)]="realisasiForm.keterangan"></div></div>
      <div class="quick-actions"><button class="btn primary" [disabled]="busy" (click)="addRealisasi()">Simpan Realisasi</button></div></div>
    <div class="transaction-list"><div *ngFor="let t of f.transaksi"><div><b>{{t.noDokumen || 'Realisasi'}}</b><small>{{tgl(t.tanggal)}} · {{t.keterangan || '-'}}</small></div><div class="tx-right"><strong>{{money(t.nilai)}}</strong><button class="table-action danger" (click)="removeRealisasi(t.id)">Hapus</button></div></div><div *ngIf="!f.transaksi.length" class="empty">Belum ada transaksi realisasi.</div></div>
  </ng-container></div>
</aside>

<div class="modal-backdrop" *ngIf="modal" (click)="modal=false"></div>
<section class="modal" *ngIf="modal"><div class="modal-head"><div><small>{{editing?'EDIT PEKERJAAN':'PEKERJAAN BARU'}}</small><h2>{{editing?'Ubah Pekerjaan':'Tambah Pekerjaan'}}</h2></div><button class="icon-btn" (click)="modal=false">×</button></div>
  <div *ngIf="formError" class="alert danger form-alert">{{formError}}</div>
  <div class="form-grid">
    <div class="span-2"><label>Program Kerja (RKA) *</label><select [(ngModel)]="form.programKerjaId" (change)="programChanged()"><option [ngValue]="null">Pilih program</option><option *ngFor="let r of programOptions" [ngValue]="r.id">{{r.tahun}} · {{r.divisiNama}} · {{r.noCoa}} {{r.namaMataAnggaran}}{{r.status==='CLOSED'?' (CLOSED)':''}}</option></select>
      <small class="hint" *ngIf="selectedProgram as sp">{{sp.tipe}} · plafon {{money(sp.pagu ?? sp.planningBudget)}} · sisa {{money(sp.sisa)}}</small></div>
    <div class="span-2"><label>Nama Pekerjaan *</label><input [(ngModel)]="form.nama"></div>
    <div><label>PIC *</label><input [(ngModel)]="form.pic" list="pic-list"><datalist id="pic-list"><option *ngFor="let p of picOptions" [value]="p"></option></datalist></div>
    <div><label>Anggaran (Rp) *</label><input type="number" min="0" [(ngModel)]="form.anggaran"><small class="hint">{{money(form.anggaran)}}</small></div>
    <div><label>Tanggal Mulai</label><input type="date" [(ngModel)]="form.tanggalMulai"></div>
    <div><label>Estimasi Selesai</label><input type="date" [(ngModel)]="form.estimasiSelesai"></div>
    <div><label>Periode</label><select [(ngModel)]="form.jenisPeriode"><option value="SINGLE_YEAR">Single Year</option><option value="MULTI_YEAR">Multi Year</option></select></div>
    <div><label>Tahun</label><select [(ngModel)]="form.tahun"><option *ngFor="let y of years" [ngValue]="y">{{y}}</option></select></div>
    <div class="span-2"><label>Deskripsi</label><textarea rows="3" [(ngModel)]="form.deskripsi"></textarea></div>
    <div class="span-2"><label>Tindak Lanjut</label><textarea rows="2" [(ngModel)]="form.tindakLanjut"></textarea></div>
  </div>
  <div class="modal-actions"><button class="btn ghost" (click)="modal=false">Batal</button><button class="btn primary" [disabled]="saving" (click)="savePekerjaan()">{{saving?'Menyimpan…':'Simpan Pekerjaan'}}</button></div></section>
`})
export class PekerjaanPage implements OnInit, OnDestroy {
  rows:Pekerjaan[]=[];companies:Company[]=[];divisi:Divisi[]=[];rka:ProgramKerja[]=[];chart?:CapexOpex;
  progress:ProgressItem[]=[];milestones:Milestone[]=[];financial?:Financial;selected?:Pekerjaan;tab:'progress'|'timeline'|'financial'='progress';
  companyId:number|null=null;divisiId:number|null=null;tahun:number|null=new Date().getFullYear();tipe:TipeAnggaran|null=null;pic:string|null=null;programKerjaId:number|null=null;q='';
  years=yearOptions();picOptions:string[]=[];error='';message='';detailError='';formError='';modal=false;saving=false;busy=false;editing?:Pekerjaan;
  milestoneStatus:StatusMilestone[]=['BELUM_MULAI','BERJALAN','SELESAI'];milestoneEdit?:Milestone;
  form:any={};progressForm:any={};realisasiForm:any={};milestoneForm:any={};
  /** Tabel per halaman dari server; summary = jumlah status dan total anggaran atas seluruh data yang cocok. */
  page=0;size=20;total=0;summary?:PekerjaanSummary;
  readonly search$=new Subject<void>();private subs=new Subscription();private rowsSub?:Subscription;
  readonly money=money;readonly clamp=clamp;readonly tgl=tgl;
  constructor(private api:ApiService,private route:ActivatedRoute,private router:Router){}

  ngOnDestroy(){this.subs.unsubscribe();this.rowsSub?.unsubscribe();}
  ngOnInit(){
    this.subs.add(this.search$.pipe(debounceTime(300)).subscribe(()=>this.filterChanged()));
    this.loadPic();
    const pk=this.route.snapshot.queryParamMap.get('programKerjaId');
    if(pk){this.programKerjaId=Number(pk);this.tahun=null;}
    forkJoin([this.api.companies(),this.api.divisiAnggaran(),this.api.rka()]).subscribe({next:([c,d,r])=>{this.companies=c;this.divisi=d;this.rka=r;this.load();},error:e=>this.error=errMsg(e,'Master data gagal dimuat.')});
  }
  get filteredDivisi(){return this.companyId?this.divisi.filter(d=>d.companyId===this.companyId):this.divisi;}
  get programFilter(){return this.programKerjaId?this.rka.find(r=>r.id===this.programKerjaId):undefined;}
  /** Program untuk form: yang OPEN, plus program milik pekerjaan yang sedang diedit. */
  get programOptions(){return this.rka.filter(r=>r.status==='OPEN'||r.id===this.form.programKerjaId);}
  get selectedProgram(){return this.rka.find(r=>r.id===this.form.programKerjaId);}
  private get filters(){return {companyId:this.companyId,divisiId:this.divisiId,tahun:this.tahun,tipe:this.tipe,pic:this.pic,programKerjaId:this.programKerjaId};}
  companyChanged(){this.divisiId=null;this.filterChanged();}
  clearProgramFilter(){this.programKerjaId=null;this.tahun=new Date().getFullYear();this.router.navigate([],{queryParams:{}});this.filterChanged();}
  filterChanged(){this.page=0;this.load();}
  private loadPic(){this.api.pekerjaanPic().subscribe({next:p=>this.picOptions=p,error:()=>{}});}
  /** Muat ulang halaman aktif tabel tanpa grafik. */
  loadRows(){
    this.rowsSub?.unsubscribe();
    this.rowsSub=this.api.pekerjaanPage({...this.filters,q:this.q,page:this.page,size:this.size}).subscribe({next:r=>{
      this.rows=r.items;this.total=r.total;this.summary=r.summary;
      if(this.page>0&&this.page>=r.totalPages){this.page=Math.max(0,r.totalPages-1);this.loadRows();}
    },error:e=>this.error=errMsg(e,'Data pekerjaan gagal dimuat.')});
  }
  load(){
    this.loadRows();
    const {companyId,divisiId,tahun,tipe}=this.filters;
    this.api.capexOpex({companyId,divisiId,tahun,tipe}).subscribe({next:c=>this.chart=c,error:()=>this.chart=undefined});
  }
  picPct(n:number,c:CapexOpex){const max=Math.max(1,...c.bebanKerjaPerPic.map(b=>b.onTrack+b.terlambat+b.selesai));return n/max*100;}
  labelStatus(s:string){return s==='ON_TRACK'?'ON TRACK':s;}
  labelMilestone(s:string){return s==='BELUM_MULAI'?'Belum mulai':s==='BERJALAN'?'Berjalan':'Selesai';}

  // ---- Tambah / ubah pekerjaan ----
  openCreate(){this.editing=undefined;this.formError='';this.form={programKerjaId:this.programKerjaId,nama:'',deskripsi:'',pic:'',tahun:this.tahun||new Date().getFullYear(),jenisPeriode:'SINGLE_YEAR',anggaran:0,tanggalMulai:null,estimasiSelesai:null,tindakLanjut:''};this.programChanged();this.modal=true;}
  openEdit(p:Pekerjaan){this.editing=p;this.formError='';this.form={programKerjaId:p.programKerjaId,nama:p.nama,deskripsi:p.deskripsi||'',pic:p.pic,tahun:p.tahun,jenisPeriode:p.jenisPeriode,anggaran:p.anggaran,tanggalMulai:p.tanggalMulai||null,estimasiSelesai:p.estimasiSelesai||null,tindakLanjut:p.tindakLanjut||''};this.modal=true;}
  programChanged(){const r=this.selectedProgram;if(r&&!this.editing)this.form.tahun=r.tahun;}
  savePekerjaan(){
    const f=this.form;this.formError='';
    if(!f.programKerjaId||!f.nama?.trim()||!f.pic?.trim()){this.formError='Program kerja, nama pekerjaan, dan PIC wajib diisi.';return;}
    if(f.anggaran===null||f.anggaran===''||Number(f.anggaran)<0){this.formError='Anggaran wajib diisi dan tidak boleh negatif.';return;}
    if(f.tanggalMulai&&f.estimasiSelesai&&f.estimasiSelesai<f.tanggalMulai){this.formError='Estimasi selesai tidak boleh sebelum tanggal mulai.';return;}
    const body={...f,nama:f.nama.trim(),pic:f.pic.trim(),anggaran:Number(f.anggaran),tanggalMulai:f.tanggalMulai||null,estimasiSelesai:f.estimasiSelesai||null,deskripsi:f.deskripsi||null,tindakLanjut:f.tindakLanjut||null};
    this.saving=true;
    const req=this.editing?this.api.updatePekerjaan(this.editing.id,body):this.api.createPekerjaan(body);
    req.subscribe({next:saved=>{this.saving=false;this.modal=false;this.message=this.editing?'Pekerjaan berhasil diubah.':'Pekerjaan berhasil ditambahkan.';this.loadPic();if(this.selected?.id===saved.id)this.selected=saved;this.load();},error:e=>{this.saving=false;this.formError=errMsg(e,'Gagal menyimpan pekerjaan.');}});
  }
  remove(p:Pekerjaan){
    if(!confirm(`Hapus pekerjaan "${p.nama}"? Progress, timeline, dan realisasinya ikut terhapus.`))return;
    this.api.deletePekerjaan(p.id).subscribe({next:()=>{this.message='Pekerjaan dihapus.';if(this.selected?.id===p.id)this.closeDetail();this.load();},error:e=>this.error=errMsg(e,'Gagal menghapus pekerjaan.')});
  }

  // ---- Detail drawer ----
  openDetail(p:Pekerjaan){
    this.selected=p;this.tab='progress';this.detailError='';this.progress=[];this.milestones=[];this.financial=undefined;
    this.progressForm={tanggal:today(),persen:p.progress,catatan:'',tindakLanjut:'',dilaporkanOleh:''};
    this.realisasiForm={tanggal:today(),nilai:null,noDokumen:'',keterangan:''};
    this.resetMilestone();
    forkJoin([this.api.progress(p.id),this.api.timeline(p.id),this.api.financial(p.id)]).subscribe({next:([pr,mi,fi])=>{this.progress=pr;this.milestones=mi;this.financial=fi;},error:e=>this.detailError=errMsg(e,'Detail pekerjaan gagal dimuat.')});
  }
  closeDetail(){this.selected=undefined;}
  /** Muat ulang header drawer (progress/realisasi/status) dan tabel setelah ada perubahan. */
  private refreshSelected(){const id=this.selected?.id;if(!id)return;this.api.getPekerjaan(id).subscribe(p=>{if(this.selected?.id===id)this.selected=p;});this.load();}
  addProgress(){
    const s=this.selected;if(!s)return;const f=this.progressForm;this.detailError='';
    const persen=Number(f.persen);if(f.persen===null||f.persen===''||persen<0||persen>100){this.detailError='Progress harus 0–100.';return;}
    this.busy=true;
    this.api.addProgress(s.id,{...f,persen,tanggal:f.tanggal||null}).subscribe({next:()=>{this.busy=false;this.api.progress(s.id).subscribe(x=>this.progress=x);this.progressForm={...f,catatan:'',tindakLanjut:''};this.refreshSelected();},error:e=>{this.busy=false;this.detailError=errMsg(e,'Gagal update progress.');}});
  }
  resetMilestone(){this.milestoneEdit=undefined;this.milestoneForm={nama:'',tanggalMulai:null,tanggalSelesai:null,status:'BELUM_MULAI',catatan:''};}
  editMilestone(m:Milestone){this.milestoneEdit=m;this.milestoneForm={nama:m.nama,tanggalMulai:m.tanggalMulai||null,tanggalSelesai:m.tanggalSelesai,status:m.status,catatan:m.catatan||''};}
  saveMilestone(){
    const s=this.selected;if(!s)return;const f=this.milestoneForm;this.detailError='';
    if(!f.nama?.trim()||!f.tanggalSelesai){this.detailError='Nama dan tanggal selesai milestone wajib diisi.';return;}
    if(f.tanggalMulai&&f.tanggalSelesai<f.tanggalMulai){this.detailError='Tanggal selesai tidak boleh sebelum tanggal mulai.';return;}
    const body={...f,nama:f.nama.trim(),tanggalMulai:f.tanggalMulai||null,catatan:f.catatan||null};
    this.busy=true;
    const req=this.milestoneEdit?this.api.updateMilestone(s.id,this.milestoneEdit.id,body):this.api.addMilestone(s.id,body);
    req.subscribe({next:()=>{this.busy=false;this.resetMilestone();this.api.timeline(s.id).subscribe(x=>this.milestones=x);this.refreshSelected();},error:e=>{this.busy=false;this.detailError=errMsg(e,'Gagal menyimpan milestone.');}});
  }
  removeMilestone(m:Milestone){
    const s=this.selected;if(!s||!confirm(`Hapus milestone "${m.nama}"?`))return;
    this.api.deleteMilestone(s.id,m.id).subscribe({next:()=>{this.api.timeline(s.id).subscribe(x=>this.milestones=x);this.refreshSelected();},error:e=>this.detailError=errMsg(e,'Gagal menghapus milestone.')});
  }
  addRealisasi(){
    const s=this.selected;if(!s)return;const f=this.realisasiForm;this.detailError='';
    if(!f.tanggal||!(Number(f.nilai)>0)){this.detailError='Tanggal dan nilai realisasi (> 0) wajib diisi.';return;}
    this.busy=true;
    this.api.addRealisasi(s.id,{...f,nilai:Number(f.nilai),noDokumen:f.noDokumen||null,keterangan:f.keterangan||null}).subscribe({next:()=>{this.busy=false;this.realisasiForm={tanggal:f.tanggal,nilai:null,noDokumen:'',keterangan:''};this.api.financial(s.id).subscribe(x=>this.financial=x);this.refreshSelected();},error:e=>{this.busy=false;this.detailError=errMsg(e,'Gagal mencatat realisasi.');}});
  }
  removeRealisasi(id:number){
    const s=this.selected;if(!s||!confirm('Hapus transaksi realisasi ini?'))return;
    this.api.deleteRealisasi(s.id,id).subscribe({next:()=>{this.api.financial(s.id).subscribe(x=>this.financial=x);this.refreshSelected();},error:e=>this.detailError=errMsg(e,'Gagal menghapus realisasi.')});
  }
}
