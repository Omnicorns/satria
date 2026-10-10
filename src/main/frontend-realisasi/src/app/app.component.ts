import { Component, inject } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/auth.service';

@Component({
  selector:'app-root', standalone:true, imports:[RouterOutlet,RouterLink,RouterLinkActive],
  template:`
  @if (bare) {
    <router-outlet/>
  } @else {
  <div class="app-shell" [class.sidebar-open]="menuOpen" [class.collapsed]="collapsed">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">S</div>
        <div class="brand-text"><b>SATRIA</b><small>Tracking Realisasi & Anggaran</small></div>
      </div>
      <nav (click)="menuOpen=false">
        <div class="nav-label">MENU UTAMA</div>
        <a routerLink="/dashboard" routerLinkActive="active" title="Executive Dashboard">
          <svg class="ico" viewBox="0 0 24 24"><path d="M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z"/></svg><span>Executive Dashboard</span></a>

        <div class="nav-group" [class.open]="rkaOpen" [class.group-active]="isRkaActive()">
          <button type="button" class="nav-group-btn" title="Budget & RKA" (click)="toggleRka(); $event.stopPropagation()">
            <svg class="ico" viewBox="0 0 24 24"><path d="M4 4h16v4H4zM4 10h7v10H4zM13 10h7v4h-7zM13 16h7v4h-7z"/></svg>
            <span>Budget & RKA</span><span class="chev">⌄</span>
          </button>
          <div class="nav-sub">
            <a routerLink="/rka" routerLinkActive="active">Monitoring RKA</a>
            <a routerLink="/pekerjaan" routerLinkActive="active">CAPEX / OPEX</a>
          </div>
        </div>

        <div class="nav-label">PENGATURAN</div>
        <a routerLink="/master" routerLinkActive="active" title="Master Data">
          <svg class="ico" viewBox="0 0 24 24"><path d="M12 3c4.97 0 9 1.34 9 3s-4.03 3-9 3-9-1.34-9-3 4.03-3 9-3zm-9 6c0 1.66 4.03 3 9 3s9-1.34 9-3v4c0 1.66-4.03 3-9 3s-9-1.34-9-3zm0 7c0 1.66 4.03 3 9 3s9-1.34 9-3v3c0 1.66-4.03 3-9 3s-9-1.34-9-3z"/></svg><span>Master Data</span></a>
      </nav>
      <div class="sidebar-foot">
        <button type="button" class="nav-group-btn collapse-btn" (click)="collapsed=!collapsed" [title]="collapsed?'Perluas menu':'Ciutkan menu'">
          <svg class="ico" viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></svg><span>Ciutkan</span>
        </button>
      </div>
    </aside>
    <div class="backdrop" (click)="menuOpen=false"></div>
    <main class="main">
      <header class="topbar">
        <button class="icon-btn mobile-menu" (click)="menuOpen=!menuOpen" aria-label="Menu">☰</button>
        <div class="top-heading"><div class="eyebrow">{{section}}</div><b class="top-title">{{title}}</b></div>
        <div class="top-actions">
          <span class="top-date">{{today}}</span>
          <span class="lang"><b>ID</b> | EN</span>
          <div class="user-chip">
            <div class="avatar small">{{initials}}</div>
            <div class="user-meta"><b>{{auth.user()?.nama||auth.user()?.username}}</b><small>{{auth.user()?.departemen||auth.user()?.jabatan||auth.user()?.username}}</small></div>
          </div>
          <button class="btn-logout" (click)="logout()" title="Keluar">Keluar</button>
        </div>
      </header>
      <div class="page-wrap"><router-outlet/></div>
    </main>
  </div>
  }`
})
export class AppComponent {
  readonly auth=inject(AuthService);
  private router=inject(Router); private route=inject(ActivatedRoute);
  menuOpen=false; rkaOpen=false; collapsed=false; bare=true;
  title=''; section='';
  today=new Date().toLocaleDateString('id-ID',{weekday:'long',day:'numeric',month:'long',year:'numeric'});

  constructor(){
    this.router.events.pipe(filter(e=>e instanceof NavigationEnd)).subscribe(()=>{
      let r=this.route; while(r.firstChild) r=r.firstChild;
      const d=r.snapshot.data;
      this.bare=!!d['bare']; this.title=d['title']||''; this.section=d['section']||'';
      if(this.isRkaActive()) this.rkaOpen=true;
    });
  }
  get initials(){ const n=this.auth.user()?.nama||this.auth.user()?.username||''; return n.split(/[\s._-]+/).filter(Boolean).slice(0,2).map(s=>s[0].toUpperCase()).join('')||'U'; }
  isRkaActive(){ const url=this.router.url; return url.startsWith('/rka')||url.startsWith('/pekerjaan'); }
  toggleRka(){ if(this.collapsed){ this.collapsed=false; this.rkaOpen=true; } else this.rkaOpen=!this.rkaOpen; }
  logout(){ this.auth.logout(); this.router.navigateByUrl('/login'); }
}
