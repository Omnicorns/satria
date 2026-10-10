import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { errMsg } from '../../core/format';
import { Divisi, LdapDepartemenGroup, LdapSyncResult } from '../../core/models';

/** User Active Directory dikelompokkan per departemen (hasil sync), dengan pemetaan ke divisi anggaran. */
@Component({selector:'app-pengguna',standalone:true,imports:[CommonModule,FormsModule],template:`
<div class="page-head">
  <div><div class="eyebrow">CONFIGURATION</div><h1>Pengguna & Departemen AD</h1>
    <p>User Active Directory dikelompokkan per departemen. Sync menarik semua user aktif dan departemennya dari AD.</p></div>
  <button class="btn primary" [disabled]="syncing" (click)="sync()">{{syncing?'Menyinkronkan…':'⟳ Sync dari AD'}}</button>
</div>
<div *ngIf="error" class="alert danger">{{error}}</div>
<div *ngIf="result" class="alert success">
  Sync selesai: {{result.totalUserAd}} user aktif di AD ({{result.userBaru}} baru, {{result.userDiperbarui}} diperbarui),
  {{result.totalDepartemen}} departemen ({{result.departemenBaru}} baru).
  <span *ngIf="result.userTidakAdaDiAd">{{result.userTidakAdaDiAd}} user di SATRIA tidak ada lagi / nonaktif di AD.</span>
</div>

<div class="status-summary">
  <div><span class="status-dot blue"></span><small>Departemen</small><strong>{{jumlahDepartemen}}</strong></div>
  <div><span class="status-dot green"></span><small>Total user</small><strong>{{totalUser}}</strong></div>
  <div><span class="status-dot purple"></span><small>Dipetakan ke divisi</small><strong>{{jumlahDipetakan}}</strong></div>
  <div><span class="status-dot red"></span><small>Tanpa departemen</small><strong>{{tanpaDepartemen}}</strong></div>
</div>

<div class="filter-card">
  <div class="filter-search" style="grid-column:span 3"><label>Cari</label><input [(ngModel)]="q" placeholder="Departemen / username / nama / jabatan"></div>
  <div><label>Tampilan</label><select [(ngModel)]="expandAll"><option [ngValue]="false">Ringkas</option><option [ngValue]="true">Buka semua</option></select></div>
</div>

<div *ngIf="loading" class="state-card">Memuat…</div>
<div *ngIf="!loading && !groups.length" class="panel empty">Belum ada data. Klik <b>Sync dari AD</b> untuk menarik user dan departemen.</div>

<article class="panel table-panel dept-panel" *ngFor="let g of filtered">
  <div class="panel-head dept-head" (click)="toggle(g)">
    <div><h3><span class="chev-sm">{{isOpen(g)?'▾':'▸'}}</span> {{g.nama}} <span class="pill">{{g.users.length}} user</span></h3>
      <p *ngIf="g.id!==null">Divisi anggaran: <b>{{g.divisiNama||'belum dipetakan'}}</b></p>
      <p *ngIf="g.id===null">User yang atribut department-nya kosong di AD</p></div>
    <div *ngIf="g.id!==null" class="dept-map" (click)="$event.stopPropagation()">
      <select [ngModel]="g.divisiId" (ngModelChange)="map(g,$event)" title="Petakan ke divisi anggaran">
        <option [ngValue]="null">— Petakan ke divisi —</option>
        <option *ngFor="let d of divisi" [ngValue]="d.id">{{d.nama}}</option>
      </select>
    </div>
  </div>
  <div class="table-scroll" *ngIf="isOpen(g)"><table><thead><tr><th>Username</th><th>Nama</th><th>Jabatan</th><th>Email</th><th>Login terakhir</th><th>Status</th></tr></thead><tbody>
    <tr *ngFor="let u of g.users"><td><b>{{u.username}}</b></td><td>{{u.nama||'-'}}</td><td>{{u.jabatan||'-'}}</td><td>{{u.email||'-'}}</td>
      <td>{{u.lastLoginAt?(u.lastLoginAt|date:'d MMM y HH:mm'):'Belum pernah'}}</td>
      <td><span class="badge" [class.success]="u.aktif" [class.danger]="!u.aktif">{{u.aktif?'Aktif':'Diblokir'}}</span></td></tr>
  </tbody></table></div>
</article>
`})
export class PenggunaPage implements OnInit {
  private api=inject(ApiService);
  groups:LdapDepartemenGroup[]=[]; divisi:Divisi[]=[]; result:LdapSyncResult|null=null;
  loading=false; syncing=false; error=''; q=''; expandAll=false;
  private open=new Set<string>();

  ngOnInit(){ this.load(); this.api.divisi({aktif:true}).subscribe({next:d=>this.divisi=d,error:()=>{}}); }

  load(){
    this.loading=true;
    this.api.ldapDepartemen().subscribe({
      next:g=>{this.groups=g;this.loading=false;},
      error:e=>{this.error=errMsg(e,'Gagal memuat departemen');this.loading=false;}
    });
  }

  sync(){
    this.syncing=true; this.error=''; this.result=null;
    this.api.ldapSync().subscribe({
      next:r=>{this.result=r;this.syncing=false;this.load();},
      error:e=>{this.error=errMsg(e,'Sync dari AD gagal');this.syncing=false;}
    });
  }

  map(g:LdapDepartemenGroup, divisiId:number|null){
    if(g.id===null) return;
    this.api.mapLdapDepartemen(g.id,divisiId).subscribe({
      next:()=>{g.divisiId=divisiId;g.divisiNama=this.divisi.find(d=>d.id===divisiId)?.nama??null;},
      error:e=>this.error=errMsg(e,'Gagal memetakan departemen')
    });
  }

  private key(g:LdapDepartemenGroup){ return g.id===null?'-':String(g.id); }
  isOpen(g:LdapDepartemenGroup){ return this.expandAll || !!this.q.trim() || this.open.has(this.key(g)); }
  toggle(g:LdapDepartemenGroup){ const k=this.key(g); this.open.has(k)?this.open.delete(k):this.open.add(k); }

  /** Cari di nama departemen atau di user; bila cocok di user, hanya user yang cocok yang ditampilkan. */
  get filtered():LdapDepartemenGroup[]{
    const q=this.q.trim().toLowerCase();
    if(!q) return this.groups;
    return this.groups.map(g=>{
      if(g.nama.toLowerCase().includes(q)) return g;
      const users=g.users.filter(u=>[u.username,u.nama,u.jabatan,u.email].some(v=>v?.toLowerCase().includes(q)));
      return {...g,users};
    }).filter(g=>g.users.length||g.nama.toLowerCase().includes(q));
  }
  get jumlahDepartemen(){ return this.groups.filter(g=>g.id!==null).length; }
  get totalUser(){ return this.groups.reduce((n,g)=>n+g.users.length,0); }
  get jumlahDipetakan(){ return this.groups.filter(g=>g.divisiId!==null).length; }
  get tanpaDepartemen(){ return this.groups.find(g=>g.id===null)?.users.length??0; }
}
