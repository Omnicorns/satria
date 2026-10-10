import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Tambah Authorization: Bearer ke request /api; bila backend menjawab 401 (sesi habis), kembali ke halaman login. */
export const authInterceptor:HttpInterceptorFn=(req,next)=>{
  const auth=inject(AuthService), router=inject(Router);
  const isLogin=req.url.endsWith('/api/auth/login');
  const token=auth.token;
  const r=token&&!isLogin&&req.url.startsWith('/api') ? req.clone({setHeaders:{Authorization:`Bearer ${token}`}}) : req;
  return next(r).pipe(catchError((err:unknown)=>{
    if(err instanceof HttpErrorResponse && err.status===401 && !isLogin && !req.url.endsWith('/api/auth/logout')){
      auth.clear();
      router.navigate(['/login'],{queryParams:{redirect:router.url}});
    }
    return throwError(()=>err);
  }));
};
