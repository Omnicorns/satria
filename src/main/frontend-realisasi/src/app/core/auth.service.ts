import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

/** User hasil login Active Directory (data dari tabel app_user di backend). */
export interface SessionUser {
  id:number; username:string; nama:string|null; email:string|null; departemen:string|null; jabatan:string|null;
  sumber:'LDAP'|'DEV'; aktif:boolean; lastLoginAt:string|null;
}
export interface LoginResponse { token:string; tokenType:string; expiresAt:string; user:SessionUser; }
interface StoredSession { token:string; expiresAt:string; user:SessionUser; }

/**
 * Login ke backend (POST /api/auth/login, dicek ke Active Directory). Token disimpan di localStorage bila
 * "Ingat saya" dicentang, selain itu di sessionStorage, dan dikirim sebagai Authorization: Bearer oleh authInterceptor.
 */
@Injectable({providedIn:'root'})
export class AuthService {
  private readonly key='satria.session';
  private http=inject(HttpClient);
  private session=signal<StoredSession|null>(this.read());
  readonly user=signal<SessionUser|null>(this.session()?.user??null);

  get loggedIn(){ const s=this.session(); return s!==null && new Date(s.expiresAt).getTime()>Date.now(); }
  get token(){ return this.session()?.token??null; }

  login(username:string, password:string, ingat:boolean):Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login',{username:username.trim(), password}).pipe(tap(res=>{
      const s:StoredSession={token:res.token, expiresAt:res.expiresAt, user:res.user};
      this.clearStorage();
      try{ (ingat?localStorage:sessionStorage).setItem(this.key,JSON.stringify(s)); }catch{ /* storage diblokir: sesi hanya di memori */ }
      this.session.set(s); this.user.set(res.user);
    }));
  }

  /** Hapus token di server (best effort) lalu bersihkan sesi lokal. */
  logout(){
    if(this.token) this.http.post('/api/auth/logout',{}).subscribe({error:()=>{}});
    this.clear();
  }

  /** Bersihkan sesi lokal tanpa memanggil server (dipakai saat token sudah ditolak 401). */
  clear(){ this.clearStorage(); this.session.set(null); this.user.set(null); }

  private clearStorage(){ try{ localStorage.removeItem(this.key); sessionStorage.removeItem(this.key); }catch{ /* abaikan */ } }

  private read():StoredSession|null {
    try{
      const raw=localStorage.getItem(this.key)??sessionStorage.getItem(this.key);
      const s=raw?JSON.parse(raw):null;
      return s&&s.token&&s.user ? s : null; // sesi format lama (tanpa token) dianggap belum login
    }catch{ return null; }
  }
}
