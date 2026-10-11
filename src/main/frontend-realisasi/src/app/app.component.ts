import { Component, HostListener, inject } from '@angular/core';
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
        <div class="brand-mark">
          <img src="logo2.png" alt="Logo Admin">
        </div>
        <div class="brand-text"><b>SATRIA</b><small>Tracking Realisasi & Anggaran</small></div>
        <button type="button" class="drawer-close" (click)="setMenu(false)" aria-label="Tutup menu">✕</button>
      </div>
      <nav (click)="onNavClick($event)">
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
        <a routerLink="/pengguna" routerLinkActive="active" title="Pengguna & Departemen AD">
          <svg class="ico" viewBox="0 0 24 24"><path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm-7 10v-1a6 6 0 0 1 6-6h2a6 6 0 0 1 6 6v1zm15-10a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm1.5 3H17a7.9 7.9 0 0 1 3 6v1h3v-1a5 5 0 0 0-4.5-6z"/></svg><span>Pengguna AD</span></a>
      </nav>
      <div class="sidebar-foot">
        <button type="button" class="nav-group-btn collapse-btn" (click)="collapsed=!collapsed" [title]="collapsed?'Perluas menu':'Ciutkan menu'">
          <svg class="ico" viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></svg><span>Minimize</span>
        </button>
      </div>
    </aside>
    <div class="backdrop" (click)="setMenu(false)"></div>
    <main class="main">
      <header class="topbar">
        <button class="icon-btn mobile-menu" (click)="setMenu(!menuOpen)" aria-label="Buka menu" [attr.aria-expanded]="menuOpen">☰</button>
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
      this.setMenu(false); // drawer mobile tertutup setiap pindah halaman
    });
  }
  /** Buka/tutup drawer menu (mobile); scroll halaman dikunci selama drawer terbuka. */
  setMenu(open:boolean){ this.menuOpen=open; document.body.classList.toggle('no-scroll',open); }
  onNavClick(e:Event){ if((e.target as HTMLElement).closest('a')) this.setMenu(false); }
  @HostListener('document:keydown.escape') onEscape(){ if(this.menuOpen) this.setMenu(false); }
  get initials(){ const n=this.auth.user()?.nama||this.auth.user()?.username||''; return n.split(/[\s._-]+/).filter(Boolean).slice(0,2).map(s=>s[0].toUpperCase()).join('')||'U'; }
  isRkaActive(){ const url=this.router.url; return url.startsWith('/rka')||url.startsWith('/pekerjaan'); }
  toggleRka(){ if(this.collapsed){ this.collapsed=false; this.rkaOpen=true; } else this.rkaOpen=!this.rkaOpen; }
  logout(){ this.auth.logout(); this.router.navigateByUrl('/login'); }
}
