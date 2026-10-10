import { HttpErrorResponse } from '@angular/common/http';

/** Rupiah penuh, contoh: Rp 1.250.000 */
export function money(n:number|null|undefined):string {
  return 'Rp '+new Intl.NumberFormat('id-ID',{maximumFractionDigits:0}).format(Number(n)||0);
}

/** Rupiah ringkas untuk KPI/grafik, contoh: Rp 1,25 M */
export function moneyShort(n:number|null|undefined):string {
  const v=Number(n)||0, abs=Math.abs(v), f=(x:number,d:number)=>x.toLocaleString('id-ID',{minimumFractionDigits:d,maximumFractionDigits:d});
  if(abs>=1e12) return 'Rp '+f(v/1e12,2)+' T';
  if(abs>=1e9) return 'Rp '+f(v/1e9,2)+' M';
  if(abs>=1e6) return 'Rp '+f(v/1e6,1)+' Jt';
  return money(v);
}

export function clamp(n:number|null|undefined):number { return Math.max(0,Math.min(100,Number(n)||0)); }

/** Tanggal ISO (yyyy-MM-dd) ke format Indonesia, contoh: 01 Okt 2026 */
export function tgl(iso:string|null|undefined):string {
  if(!iso) return '-';
  const d=new Date(iso+'T00:00:00');
  return isNaN(d.getTime()) ? iso : d.toLocaleDateString('id-ID',{day:'2-digit',month:'short',year:'numeric'});
}

export function today():string {
  const d=new Date(); return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,10);
}

/** Pilihan tahun: 3 tahun ke belakang s/d 2 tahun ke depan (RKAP tahun berikutnya). */
export function yearOptions():number[] {
  const y=new Date().getFullYear(); return [y-3,y-2,y-1,y,y+1,y+2];
}

/** Ambil pesan error dari ErrorResponse backend (message + details). */
export function errMsg(e:unknown, fallback:string):string {
  if(e instanceof HttpErrorResponse){
    if(e.status===0) return 'Tidak dapat terhubung ke backend. Pastikan Spring Boot aktif di port 8080.';
    const body=e.error;
    if(body && typeof body==='object'){
      const details:string[]=Array.isArray(body.details)?body.details:[];
      if(body.message) return details.length ? `${body.message}: ${details.join('; ')}` : body.message;
    }
  }
  return fallback;
}

export function downloadBlob(blob:Blob,name:string){
  const u=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=u; a.download=name; document.body.appendChild(a); a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(u),1000);
}
