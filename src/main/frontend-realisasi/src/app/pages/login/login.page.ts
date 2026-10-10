import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';

@Component({selector:'app-login',standalone:true,imports:[CommonModule,FormsModule],template:`
<div class="login-page">
  <div class="login-bg"></div><div class="login-shade"></div>

  <header class="login-top">
    <div class="satria-logo"><span class="satria-mark">S</span><div><b>SATRIA</b><small>by Sarinah</small></div></div>
    <nav class="login-links"><a>Tentang</a><a>Panduan</a><a>Bantuan</a><span class="lang"><b>ID</b> | EN</span></nav>
  </header>

  <section class="login-hero">
    <div class="hero-eyebrow">Platform Anggaran Sarinah</div>
    <h1>SATRIA</h1>
    <p class="hero-full">Sarinah Tracking Realisasi<br>dan Informasi Anggaran</p>
    <p class="hero-tag" [class.fade]="fading">{{taglines[tagIndex]}}</p>
  </section>

  <section class="login-card">
    <div class="login-card-body">
      <h2>Selamat Datang di SATRIA</h2>
      <p class="login-sub">Masuk untuk memantau RKA, realisasi CAPEX/OPEX, dan anggaran divisi.</p>
      <form (ngSubmit)="submit()" novalidate>
        <label for="uid">Username</label>
        <input id="uid" name="uid" [(ngModel)]="userId" placeholder="Username Windows / email kantor" autocomplete="username">
        <label for="pwd">Kata Sandi</label>
        <div class="pwd-wrap">
          <input id="pwd" name="pwd" [type]="showPwd?'text':'password'" [(ngModel)]="password" placeholder="Kata sandi Windows" autocomplete="current-password">
          <button type="button" class="pwd-toggle" (click)="showPwd=!showPwd">{{showPwd?'Sembunyikan':'Tampilkan'}}</button>
        </div>
        <div class="login-row"><label class="check"><input type="checkbox" name="ingat" [(ngModel)]="ingat"> Ingat saya</label><a class="login-link">Lupa kata sandi?</a></div>
        <div *ngIf="error" class="login-error">{{error}}</div>
        <button type="submit" class="login-btn" [disabled]="loading">{{loading?'Memproses…':'Masuk'}}</button>
      </form>
      <p class="login-help">Belum punya akun atau akun terkunci? <a class="login-link">Hubungi admin anggaran</a></p>
    </div>
    <div class="login-card-foot">© {{year}} PT Sarinah · SATRIA v1.0</div>
  </section>
</div>
`})
export class LoginPage implements OnInit, OnDestroy {
  private auth=inject(AuthService); private router=inject(Router); private route=inject(ActivatedRoute);
  userId='';password='';ingat=false;showPwd=false;loading=false;error='';year=new Date().getFullYear();
  taglines=['Pantau realisasi anggaran secara real time','RKA per divisi, terukur dan transparan','CAPEX & OPEX dalam satu tampilan'];
  tagIndex=0;fading=false;private timer?:ReturnType<typeof setInterval>;

  ngOnInit(){ this.timer=setInterval(()=>{this.fading=true;setTimeout(()=>{this.tagIndex=(this.tagIndex+1)%this.taglines.length;this.fading=false;},350);},4000); }
  ngOnDestroy(){ clearInterval(this.timer); }

  submit(){
    this.error='';
    if(!this.userId.trim()||!this.password){this.error='Username dan kata sandi wajib diisi.';return;}
    this.loading=true;
    this.auth.login(this.userId,this.password,this.ingat).subscribe({
      next:()=>{
        this.loading=false;
        const target=this.route.snapshot.queryParamMap.get('redirect');
        this.router.navigateByUrl(target&&target.startsWith('/')&&!target.startsWith('/login')?target:'/dashboard');
      },
      error:(e:HttpErrorResponse)=>{
        this.loading=false; this.password='';
        this.error=e.status===0?'Server SATRIA tidak dapat dihubungi. Coba lagi beberapa saat.'
          :e.status===503?'Server Active Directory tidak dapat dihubungi. Hubungi tim IT.'
          :e.error?.message||'Login gagal. Coba lagi.';
      }
    });
  }
}
