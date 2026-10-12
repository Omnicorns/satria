import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { downloadBlob, errMsg } from '../../core/format';
import { PagerComponent } from '../../core/pager.component';
import { Company, Divisi, LevelOrganisasi, MataAnggaran, TipeAnggaran } from '../../core/models';

@Component({selector:'app-master',standalone:true,imports:[CommonModule,FormsModule,PagerComponent],template:`
<div class="page-head"><div><div class="eyebrow">CONFIGURATION</div><h1>Master Data</h1><p>Kelola perusahaan, divisi, dan chart of accounts yang dipakai pada RKA.</p></div><div class="head-actions"><ng-container *ngIf="tab==='coa'"><button class="btn secondary" (click)="downloadTemplateCoa()">↓ Template Excel</button><button class="btn secondary" (click)="exportCoa()">⇩ Export</button><input #coaFile type="file" accept=".xlsx" hidden (change)="importCoa($event)"><button class="btn secondary" [disabled]="importing" (click)="coaFile.click()">{{importing?'Mengimpor…':'↑ Import Excel'}}</button></ng-container><button class="btn primary" (click)="openAdd()">＋ Tambah {{tabLabel}}</button></div></div>
<div class="segmented"><button [class.active]="tab==='company'" (click)="setTab('company')">Perusahaan <span>{{companies.length}}</span></button><button [class.active]="tab==='divisi'" (click)="setTab('divisi')">Divisi <span>{{divisi.length}}</span></button><button [class.active]="tab==='coa'" (click)="setTab('coa')">Mata Anggaran <span>{{coaCount}}</span></button></div>
<div *ngIf="error" class="alert danger">{{error}}<ul *ngIf="importErrors.length" class="error-list"><li *ngFor="let e of importErrors">{{e}}</li></ul></div><div *ngIf="message" class="alert success">{{message}}</div>

<article class="panel table-panel" *ngIf="tab==='company'"><div class="panel-head"><div><h3>Perusahaan / Entitas</h3><p>Daftar entitas di dalam group</p></div></div><div class="table-scroll"><table><thead><tr><th>Kode</th><th>Nama</th><th>Group</th><th>Divisi</th><th></th></tr></thead><tbody><tr *ngFor="let x of companies"><td><b>{{x.kode}}</b></td><td>{{x.nama}}</td><td>{{x.namaGrup || '-'}}</td><td>{{countDivisi(x.id)}}</td><td><button class="table-action" (click)="editCompany(x)">Edit</button></td></tr><tr *ngIf="!companies.length"><td colspan="5"><div class="empty">Belum ada perusahaan.</div></td></tr></tbody></table></div></article>

<ng-container *ngIf="tab==='divisi'">
  <div class="filter-card">
    <div><label>Perusahaan</label><select [(ngModel)]="fCompany" (ngModelChange)="filterChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let c of companies" [ngValue]="c.id">{{c.nama}}</option></select></div>
    <div><label>Level</label><select [(ngModel)]="fLevel" (ngModelChange)="filterChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let l of levels" [ngValue]="l">{{levelLabel(l)}}</option></select></div>
    <div><label>Status</label><select [(ngModel)]="fAktif" (ngModelChange)="filterChanged()"><option [ngValue]="null">Semua</option><option [ngValue]="true">Aktif</option><option [ngValue]="false">Nonaktif</option></select></div>
    <div class="filter-search"><label>Cari</label><input [(ngModel)]="q" (ngModelChange)="search$.next()" placeholder="Kode / nama"></div>
  </div>
  <article class="panel table-panel"><div class="panel-head"><div><h3>Divisi / Unit Organisasi</h3><p>Anggaran (RKA) diisi pada unit level <b>Divisi</b> yang aktif · {{total}} data</p></div></div><div class="table-scroll"><table><thead><tr><th>Kode</th><th>Nama</th><th>Level</th><th>Induk</th><th>Perusahaan</th><th>Dept Odoo</th><th>Status</th><th></th></tr></thead><tbody>
    <tr *ngFor="let x of divisiRows"><td><b>{{x.kode}}</b></td><td>{{x.nama}}<small class="cell-sub" *ngIf="x.path && x.path!==x.nama">{{x.path}}</small></td><td><span class="badge" [class.capex]="x.level!=='DIVISI'">{{levelLabel(x.level)}}</span></td><td>{{x.parentNama || '-'}}</td><td>{{x.companyNama}}</td><td>{{x.odooDepartmentId ?? '-'}}</td><td><span class="badge" [class.success]="x.aktif" [class.danger]="!x.aktif">{{x.aktif?'Aktif':'Nonaktif'}}</span></td><td><button class="table-action" (click)="editDivisi(x)">Edit</button></td></tr>
    <tr *ngIf="!divisiRows.length"><td colspan="8"><div class="empty">{{loading?'Memuat…':'Tidak ada data.'}}</div></td></tr></tbody></table></div><app-pager [page]="page" [size]="size" [total]="total" (pageChange)="goPage($event)" (sizeChange)="setSize($event)"></app-pager></article>
</ng-container>

<ng-container *ngIf="tab==='coa'">
  <div class="filter-card">
    <div><label>Tipe</label><select [(ngModel)]="fTipe" (ngModelChange)="filterChanged()"><option [ngValue]="null">Semua</option><option value="OPEX">OPEX</option><option value="CAPEX">CAPEX</option></select></div>
    <div><label>Status</label><select [(ngModel)]="fAktif" (ngModelChange)="filterChanged()"><option [ngValue]="null">Semua</option><option [ngValue]="true">Aktif</option><option [ngValue]="false">Nonaktif</option></select></div>
    <div class="filter-search"><label>Cari</label><input [(ngModel)]="q" (ngModelChange)="search$.next()" placeholder="No COA / nama / kategori"></div>
  </div>
  <article class="panel table-panel"><div class="panel-head"><div><h3>Mata Anggaran / COA</h3><p>Master akun OPEX dan CAPEX · {{total}} data</p></div></div><div class="table-scroll"><table><thead><tr><th>No COA</th><th>Nama Mata Anggaran</th><th>Tipe</th><th>Kategori</th><th>Status</th><th></th></tr></thead><tbody>
    <tr *ngFor="let x of coaRows"><td><b>{{x.noCoa}}</b></td><td>{{x.nama}}</td><td><span class="badge" [class.capex]="x.tipe==='CAPEX'">{{x.tipe}}</span></td><td>{{x.kategori||'-'}}</td><td><span class="badge" [class.success]="x.aktif" [class.danger]="!x.aktif">{{x.aktif?'Aktif':'Nonaktif'}}</span></td><td><button class="table-action" (click)="editCoa(x)">Edit</button></td></tr>
    <tr *ngIf="!coaRows.length"><td colspan="6"><div class="empty">{{loading?'Memuat…':'Tidak ada data.'}}</div></td></tr></tbody></table></div><app-pager [page]="page" [size]="size" [total]="total" (pageChange)="goPage($event)" (sizeChange)="setSize($event)"></app-pager></article>
</ng-container>

<div class="modal-backdrop" *ngIf="modal" (click)="modal=false"></div><section class="modal" *ngIf="modal"><div class="modal-head"><div><small>MASTER DATA</small><h2>{{editing?'Ubah':'Tambah'}} {{tabLabel}}</h2></div><button class="icon-btn" (click)="modal=false">×</button></div>
<div *ngIf="formError" class="alert danger form-alert">{{formError}}</div>
<div class="form-grid" *ngIf="tab==='company'"><div><label>Kode *</label><input [(ngModel)]="form.kode" maxlength="30"></div><div><label>Nama *</label><input [(ngModel)]="form.nama" maxlength="150"></div><div><label>Nama Group</label><input [(ngModel)]="form.namaGrup"></div><div><label>Logo URL</label><input [(ngModel)]="form.logoUrl" placeholder="https://..."></div></div>
<div class="form-grid" *ngIf="tab==='divisi'">
  <div><label>Perusahaan *</label><select [(ngModel)]="form.companyId" (change)="form.parentId=null"><option *ngFor="let c of companies" [ngValue]="c.id">{{c.nama}}</option></select></div>
  <div><label>Level *</label><select [(ngModel)]="form.level"><option *ngFor="let l of levels" [ngValue]="l">{{levelLabel(l)}}</option></select></div>
  <div><label>Kode *</label><input [(ngModel)]="form.kode" maxlength="30"></div>
  <div><label>Nama *</label><input [(ngModel)]="form.nama" maxlength="150"></div>
  <div class="span-2"><label>Induk</label><select [(ngModel)]="form.parentId"><option [ngValue]="null">— Tanpa induk —</option><option *ngFor="let p of parentOptions" [ngValue]="p.id">{{p.path || p.nama}} ({{levelLabel(p.level)}})</option></select></div>
  <div><label>ID Departemen Odoo</label><input type="number" [(ngModel)]="form.odooDepartmentId" placeholder="hr_department.id"></div>
  <div><label>Status</label><select [(ngModel)]="form.aktif"><option [ngValue]="true">Aktif</option><option [ngValue]="false">Nonaktif</option></select></div>
  <div class="span-2"><label>Path</label><input [(ngModel)]="form.path" placeholder="Kosong = dibentuk otomatis dari induk"></div>
</div>
<div class="form-grid" *ngIf="tab==='coa'"><div><label>No COA *</label><input [(ngModel)]="form.noCoa" inputmode="numeric" placeholder="10 digit, contoh 6101010001"></div><div><label>Tipe *</label><select [(ngModel)]="form.tipe"><option value="OPEX">OPEX</option><option value="CAPEX">CAPEX</option></select></div><div class="span-2"><label>Nama Mata Anggaran *</label><input [(ngModel)]="form.nama" maxlength="200"></div><div><label>Kategori</label><input [(ngModel)]="form.kategori"></div><div><label>Status</label><select [(ngModel)]="form.aktif"><option [ngValue]="true">Aktif</option><option [ngValue]="false">Nonaktif</option></select></div></div>
<div class="modal-actions"><button class="btn ghost" (click)="modal=false">Batal</button><button class="btn primary" [disabled]="saving" (click)="save()">{{saving?'Menyimpan…':'Simpan'}}</button></div></section>
`})
export class MasterPage implements OnInit, OnDestroy {
  tab:'company'|'divisi'|'coa'='company';companies:Company[]=[];divisi:Divisi[]=[];
  /** Isi tabel divisi / COA per halaman dari server; divisi penuh tetap dipakai untuk pilihan induk dan jumlah per perusahaan. */
  divisiRows:Divisi[]=[];coaRows:MataAnggaran[]=[];coaCount=0;page=0;size=20;total=0;loading=false;
  readonly search$=new Subject<void>();private subs=new Subscription();private tableSub?:Subscription;
  modal=false;saving=false;importing=false;editing:any;form:any={};message='';error='';formError='';importErrors:string[]=[];
  levels:LevelOrganisasi[]=['HEAD_OFFICE','DIREKTORAT','DIVISI','UNIT'];
  fCompany:number|null=null;fLevel:LevelOrganisasi|null=null;fAktif:boolean|null=null;fTipe:TipeAnggaran|null=null;q='';
  constructor(private api:ApiService){}
  ngOnInit(){this.subs.add(this.search$.pipe(debounceTime(300)).subscribe(()=>this.filterChanged()));this.reload();}
  ngOnDestroy(){this.subs.unsubscribe();this.tableSub?.unsubscribe();}
  reload(){forkJoin([this.api.companies(),this.api.divisi(),this.api.coaPage({page:0,size:1})]).subscribe({next:([a,b,c])=>{this.companies=a;this.divisi=b;this.coaCount=c.total;this.loadTable();},error:e=>this.error=errMsg(e,'Master data gagal dimuat.')});}
  /** Ambil satu halaman tabel tab aktif sesuai filter dan kata kunci. */
  loadTable(){
    this.tableSub?.unsubscribe();
    if(this.tab==='company')return;
    const base={q:this.q,page:this.page,size:this.size,aktif:this.fAktif};
    this.loading=true;
    const done=()=>this.loading=false;
    const fail=(e:unknown)=>{done();this.error=errMsg(e,'Data gagal dimuat.');};
    this.tableSub=this.tab==='divisi'
      ?this.api.divisiPage({...base,companyId:this.fCompany,level:this.fLevel}).subscribe({next:r=>{done();this.divisiRows=r.items;this.total=r.total;this.clampPage(r.totalPages);},error:fail})
      :this.api.coaPage({...base,tipe:this.fTipe}).subscribe({next:r=>{done();this.coaRows=r.items;this.total=r.total;this.clampPage(r.totalPages);},error:fail});
  }
  /** Setelah hapus/filter halaman bisa melewati halaman terakhir: mundur ke halaman terakhir yang ada. */
  private clampPage(totalPages:number){if(this.page>0&&this.page>=totalPages){this.page=Math.max(0,totalPages-1);this.loadTable();}}
  filterChanged(){this.page=0;this.loadTable();}
  goPage(p:number){this.page=p;this.loadTable();}
  setSize(s:number){this.size=s;this.page=0;this.loadTable();}
  get tabLabel(){return this.tab==='company'?'Perusahaan':this.tab==='divisi'?'Divisi':'Mata Anggaran';}
  setTab(t:'company'|'divisi'|'coa'){this.tab=t;this.error='';this.message='';this.importErrors=[];this.q='';this.fAktif=null;this.page=0;this.total=0;this.loadTable();}
  downloadTemplateCoa(){this.api.templateCoa().subscribe({next:b=>downloadBlob(b,'template-mata-anggaran.xlsx'),error:e=>this.error=errMsg(e,'Template gagal diunduh.')});}
  /** Export mengikuti filter dan kata kunci tabel yang sedang tampil. */
  exportCoa(){this.api.exportCoa({tipe:this.fTipe,aktif:this.fAktif,q:this.q}).subscribe({next:b=>downloadBlob(b,'mata-anggaran.xlsx'),error:e=>this.error=errMsg(e,'Export gagal.')});}
  importCoa(ev:Event){
    const input=ev.target as HTMLInputElement; const f=input.files?.[0]; if(!f)return;
    this.error='';this.message='';this.importErrors=[];this.importing=true;
    this.api.importCoa(f).subscribe({
      next:r=>{this.importing=false;input.value='';this.message=`Import selesai: ${r.totalBaris} baris, ${r.dibuat} dibuat, ${r.diperbarui} diperbarui.`;this.reload();},
      error:e=>{this.importing=false;input.value='';
        if(e?.status===422&&e.error?.errors){this.error=`Import dibatalkan, ditemukan ${e.error.errors.length} kesalahan (tidak ada data yang disimpan):`;this.importErrors=e.error.errors;}
        else this.error=errMsg(e,'Import Excel gagal.');}
    });
  }
  levelLabel(l:string){return ({HEAD_OFFICE:'Head Office',DIREKTORAT:'Direktorat',DIVISI:'Divisi',UNIT:'Unit'} as Record<string,string>)[l]||l;}
  countDivisi(companyId:number){return this.divisi.filter(d=>d.companyId===companyId&&d.level==='DIVISI').length;}
  /** Induk harus satu perusahaan dan bukan dirinya sendiri. */
  get parentOptions(){return this.divisi.filter(d=>d.companyId===this.form.companyId&&d.id!==this.editing?.id);}

  openAdd(){this.editing=null;this.formError='';
    this.form=this.tab==='company'?{kode:'',nama:'',namaGrup:'',logoUrl:''}
      :this.tab==='divisi'?{companyId:this.fCompany??this.companies[0]?.id,kode:'',nama:'',level:'DIVISI',parentId:null,path:'',odooDepartmentId:null,aktif:true}
      :{noCoa:'',nama:'',tipe:'OPEX',kategori:'',aktif:true};
    this.modal=true;}
  editCompany(x:Company){this.tab='company';this.editing=x;this.formError='';this.form={...x};this.modal=true;}
  editDivisi(x:Divisi){this.tab='divisi';this.editing=x;this.formError='';this.form={...x,path:x.path||''};this.modal=true;}
  editCoa(x:MataAnggaran){this.tab='coa';this.editing=x;this.formError='';this.form={...x};this.modal=true;}
  save(){
    const f=this.form;this.formError='';
    let req:any;
    if(this.tab==='company'){
      if(!f.kode?.trim()||!f.nama?.trim()){this.formError='Kode dan nama wajib diisi.';return;}
      const body={kode:f.kode.trim(),nama:f.nama.trim(),namaGrup:f.namaGrup||null,logoUrl:f.logoUrl||null};
      req=this.editing?this.api.updateCompany(this.editing.id,body):this.api.createCompany(body);
    }else if(this.tab==='divisi'){
      if(!f.companyId||!f.kode?.trim()||!f.nama?.trim()){this.formError='Perusahaan, kode, dan nama wajib diisi.';return;}
      // Kirim semua field: backend mengganti level/induk/path/odoo jika dikirim null.
      const pathChanged=this.editing&&(f.nama.trim()!==this.editing.nama||f.parentId!==this.editing.parentId);
      const body={companyId:f.companyId,kode:f.kode.trim(),nama:f.nama.trim(),parentId:f.parentId??null,level:f.level||'DIVISI',
        path:(f.path&&!(pathChanged&&f.path===this.editing.path))?f.path:null,
        odooDepartmentId:f.odooDepartmentId===''||f.odooDepartmentId==null?null:Number(f.odooDepartmentId),aktif:f.aktif};
      req=this.editing?this.api.updateDivisi(this.editing.id,body):this.api.createDivisi(body);
    }else{
      if(!/^\d{4,30}$/.test(String(f.noCoa||'').trim())){this.formError='No COA harus berupa angka (minimal 4 digit).';return;}
      if(!f.nama?.trim()){this.formError='Nama mata anggaran wajib diisi.';return;}
      const body={noCoa:String(f.noCoa).trim(),nama:f.nama.trim(),tipe:f.tipe,kategori:f.kategori||null,aktif:f.aktif};
      req=this.editing?this.api.updateCoa(this.editing.id,body):this.api.createCoa(body);
    }
    this.saving=true;
    req.subscribe({next:()=>{this.saving=false;this.modal=false;this.message=`${this.tabLabel} berhasil disimpan.`;this.reload();},error:(e:unknown)=>{this.saving=false;this.formError=errMsg(e,'Gagal menyimpan master data.');}});
  }
}
