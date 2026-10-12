import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { clamp, errMsg, moneyShort, yearOptions } from '../../core/format';
import { CapexOpex, Company, Divisi, GroupOverview, MonitoringRka, OverviewRow, TopItem } from '../../core/models';

/** Dashboard hanya untuk PT Sarinah (tanpa "Semua perusahaan"); bila tidak ada, pakai semua perusahaan yang ada. */
function onlySarinah(c:Company[]){ const s=c.filter(x=>/sarinah/i.test(x.nama)); return s.length?s:c; }

@Component({selector:'app-dashboard',standalone:true,imports:[CommonModule,FormsModule],template:`
<div class="page-head">
  <div><div class="eyebrow">GROUP OVERVIEW</div><h1>Executive Budget Dashboard</h1><p>Ringkasan anggaran, serapan, dan proyeksi RKAP dalam satu tampilan.</p></div>
  <div class="filter-card compact">
    <select [(ngModel)]="companyId" (change)="companyChanged()"><option *ngFor="let c of companies" [ngValue]="c.id">{{c.nama}}</option></select>
    <select [(ngModel)]="divisiId" (change)="load()"><option [ngValue]="null">Semua divisi</option><option *ngFor="let d of filteredDivisi" [ngValue]="d.id">{{d.nama}}</option></select>
    <select [(ngModel)]="tahun" (change)="load()"><option *ngFor="let y of years" [ngValue]="y">{{y}}</option></select>
  </div>
</div>

<div *ngIf="loading && !overview" class="state-card">Memuat dashboard…</div>
<div *ngIf="error" class="alert danger">{{error}}</div>
<ng-container *ngIf="overview as o">
  <section class="kpi-grid">
    <article class="kpi"><div class="kpi-top"><span class="kpi-icon blue">▦</span><span class="trend neutral">TA {{o.tahun}}</span></div><span>Total RKA</span><strong>{{money(o.kpi.rka)}}</strong><small>CAPEX {{money(o.kpi.rkaCapex)}} · OPEX {{money(o.kpi.rkaOpex)}}</small></article>
    <article class="kpi"><div class="kpi-top"><span class="kpi-icon green">↗</span><span class="trend good">{{o.kpi.serapanPersen | number:'1.1-1'}}%</span></div><span>Total Serapan</span><strong>{{money(o.kpi.serapan)}}</strong><div class="progress-track"><i [style.width.%]="clamp(o.kpi.serapanPersen)"></i></div><small>dari total RKA tahun berjalan</small></article>
    <article class="kpi"><div class="kpi-top"><span class="kpi-icon amber">◷</span><span class="trend warn">{{o.kpi.belumTerserapPersen | number:'1.1-1'}}%</span></div><span>Belum Terserap</span><strong>{{money(o.kpi.belumTerserap)}}</strong><small>ruang anggaran yang masih tersedia</small></article>
    <article class="kpi featured"><div class="kpi-top"><span class="kpi-icon purple">◆</span><span class="trend neutral">RKAP {{o.tahunUsulan}}</span></div><span>Usulan Tahun Berikutnya</span><strong>{{money(o.kpi.usulanRkap)}}</strong><small>nilai usulan lintas entitas & divisi</small></article>
  </section>

  <section class="dashboard-grid two-one">
    <article class="panel">
      <div class="panel-head"><div><h3>Serapan per Divisi</h3><p>Anggaran vs realisasi tahun {{o.tahun}}</p></div><span class="pill">Top 8</span></div>
      <div class="bar-list" *ngIf="o.perDivisi.length; else emptyDivisi">
        <div class="bar-row" *ngFor="let row of o.perDivisi.slice(0,8)">
          <div class="bar-label"><b>{{row.nama}}</b><span>{{row.persenTotal | number:'1.1-1'}}%</span></div>
          <div class="bar-bg"><i [style.width.%]="clamp(row.persenTotal)"></i></div>
          <small>{{money(row.serapan)}} / {{money(row.rka)}}</small>
        </div>
      </div><ng-template #emptyDivisi><div class="empty">Belum ada data divisi.</div></ng-template>
    </article>

    <article class="panel">
      <div class="panel-head"><div><h3>Komposisi Serapan</h3><p>Porsi realisasi OPEX dibanding CAPEX</p></div></div>
      <div class="donut-wrap">
        <div class="donut" [style.background]="donutBackground(o)"><div><strong>{{o.kpi.serapanPersen | number:'1.0-0'}}%</strong><span>serapan total</span></div></div>
        <div class="legend">
          <div *ngFor="let kv of objectEntries(o.opexVsCapex)"><span class="dot" [class.capex]="kv[0]==='CAPEX'"></span><div><b>{{kv[0]}}</b><small>{{money(kv[1].realisasi)}} dari {{money(kv[1].anggaran)}}</small><small>serapan {{kv[1].persen | number:'1.1-1'}}%</small></div></div>
        </div>
      </div>
    </article>
  </section>

  <section class="dashboard-grid equal" *ngIf="capex">
    <article class="panel">
      <div class="panel-head"><div><h3>Status Pekerjaan</h3><p>Kondisi pekerjaan CAPEX & OPEX</p></div></div>
      <div class="status-grid">
        <div class="status-box green"><span>On Track</span><strong>{{capex.statusPekerjaan.onTrack}}</strong><small>pekerjaan berjalan</small></div>
        <div class="status-box red"><span>Terlambat</span><strong>{{capex.statusPekerjaan.terlambat}}</strong><small>perlu perhatian</small></div>
        <div class="status-box blue"><span>Selesai</span><strong>{{capex.statusPekerjaan.selesai}}</strong><small>telah selesai</small></div>
      </div>
      <div class="mini-table" *ngIf="capex.progressPekerjaan.length"><div class="mini-row" *ngFor="let p of capex.progressPekerjaan.slice(0,5)"><div><b>{{p.nama}}</b><small>{{p.pic}}</small></div><div class="mini-progress"><span>{{p.progress}}%</span><div class="bar-bg"><i [style.width.%]="p.progress"></i></div></div></div></div>
    </article>
    <article class="panel">
      <div class="panel-head"><div><h3>Progress RKA Terendah</h3><p>Akun yang perlu dipantau</p></div></div>
      <div class="attention-list" *ngIf="monitoring?.progressTerendah?.length; else noAtt">
        <div *ngFor="let item of monitoring!.progressTerendah.slice(0,6)"><div class="rank">{{item.capaianPersen | number:'1.0-0'}}%</div><div><b>{{item.nama}}</b><small>{{item.noCoa}} · {{item.divisiNama}}</small></div><span>{{money(item.realisasi)}}</span></div>
      </div><ng-template #noAtt><div class="empty">Belum ada data untuk ditampilkan.</div></ng-template>
    </article>
  </section>

  <article class="panel table-panel" *ngIf="!companyId && o.perEntitas.length">
    <div class="panel-head"><div><h3>Serapan & Usulan per Entitas</h3><p>Realisasi {{o.tahun}} dan usulan RKAP {{o.tahunUsulan}}</p></div></div>
    <ng-container *ngTemplateOutlet="tbl; context:{rows:o.perEntitas, label:'Entitas', o:o}"></ng-container>
  </article>
  <article class="panel table-panel">
    <div class="panel-head"><div><h3>Serapan & Usulan per Divisi</h3><p>Realisasi {{o.tahun}} dan usulan RKAP {{o.tahunUsulan}}</p></div></div>
    <ng-container *ngTemplateOutlet="tbl; context:{rows:o.perDivisi, label:'Divisi', o:o}"></ng-container>
  </article>

  <section class="dashboard-grid three">
    <article class="panel" *ngFor="let t of tops(o)">
      <div class="panel-head"><div><h3>{{t.title}}</h3><p>Usulan RKAP {{o.tahunUsulan}}</p></div></div>
      <div class="rank-list" *ngIf="t.items.length; else noTop"><div *ngFor="let it of t.items; let i=index"><span class="rank-no">{{i+1}}</span><div><b>{{it.nama}}</b><small>{{it.noCoa}} · {{it.companyNama}}</small></div><strong>{{money(it.nilai)}}</strong></div></div>
      <ng-template #noTop><div class="empty">Belum ada usulan.</div></ng-template>
    </article>
  </section>

  <ng-template #tbl let-rows="rows" let-label="label" let-o="o">
    <div class="table-scroll"><table><thead><tr><th>{{label}}</th><th>RKA {{o.tahun}}</th><th>Serapan</th><th>% OPEX</th><th>% CAPEX</th><th>% Total</th><th>Usulan {{o.tahunUsulan}}</th><th>Usulan vs Serapan</th></tr></thead><tbody>
      <tr *ngFor="let r of asRows(rows)"><td><b>{{r.nama}}</b><small class="cell-sub">{{r.kode}}{{r.induk ? ' · '+r.induk : ''}}</small></td><td>{{money(r.rka)}}</td><td>{{money(r.serapan)}}</td><td>{{pct(r.persenOpex)}}</td><td>{{pct(r.persenCapex)}}</td><td><span class="badge" [class.success]="r.persenTotal>=80" [class.danger]="r.persenTotal<30">{{r.persenTotal | number:'1.1-1'}}%</span></td><td>{{money(r.usulan)}}</td><td>{{ratio(r.usulanVsSerapan)}}</td></tr>
      <tr *ngIf="!rows.length"><td colspan="8"><div class="empty">Belum ada data.</div></td></tr>
    </tbody></table></div>
  </ng-template>
</ng-container>
`})
export class DashboardPage implements OnInit {
  companies:Company[]=[]; divisi:Divisi[]=[]; overview?:GroupOverview; capex?:CapexOpex; monitoring?:MonitoringRka;
  companyId:number|null=null; divisiId:number|null=null; tahun=new Date().getFullYear(); years=yearOptions(); loading=false; error='';
  constructor(private api:ApiService){}
  ngOnInit(){ forkJoin([this.api.companies(),this.api.divisiAnggaran()]).subscribe({next:([c,d])=>{this.companies=onlySarinah(c);this.companyId=this.companies[0]?.id??null;this.divisi=d;this.load();},error:e=>this.error=errMsg(e,'Tidak dapat mengambil master data. Pastikan backend Spring Boot aktif di port 8080.')}); }
  get filteredDivisi(){ return this.companyId ? this.divisi.filter(d=>d.companyId===this.companyId) : this.divisi; }
  companyChanged(){ this.divisiId=null; this.load(); }
  load(){ this.loading=true;this.error=''; const f={companyId:this.companyId,divisiId:this.divisiId,tahun:this.tahun}; forkJoin([this.api.groupOverview(f),this.api.capexOpex(f),this.api.monitoring(f)]).subscribe({next:([o,c,m])=>{this.overview=o;this.capex=c;this.monitoring=m;this.loading=false;},error:e=>{this.loading=false;this.error=errMsg(e,'Dashboard gagal dimuat. Pastikan API backend berjalan.');}}); }
  readonly money=moneyShort; readonly clamp=clamp;
  pct(n:number|null|undefined){ return n==null ? '-' : n.toLocaleString('id-ID',{maximumFractionDigits:1})+'%'; }
  ratio(n:number|null|undefined){ return n==null ? '-' : n.toLocaleString('id-ID',{minimumFractionDigits:2,maximumFractionDigits:2})+'×'; }
  asRows(r:unknown){ return r as OverviewRow[]; }
  tops(o:GroupOverview):{title:string;items:TopItem[]}[]{ return [{title:'Top Akun OPEX',items:o.topAkunOpexUsulan||[]},{title:'Top Program OPEX',items:o.topProgramOpexUsulan||[]},{title:'Top CAPEX',items:o.topCapexUsulan||[]}]; }
  objectEntries(obj:GroupOverview['opexVsCapex']){ return Object.entries(obj||{}).filter(([k])=>k==='OPEX'||k==='CAPEX'); }
  donutBackground(o:GroupOverview){ const op=o.opexVsCapex?.['OPEX']?.realisasi||0, cp=o.opexVsCapex?.['CAPEX']?.realisasi||0, total=op+cp; if(!total) return 'conic-gradient(#e2e8f0 0 100%)'; const pct=op/total*100; return `conic-gradient(#2563eb 0 ${pct}%, #8b5cf6 ${pct}% 100%)`; }
}
