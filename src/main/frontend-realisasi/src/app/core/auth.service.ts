import { Injectable, signal } from '@angular/core';

export interface SessionUser { userId:string; nama:string; }

/**
 * Login sementara (hanya tampilan). Belum memanggil API: user ID & kata sandi apa pun diterima
 * dan sesi disimpan di browser. Ganti isi login() dengan pemanggilan API auth saat backend siap.
 */
@Injectable({providedIn:'root'})
export class AuthService {
  private readonly key='satria.session';
  readonly user=signal<SessionUser|null>(this.read());

  get loggedIn(){ return this.user()!==null; }

  login(userId:string, _password:string, ingat:boolean):SessionUser {
    const u:SessionUser={userId:userId.trim(), nama:userId.trim()};
    const raw=JSON.stringify(u);
    try{ (ingat?localStorage:sessionStorage).setItem(this.key,raw); }catch{ /* storage diblokir: sesi hanya di memori */ }
    this.user.set(u);
    return u;
  }

  logout(){
    try{ localStorage.removeItem(this.key); sessionStorage.removeItem(this.key); }catch{ /* abaikan */ }
    this.user.set(null);
  }

  private read():SessionUser|null {
    try{ const raw=localStorage.getItem(this.key)??sessionStorage.getItem(this.key); return raw?JSON.parse(raw):null; }catch{ return null; }
  }
}
