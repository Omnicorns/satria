import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { clamp, downloadBlob, errMsg, money, yearOptions } from '../../core/format';
import { Company, Divisi, MataAnggaran, MonitoringRka, ProgramKerja, TipeAnggaran } from '../../core/models';

@Component({selector:'app-rka',standalone:true,imports:[CommonModule,FormsModule,RouterLink],template:`
<div class="page-head">
  <div><div class="eyebrow">BUDGET & RKA</div><h1>Monitoring RKA</h1><p>Kelola program kerja, pagu, realisasi, dan capaian per mata anggaran per divisi.</p></div>
  <div class="head-actions"><button class="btn secondary" (click)="downloadTemplate()">↓ Template Excel</button><button class="btn secondary" (click)="exportExcel()">⇩ Export</button><button class="btn primary" (click)="openCreate()">＋ Tambah Program</button></div>
</div>
<div class="filter-card">
  <div><label>Perusahaan</label><select [(ngModel)]="companyId" (change)="companyChanged()"><option [ngValue]="null">Semua</option><option *ngFor="let c of companies" [ngValue]="c.id">{{c.nama}}</option></select></div>
  <div><label>Divisi</label><select [(ngModel)]="divisiId" (change)="load()"><option [ngValue]="null">Semua</option><option *ngFor="let d of filteredDivisi" [ngValue]="d.id">{{d.nama}}</option></select></div>
  <div><label>Tahun</label><select [(ngModel)]="tahun" (change)="load()"><option [ngValue]="null">Semua tahun</option><option *ngFor="let y of years" [ngValue]="y">{{y}}</option></select></div>
  <div><label>Tipe</label><select [(ngModel)]="tipe" (change)="load()"><option [ngValue]="null">Semua</option><option value="OPEX">OPEX</option><option value="CAPEX">CAPEX</option></select></div>
  <div class="filter-search"><label>Cari</label><input [(ngModel)]="q" placeholder="COA / program / divisi"></div>
  <div class="file-action"><label>Import RKA</label><input #file type="file" accept=".xlsx" hidden (change)="importExcel($event)"><button class="btn secondary full" [disabled]="importing" (click)="file.click()">{{importing?'Mengimpor…':'↑ Import Excel'}}</button></div>
</div>
<div *ngIf="message" class="alert success">{{message}}</div>
<div *ngIf="error" class="alert danger">{{error}}<ul *ngIf="importErrors.length" class="error-list"><li *ngFor="let e of importErrors">{{e}}</li></ul></div>
<section class="kpi-grid small-kpi">
  <article class="kpi"><span>Total Planning</span><strong>{{money(total('planningBudget'))}}</strong><small>{{filtered.length}} program kerja</small></article>
  <article class="kpi"><span>Total Pagu</span><strong>{{money(total('pagu'))}}</strong><small>plafon aktif</small></article>
  <article class="kpi"><span>Total Realisasi</span><strong>{{money(total('realisasi'))}}</strong><small>serapan berjalan</small></article>
  <article class="kpi"><span>Sisa Anggaran</span><strong>{{money(total('sisa'))}}</strong><small>belum terealisasi</small></article>
</section>

<section class="dashboard-grid two-one" *ngIf="monitoring as m">
  <article class="panel">
    <div class="panel-head"><div><h3>Planning · Pagu · Realisasi</h3><p>Per mata anggaran (10 pagu terbesar)</p></div>
      <div class="chart-legend"><span><i class="sw planning"></i>Planning</span><span><i class="sw pagu"></i>Pagu</span><span><i class="sw real"></i>Realisasi</span></div></div>
    <div class="group-bars" *ngIf="topItems(m).length; else noChart">
      <div class="gb-row" *ngFor="let it of topItems(m)">
        <div class="gb-label"><b>{{it.nama}}</b><small>{{it.noCoa}} · {{it.divisiNama}}</small></div>
        <div class="gb-bars">
          <i class="planning" [style.width.%]="scale(it.planning,m)" [title]="'Planning '+money(it.planning)"></i>
          <i class="pagu" [style.width.%]="scale(it.pagu,m)" [title]="'Pagu '+money(it.pagu)"></i>
          <i class="real" [style.width.%]="scale(it.realisasi,m)" [title]="'Realisasi '+money(it.realisasi)"></i>
        </div>
        <span class="gb-pct">{{it.capaianPersen | number:'1.0-1'}}%</span>
      </div>
    </div><ng-template #noChart><div class="empty">Belum ada data.</div></ng-template>
  </article>
  <article class="panel">
    <div class="panel-head"><div><h3>Distribusi Capaian</h3><p>Jumlah mata anggaran per rentang capaian</p></div></div>
    <div class="dist-list">
      <div *ngFor="let d of distribusi(m)"><span class="dist-name">{{d.label}}</span><div class="bar-bg"><i [style.width.%]="d.pct" [style.background]="d.color"></i></div><b>{{d.n}}</b></div>
    </div>
  </article>
</section>

<article class="panel table-panel">
  <div class="panel-head"><div><h3>Daftar Program Kerja</h3><p>{{filtered.length}} data ditemukan</p></div></div>
  <div class="table-scroll"><table><thead><tr><th>COA / Mata Anggaran</th><th>Divisi</th><th>Tahun</th><th>Tipe</th><th>Planning</th><th>Pagu</th><th>Realisasi</th><th>Sisa</th><th>Capaian</th><th>Status</th><th></th></tr></thead><tbody>
    <tr *ngFor="let r of filtered"><td><b>{{r.namaMataAnggaran}}</b><small class="cell-sub">{{r.noCoa}}</small></td><td>{{r.divisiNama}}<small class="cell-sub">{{r.companyNama}}</small></td><td>{{r.tahun}}</td><td><span class="badge" [class.capex]="r.tipe==='CAPEX'">{{r.tipe}}</span></td><td>{{money(r.planningBudget)}}</td><td>{{r.pagu==null?'-':money(r.pagu)}}</td><td>{{money(r.realisasi)}}</td><td [class.neg]="r.sisa<0">{{money(r.sisa)}}</td><td><div class="progress-cell"><span>{{r.capaianPersen | number:'1.1-1'}}%</span><div class="bar-bg"><i [style.width.%]="clamp(r.capaianPersen)"></i></div></div></td><td><span class="badge" [class.success]="r.status==='OPEN'">{{r.status}}</span></td>
      <td class="row-actions"><a class="table-action" routerLink="/pekerjaan" [queryParams]="{programKerjaId:r.id}">Pekerjaan</a><button class="table-action" (click)="openEdit(r)">Edit</button><button class="table-action danger" (click)="remove(r)">Hapus</button></td></tr>
    <tr *ngIf="!filtered.length"><td colspan="11"><div class="empty">Belum ada data RKA untuk filter ini.</div></td></tr>
  </tbody></table></div>
</article>

<div class="modal-backdrop" *ngIf="modal" (click)="modal=false"></div>
<section class="modal" *ngIf="modal">
  <div class="modal-head"><div><small>{{editing ? 'EDIT PROGRAM' : 'PROGRAM BARU'}}</small><h2>{{editing ? 'Ubah Program Kerja' : 'Tambah Program Kerja'}}</h2></div><button class="icon-btn" (click)="modal=false">×</button></div>
  <div *ngIf="formError" class="alert danger form-alert">{{formError}}</div>
  <div class="form-grid">
    <div class="span-2"><label>Divisi *</label><select [(ngModel)]="form.divisiId"><option [ngValue]="null">Pilih divisi</option><option *ngFor="let d of divisi" [ngValue]="d.id">{{d.companyNama}} · {{d.nama}}</option></select></div>
    <div><label>Tahun *</label><select [(ngModel)]="form.tahun"><option *ngFor="let y of years" [ngValue]="y">{{y}}</option></select></div>
    <div><label>Tipe</label><select [(ngModel)]="form.tipe" (change)="tipeChanged()"><option value="OPEX">OPEX</option><option value="CAPEX">CAPEX</option></select></div>
    <div class="span-2"><label>Mata Anggaran (COA) *</label><select [(ngModel)]="form.mataAnggaranId"><option [ngValue]="null">Pilih COA</option><option *ngFor="let c of selectableCoa" [ngValue]="c.id">{{c.noCoa}} · {{c.nama}}</option></select></div>
    <div><label>Planning Budget *</label><input type="number" min="0" [(ngModel)]="form.planningBudget" placeholder="0"><small class="hint">{{money(form.planningBudget)}}</small></div>
    <div><label>Pagu</label><input type="number" min="0" [(ngModel)]="form.pagu" placeholder="Kosong = sama dengan planning"><small class="hint">{{form.pagu==null||form.pagu===''?'mengikuti planning':money(form.pagu)}}</small></div>
    <div><label>Status</label><select [(ngModel)]="form.status"><option value="OPEN">OPEN</option><option value="CLOSED">CLOSED</option></select></div>
    <div class="span-2"><label>Keterangan</label><textarea rows="3" [(ngModel)]="form.keterangan" placeholder="Catatan tambahan"></textarea></div>
  </div>
  <div class="modal-actions"><button class="btn ghost" (click)="modal=false">Batal</button><button class="btn primary" [disabled]="saving" (click)="save()">{{saving?'Menyimpan…':'Simpan Program'}}</button></div>
</section>
`})
export class RkaPage implements OnInit {
  companies:Company[]=[];divisi:Divisi[]=[];coa:MataAnggaran[]=[];rows:ProgramKerja[]=[];selectableCoa:MataAnggaran[]=[];monitoring?:MonitoringRka;
  companyId:number|null=null;divisiId:number|null=null;tahun:number|null=new Date().getFullYear();tipe:TipeAnggaran|null=null;q='';years=yearOptions();
  modal=false;editing?:ProgramKerja;saving=false;importing=false;message='';error='';formError='';importErrors:string[]=[];
  form:any={};
  readonly money=money; readonly clamp=clamp;
  constructor(private api:ApiService){}
  ngOnInit(){forkJoin([this.api.companies(),this.api.divisiAnggaran(),this.api.coa()]).subscribe({next:([c,d,m])=>{this.companies=c;this.divisi=d;this.coa=m;this.load();},error:e=>this.error=errMsg(e,'Master data gagal dimuat.')});}
  get filteredDivisi(){return this.companyId?this.divisi.filter(d=>d.companyId===this.companyId):this.divisi;}
  get filtered(){const x=this.q.toLowerCase().trim();return !x?this.rows:this.rows.filter(r=>[r.noCoa,r.namaMataAnggaran,r.divisiNama,r.companyNama,r.keterangan].join(' ').toLowerCase().includes(x));}
  private get filters(){return {companyId:this.companyId,divisiId:this.divisiId,tahun:this.tahun,tipe:this.tipe};}
  companyChanged(){this.divisiId=null;this.load();}
  load(){
    this.api.rka(this.filters).subscribe({next:r=>this.rows=r,error:e=>this.error=errMsg(e,'Data RKA gagal dimuat.')});
    this.api.monitoring(this.filters).subscribe({next:m=>this.monitoring=m,error:()=>this.monitoring=undefined});
  }
  filterCoa(){this.selectableCoa=this.coa.filter(c=>c.tipe===this.form.tipe && (c.aktif || c.id===this.form.mataAnggaranId));}
  tipeChanged(){this.filterCoa();if(!this.selectableCoa.some(c=>c.id===this.form.mataAnggaranId))this.form.mataAnggaranId=null;}
  total(k:'planningBudget'|'pagu'|'realisasi'|'sisa'){return this.filtered.reduce((a,r)=>a+(Number(k==='pagu'?(r.pagu??r.planningBudget):r[k])||0),0);}

  topItems(m:MonitoringRka){return [...(m.perMataAnggaran||[])].sort((a,b)=>(b.pagu||0)-(a.pagu||0)).slice(0,10);}
  scale(v:number,m:MonitoringRka){const max=Math.max(1,...this.topItems(m).map(i=>Math.max(i.planning||0,i.pagu||0,i.realisasi||0)));return clamp((v||0)/max*100);}
  distribusi(m:MonitoringRka){const d=m.distribusi||{nol:0,satuSampai49:0,limaPuluhSampai99:0,seratusPlus:0};const total=Math.max(1,d.nol+d.satuSampai49+d.limaPuluhSampai99+d.seratusPlus);
    return [{label:'0%',n:d.nol,color:'#ef4444'},{label:'1–49%',n:d.satuSampai49,color:'#f59e0b'},{label:'50–99%',n:d.limaPuluhSampai99,color:'#3b82f6'},{label:'≥100%',n:d.seratusPlus,color:'#22c55e'}].map(x=>({...x,pct:x.n/total*100}));}

  openCreate(){this.editing=undefined;this.formError='';this.form={divisiId:this.divisiId,tahun:this.tahun||new Date().getFullYear(),tipe:this.tipe||'OPEX',mataAnggaranId:null,planningBudget:0,pagu:null,status:'OPEN',keterangan:''};this.filterCoa();this.modal=true;}
  openEdit(r:ProgramKerja){this.editing=r;this.formError='';this.form={divisiId:r.divisiId,tahun:r.tahun,tipe:r.tipe,mataAnggaranId:r.mataAnggaranId,planningBudget:r.planningBudget,pagu:r.pagu,status:r.status,keterangan:r.keterangan||''};
    if(!this.divisi.some(d=>d.id===r.divisiId))this.divisi=[...this.divisi,{id:r.divisiId,companyId:r.companyId,companyNama:r.companyNama,kode:r.divisiKode,nama:r.divisiNama,level:'DIVISI',aktif:false}];
    this.filterCoa();this.modal=true;}
  save(){
    this.formError='';this.message='';
    const f=this.form;
    if(!f.divisiId||!f.mataAnggaranId||!f.tahun){this.formError='Divisi, tahun, dan mata anggaran wajib diisi.';return;}
    if(f.planningBudget===null||f.planningBudget===''||Number(f.planningBudget)<0){this.formError='Planning budget wajib diisi dan tidak boleh negatif.';return;}
    if(f.pagu!==null&&f.pagu!==''&&Number(f.pagu)<0){this.formError='Pagu tidak boleh negatif.';return;}
    const body={...f,planningBudget:Number(f.planningBudget),pagu:f.pagu===null||f.pagu===''?null:Number(f.pagu),keterangan:f.keterangan||null};
    this.saving=true;
    const req=this.editing?this.api.updateRka(this.editing.id,body):this.api.createRka(body);
    req.subscribe({next:()=>{this.saving=false;this.modal=false;this.message='Program kerja berhasil disimpan.';this.load();},error:e=>{this.saving=false;this.formError=errMsg(e,'Gagal menyimpan program kerja.');}});
  }
  remove(r:ProgramKerja){
    if(!confirm(`Hapus program ${r.noCoa} · ${r.namaMataAnggaran} (${r.divisiNama} ${r.tahun})?`))return;
    this.error='';this.message='';
    this.api.deleteRka(r.id).subscribe({next:()=>{this.message='Program kerja dihapus.';this.load();},error:e=>this.error=errMsg(e,'Gagal menghapus program kerja.')});
  }
  downloadTemplate(){this.api.templateRka().subscribe({next:b=>downloadBlob(b,'template-rka.xlsx'),error:e=>this.error=errMsg(e,'Template gagal diunduh.')});}
  exportExcel(){this.api.exportRka(this.filters).subscribe({next:b=>downloadBlob(b,`monitoring-rka-${this.tahun||'semua'}.xlsx`),error:e=>this.error=errMsg(e,'Export gagal.')});}
  importExcel(ev:Event){
    const input=ev.target as HTMLInputElement; const f=input.files?.[0]; if(!f)return;
    this.error='';this.message='';this.importErrors=[];this.importing=true;
    this.api.importRka(f).subscribe({
      next:r=>{this.importing=false;input.value='';this.message=`Import selesai: ${r.totalBaris} baris, ${r.dibuat} dibuat, ${r.diperbarui} diperbarui.`;this.load();},
      error:e=>{this.importing=false;input.value='';
        if(e?.status===422&&e.error?.errors){this.error=`Import dibatalkan, ${e.error.errors.length} baris bermasalah (tidak ada data yang disimpan):`;this.importErrors=e.error.errors;}
        else this.error=errMsg(e,'Import Excel gagal.');}
    });
  }
}
